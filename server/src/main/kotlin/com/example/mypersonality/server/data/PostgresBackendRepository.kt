package com.example.mypersonality.server.data

import com.example.mypersonality.server.models.ApplicationDto
import com.example.mypersonality.server.models.ApplicationRequest
import com.example.mypersonality.server.models.AuthResponse
import com.example.mypersonality.server.models.LoginRequest
import com.example.mypersonality.server.models.ProfileDto
import com.example.mypersonality.server.models.RegisterRequest
import com.example.mypersonality.server.models.UserRole
import com.example.mypersonality.server.models.VacancyDto
import com.example.mypersonality.server.models.VacancyRequest
import java.util.UUID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class PostgresBackendRepository : BackendRepository {

    override fun register(request: RegisterRequest): AuthResponse = transaction {
        require(request.email.contains("@")) { "Укажите корректную почту" }
        require(request.password.length >= 6) { "Пароль должен быть не короче 6 символов" }
        require(request.fullName.isNotBlank()) { "Укажите имя пользователя" }

        val normalizedEmail = request.email.trim().lowercase()
        val existing = UsersTable.selectAll().where { UsersTable.email eq normalizedEmail }.singleOrNull()
        require(existing == null) { "Пользователь с такой почтой уже существует" }

        val userId = UUID.randomUUID().toString()
        UsersTable.insert {
            it[id] = userId
            it[email] = normalizedEmail
            it[passwordHash] = PasswordHasher.hash(request.password)
            it[fullName] = request.fullName.trim()
            it[role] = request.role.name
            it[city] = "Не указан"
            it[about] = "Профиль создан в системе MyPersonality"
        }
        AuthResponse(profile = requireNotNull(getProfile(userId)))
    }

    override fun login(request: LoginRequest): AuthResponse = transaction {
        require(request.email.contains("@")) { "Укажите корректную почту" }
        require(request.password.isNotBlank()) { "Введите пароль" }

        val normalizedEmail = request.email.trim().lowercase()
        val user = UsersTable.selectAll().where { UsersTable.email eq normalizedEmail }.singleOrNull()
            ?: error("Пользователь не найден")
        require(PasswordHasher.matches(request.password, user[UsersTable.passwordHash])) { "Неверный пароль" }
        AuthResponse(profile = user.toProfile())
    }

    override fun getProfile(userId: String): ProfileDto? = transaction {
        UsersTable.selectAll().where { UsersTable.id eq userId }.singleOrNull()?.toProfile()
    }

    override fun getVacancies(query: String): List<VacancyDto> = transaction {
        VacanciesTable.selectAll()
            .map { it.toVacancy() }
            .filter { vacancy ->
                query.isBlank() ||
                    vacancy.title.contains(query, ignoreCase = true) ||
                    vacancy.companyName.contains(query, ignoreCase = true) ||
                    vacancy.description.contains(query, ignoreCase = true)
            }
    }

    override fun getVacancy(vacancyId: String): VacancyDto? = transaction {
        VacanciesTable.selectAll().where { VacanciesTable.id eq vacancyId }.singleOrNull()?.toVacancy()
    }

    override fun getEmployerVacancies(employerId: String): List<VacancyDto> = transaction {
        VacanciesTable.selectAll().where { VacanciesTable.employerId eq employerId }.map { it.toVacancy() }
    }

    override fun getFavorites(userId: String): List<VacancyDto> = transaction {
        (FavoritesTable innerJoin VacanciesTable)
            .selectAll()
            .where { FavoritesTable.userId eq userId }
            .map { it.toVacancy() }
    }

    override fun toggleFavorite(userId: String, vacancyId: String): Boolean = transaction {
        val existing = FavoritesTable.selectAll().where {
            (FavoritesTable.userId eq userId) and (FavoritesTable.vacancyId eq vacancyId)
        }.singleOrNull()
        if (existing == null) {
            FavoritesTable.insert {
                it[FavoritesTable.userId] = userId
                it[FavoritesTable.vacancyId] = vacancyId
            }
            true
        } else {
            FavoritesTable.deleteWhere {
                (FavoritesTable.userId eq userId) and (FavoritesTable.vacancyId eq vacancyId)
            }
            false
        }
    }

    override fun saveVacancy(employerId: String, request: VacancyRequest): VacancyDto = transaction {
        require(request.title.isNotBlank()) { "Название вакансии обязательно" }
        require(request.companyName.isNotBlank()) { "Укажите компанию" }
        require(request.description.isNotBlank()) { "Описание обязательно" }

        val vacancyId = request.id ?: UUID.randomUUID().toString()
        val exists = VacanciesTable.selectAll().where { VacanciesTable.id eq vacancyId }.singleOrNull()
        if (exists == null) {
            VacanciesTable.insert {
                it[id] = vacancyId
                it[VacanciesTable.employerId] = employerId
                it[companyName] = request.companyName
                it[title] = request.title
                it[city] = request.city.ifBlank { "Не указан" }
                it[salary] = request.salary.ifBlank { "По договоренности" }
                it[employmentType] = request.employmentType.ifBlank { "Полный день" }
                it[description] = request.description
            }
        } else {
            VacanciesTable.update({ VacanciesTable.id eq vacancyId }) {
                it[companyName] = request.companyName
                it[title] = request.title
                it[city] = request.city.ifBlank { "Не указан" }
                it[salary] = request.salary.ifBlank { "По договоренности" }
                it[employmentType] = request.employmentType.ifBlank { "Полный день" }
                it[description] = request.description
            }
        }
        requireNotNull(getVacancy(vacancyId))
    }

    override fun deleteVacancy(employerId: String, vacancyId: String) {
        transaction {
        val vacancy = VacanciesTable.selectAll().where { VacanciesTable.id eq vacancyId }.singleOrNull()
            ?: error("Вакансия не найдена")
        require(vacancy[VacanciesTable.employerId] == employerId) { "Удалять вакансию может только ее работодатель" }
        VacanciesTable.deleteWhere { VacanciesTable.id eq vacancyId }
        }
    }

    override fun apply(userId: String, request: ApplicationRequest): ApplicationDto = transaction {
        val vacancy = requireNotNull(getVacancy(request.vacancyId)) { "Вакансия не найдена" }
        val profile = requireNotNull(getProfile(userId)) { "Пользователь не найден" }
        require(profile.role == UserRole.SEEKER) { "Откликаться может только соискатель" }

        val duplicate = ApplicationsTable.selectAll().where {
            (ApplicationsTable.vacancyId eq request.vacancyId) and (ApplicationsTable.applicantId eq userId)
        }.singleOrNull()
        require(duplicate == null) { "Вы уже откликались на эту вакансию" }

        val applicationId = UUID.randomUUID().toString()
        ApplicationsTable.insert {
            it[id] = applicationId
            it[vacancyId] = request.vacancyId
            it[vacancyTitle] = vacancy.title
            it[applicantId] = userId
            it[applicantName] = profile.fullName
            it[employerId] = vacancy.employerId
            it[coverLetter] = request.coverLetter.ifBlank { "Без сопроводительного письма" }
            it[status] = "Отправлен"
        }
        ApplicationsTable.selectAll().where { ApplicationsTable.id eq applicationId }.single().toApplication()
    }

    override fun getApplicationsForSeeker(seekerId: String): List<ApplicationDto> = transaction {
        ApplicationsTable.selectAll().where { ApplicationsTable.applicantId eq seekerId }.map { it.toApplication() }
    }

    override fun getApplicationsForVacancy(vacancyId: String): List<ApplicationDto> = transaction {
        ApplicationsTable.selectAll().where { ApplicationsTable.vacancyId eq vacancyId }.map { it.toApplication() }
    }
}

private fun org.jetbrains.exposed.sql.ResultRow.toProfile() = ProfileDto(
    id = this[UsersTable.id],
    fullName = this[UsersTable.fullName],
    email = this[UsersTable.email],
    role = UserRole.valueOf(this[UsersTable.role]),
    city = this[UsersTable.city],
    about = this[UsersTable.about]
)

private fun org.jetbrains.exposed.sql.ResultRow.toVacancy() = VacancyDto(
    id = this[VacanciesTable.id],
    employerId = this[VacanciesTable.employerId],
    companyName = this[VacanciesTable.companyName],
    title = this[VacanciesTable.title],
    city = this[VacanciesTable.city],
    salary = this[VacanciesTable.salary],
    employmentType = this[VacanciesTable.employmentType],
    description = this[VacanciesTable.description]
)

private fun org.jetbrains.exposed.sql.ResultRow.toApplication() = ApplicationDto(
    id = this[ApplicationsTable.id],
    vacancyId = this[ApplicationsTable.vacancyId],
    vacancyTitle = this[ApplicationsTable.vacancyTitle],
    applicantId = this[ApplicationsTable.applicantId],
    applicantName = this[ApplicationsTable.applicantName],
    employerId = this[ApplicationsTable.employerId],
    coverLetter = this[ApplicationsTable.coverLetter],
    status = this[ApplicationsTable.status]
)

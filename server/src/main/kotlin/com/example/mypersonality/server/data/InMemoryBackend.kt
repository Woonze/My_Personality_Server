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

class InMemoryBackend : BackendRepository {

    private val users = mutableMapOf<String, ProfileDto>()
    private val passwords = mutableMapOf<String, String>()
    private val favorites = mutableMapOf<String, MutableSet<String>>()
    private val vacancies = mutableListOf(
        VacancyDto(
            id = "v1",
            employerId = "employer-demo",
            companyName = "ООО Старт Карьера",
            title = "Junior Android Developer",
            city = "Москва",
            salary = "от 90 000 ₽",
            employmentType = "Полный день",
            description = "Разработка клиентского приложения на Kotlin, работа с Compose и REST API."
        ),
        VacancyDto(
            id = "v2",
            employerId = "employer-demo",
            companyName = "HR Tech Lab",
            title = "QA Engineer",
            city = "Санкт-Петербург",
            salary = "от 75 000 ₽",
            employmentType = "Гибрид",
            description = "Ручное и автоматизированное тестирование мобильного приложения."
        )
    )
    private val applications = mutableListOf<ApplicationDto>()

    override fun register(request: RegisterRequest): AuthResponse {
        require(request.email.contains("@")) { "Укажите корректную почту" }
        require(request.password.length >= 6) { "Пароль должен быть не короче 6 символов" }
        require(request.fullName.isNotBlank()) { "Укажите имя пользователя" }
        require(users.values.none { it.email.equals(request.email, ignoreCase = true) }) {
            "Пользователь с такой почтой уже существует"
        }

        val userId = UUID.randomUUID().toString()
        val profile = ProfileDto(
            id = userId,
            fullName = request.fullName,
            email = request.email,
            role = request.role,
            city = "Не указан",
            about = "Профиль создан в системе MyPersonality"
        )
        users[profile.id] = profile
        passwords[profile.id] = PasswordHasher.hash(request.password)
        return AuthResponse(profile = profile)
    }

    override fun login(request: LoginRequest): AuthResponse {
        val profile = users.values.firstOrNull { it.email.equals(request.email, ignoreCase = true) }
            ?: error("Пользователь не найден")
        val storedHash = passwords[profile.id] ?: error("Учетная запись повреждена")
        require(PasswordHasher.matches(request.password, storedHash)) { "Неверный пароль" }
        return AuthResponse(profile = profile)
    }

    override fun getProfile(userId: String): ProfileDto? = users[userId]

    override fun getVacancies(query: String): List<VacancyDto> =
        vacancies.filter {
            query.isBlank() ||
                it.title.contains(query, ignoreCase = true) ||
                it.companyName.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true)
        }

    override fun getVacancy(vacancyId: String): VacancyDto? = vacancies.firstOrNull { it.id == vacancyId }

    override fun getEmployerVacancies(employerId: String): List<VacancyDto> =
        vacancies.filter { it.employerId == employerId }

    override fun getFavorites(userId: String): List<VacancyDto> {
        val favoriteIds = favorites[userId].orEmpty()
        return vacancies.filter { it.id in favoriteIds }
    }

    override fun toggleFavorite(userId: String, vacancyId: String): Boolean {
        val favoriteSet = favorites.getOrPut(userId) { mutableSetOf() }
        return if (favoriteSet.add(vacancyId)) {
            true
        } else {
            favoriteSet.remove(vacancyId)
            false
        }
    }

    override fun saveVacancy(employerId: String, request: VacancyRequest): VacancyDto {
        require(request.title.isNotBlank()) { "Название вакансии обязательно" }
        require(request.companyName.isNotBlank()) { "Укажите компанию" }
        require(request.description.isNotBlank()) { "Описание обязательно" }

        val vacancy = VacancyDto(
            id = request.id ?: UUID.randomUUID().toString(),
            employerId = employerId,
            companyName = request.companyName,
            title = request.title,
            city = request.city.ifBlank { "Не указан" },
            salary = request.salary.ifBlank { "По договоренности" },
            employmentType = request.employmentType.ifBlank { "Полный день" },
            description = request.description
        )

        val index = vacancies.indexOfFirst { it.id == vacancy.id }
        if (index >= 0) {
            vacancies[index] = vacancy
        } else {
            vacancies.add(0, vacancy)
        }
        return vacancy
    }

    override fun deleteVacancy(employerId: String, vacancyId: String) {
        val vacancy = getVacancy(vacancyId) ?: error("Вакансия не найдена")
        require(vacancy.employerId == employerId) { "Удалять вакансию может только ее работодатель" }
        vacancies.removeAll { it.id == vacancyId }
        applications.removeAll { it.vacancyId == vacancyId }
        favorites.values.forEach { it.remove(vacancyId) }
    }

    override fun apply(userId: String, request: ApplicationRequest): ApplicationDto {
        val vacancy = getVacancy(request.vacancyId) ?: error("Вакансия не найдена")
        val profile = getProfile(userId) ?: error("Пользователь не найден")
        require(profile.role == UserRole.SEEKER) { "Откликаться может только соискатель" }
        require(applications.none { it.vacancyId == request.vacancyId && it.applicantId == userId }) {
            "Вы уже откликались на эту вакансию"
        }
        val application = ApplicationDto(
            id = UUID.randomUUID().toString(),
            vacancyId = request.vacancyId,
            vacancyTitle = vacancy.title,
            applicantId = userId,
            applicantName = profile.fullName,
            employerId = vacancy.employerId,
            coverLetter = request.coverLetter.ifBlank { "Без сопроводительного письма" },
            status = "Отправлен"
        )
        applications += application
        return application
    }

    override fun getApplicationsForSeeker(seekerId: String): List<ApplicationDto> =
        applications.filter { it.applicantId == seekerId }

    override fun getApplicationsForVacancy(vacancyId: String): List<ApplicationDto> =
        applications.filter { it.vacancyId == vacancyId }
}

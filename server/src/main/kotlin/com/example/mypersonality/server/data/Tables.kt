package com.example.mypersonality.server.data

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object UsersTable : Table("users") {
    val id = varchar("id", 128)
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 512)
    val fullName = varchar("full_name", 255)
    val role = varchar("role", 32)
    val city = varchar("city", 128)
    val about = text("about")

    override val primaryKey = PrimaryKey(id)
}

object VacanciesTable : Table("vacancies") {
    val id = varchar("id", 128)
    val employerId = varchar("employer_id", 128).references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val companyName = varchar("company_name", 255)
    val title = varchar("title", 255)
    val city = varchar("city", 128)
    val salary = varchar("salary", 128)
    val employmentType = varchar("employment_type", 128)
    val description = text("description")

    override val primaryKey = PrimaryKey(id)
}

object FavoritesTable : Table("favorites") {
    val userId = varchar("user_id", 128).references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val vacancyId = varchar("vacancy_id", 128).references(VacanciesTable.id, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(userId, vacancyId)
}

object ApplicationsTable : Table("applications") {
    val id = varchar("id", 128)
    val vacancyId = varchar("vacancy_id", 128).references(VacanciesTable.id, onDelete = ReferenceOption.CASCADE)
    val vacancyTitle = varchar("vacancy_title", 255)
    val applicantId = varchar("applicant_id", 128).references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val applicantName = varchar("applicant_name", 255)
    val employerId = varchar("employer_id", 128).references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val coverLetter = text("cover_letter")
    val status = varchar("status", 64)

    override val primaryKey = PrimaryKey(id)
}

package com.example.mypersonality.server.models

import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    SEEKER,
    EMPLOYER
}

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val fullName: String,
    val role: UserRole
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val profile: ProfileDto
)

@Serializable
data class ProfileDto(
    val id: String,
    val fullName: String,
    val email: String,
    val role: UserRole,
    val city: String,
    val about: String
)

@Serializable
data class VacancyDto(
    val id: String,
    val employerId: String,
    val companyName: String,
    val title: String,
    val city: String,
    val salary: String,
    val employmentType: String,
    val description: String
)

@Serializable
data class VacancyRequest(
    val id: String? = null,
    val companyName: String,
    val title: String,
    val city: String,
    val salary: String,
    val employmentType: String,
    val description: String
)

@Serializable
data class ApplicationRequest(
    val vacancyId: String,
    val coverLetter: String
)

@Serializable
data class ApplicationDto(
    val id: String,
    val vacancyId: String,
    val vacancyTitle: String,
    val applicantId: String,
    val applicantName: String,
    val employerId: String,
    val coverLetter: String,
    val status: String
)

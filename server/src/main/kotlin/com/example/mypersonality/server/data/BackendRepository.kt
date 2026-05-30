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

interface BackendRepository {
    fun register(request: RegisterRequest): AuthResponse
    fun login(request: LoginRequest): AuthResponse
    fun getProfile(userId: String): ProfileDto?
    fun getVacancies(query: String): List<VacancyDto>
    fun getVacancy(vacancyId: String): VacancyDto?
    fun getEmployerVacancies(employerId: String): List<VacancyDto>
    fun getFavorites(userId: String): List<VacancyDto>
    fun toggleFavorite(userId: String, vacancyId: String): Boolean
    fun saveVacancy(employerId: String, request: VacancyRequest): VacancyDto
    fun deleteVacancy(employerId: String, vacancyId: String)
    fun apply(userId: String, request: ApplicationRequest): ApplicationDto
    fun getApplicationsForSeeker(seekerId: String): List<ApplicationDto>
    fun getApplicationsForVacancy(vacancyId: String): List<ApplicationDto>
}

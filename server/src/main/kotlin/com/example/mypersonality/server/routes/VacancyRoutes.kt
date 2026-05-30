package com.example.mypersonality.server.routes

import com.example.mypersonality.server.data.BackendRepository
import com.example.mypersonality.server.models.ApplicationRequest
import com.example.mypersonality.server.models.VacancyRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.vacancyRoutes(repository: BackendRepository) {
    route("/vacancies") {
        get {
            val query = call.request.queryParameters["query"].orEmpty()
            call.respond(repository.getVacancies(query))
        }

        get("/favorites/{userId}") {
            call.respond(repository.getFavorites(call.parameters["userId"].orEmpty()))
        }

        get("/employer/{userId}") {
            call.respond(repository.getEmployerVacancies(call.parameters["userId"].orEmpty()))
        }

        get("/{vacancyId}") {
            val vacancy = repository.getVacancy(call.parameters["vacancyId"].orEmpty())
            if (vacancy == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("message" to "Вакансия не найдена"))
            } else {
                call.respond(vacancy)
            }
        }

        post {
            val userId = call.request.queryParameters["employerId"].orEmpty()
            runCatching {
                repository.saveVacancy(userId, call.receive<VacancyRequest>())
            }.onSuccess { vacancy ->
                call.respond(HttpStatusCode.Created, vacancy)
            }.onFailure { error ->
                call.respond(HttpStatusCode.BadRequest, mapOf("message" to (error.message ?: "Ошибка создания вакансии")))
            }
        }

        put("/{vacancyId}") {
            val userId = call.request.queryParameters["employerId"].orEmpty()
            runCatching {
                repository.saveVacancy(
                    employerId = userId,
                    request = call.receive<VacancyRequest>().copy(id = call.parameters["vacancyId"])
                )
            }.onSuccess { vacancy ->
                call.respond(vacancy)
            }.onFailure { error ->
                call.respond(HttpStatusCode.BadRequest, mapOf("message" to (error.message ?: "Ошибка обновления вакансии")))
            }
        }

        delete("/{vacancyId}") {
            val userId = call.request.queryParameters["employerId"].orEmpty()
            runCatching {
                repository.deleteVacancy(
                    employerId = userId,
                    vacancyId = call.parameters["vacancyId"].orEmpty()
                )
            }.onSuccess {
                call.respond(HttpStatusCode.NoContent)
            }.onFailure { error ->
                call.respond(HttpStatusCode.BadRequest, mapOf("message" to (error.message ?: "Ошибка удаления вакансии")))
            }
        }

        post("/{vacancyId}/favorite/{userId}") {
            val isFavorite = repository.toggleFavorite(
                userId = call.parameters["userId"].orEmpty(),
                vacancyId = call.parameters["vacancyId"].orEmpty()
            )
            call.respond(mapOf("isFavorite" to isFavorite))
        }

        post("/{vacancyId}/apply/{userId}") {
            runCatching {
                repository.apply(
                    userId = call.parameters["userId"].orEmpty(),
                    request = call.receive<ApplicationRequest>().copy(
                        vacancyId = call.parameters["vacancyId"].orEmpty()
                    )
                )
            }.onSuccess { application ->
                call.respond(HttpStatusCode.Created, application)
            }.onFailure { error ->
                call.respond(HttpStatusCode.BadRequest, mapOf("message" to (error.message ?: "Ошибка отклика")))
            }
        }

        get("/applications/seeker/{userId}") {
            call.respond(repository.getApplicationsForSeeker(call.parameters["userId"].orEmpty()))
        }

        get("/{vacancyId}/applications") {
            call.respond(repository.getApplicationsForVacancy(call.parameters["vacancyId"].orEmpty()))
        }
    }
}

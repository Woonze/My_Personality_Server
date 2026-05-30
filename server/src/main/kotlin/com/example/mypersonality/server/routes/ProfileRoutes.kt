package com.example.mypersonality.server.routes

import com.example.mypersonality.server.data.BackendRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.profileRoutes(repository: BackendRepository) {
    route("/profile") {
        get("/{userId}") {
            val userId = call.parameters["userId"].orEmpty()
            val profile = repository.getProfile(userId)
            if (profile == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("message" to "Профиль не найден"))
            } else {
                call.respond(profile)
            }
        }
    }
}

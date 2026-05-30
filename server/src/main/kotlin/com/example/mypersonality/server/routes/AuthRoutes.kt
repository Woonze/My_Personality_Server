package com.example.mypersonality.server.routes

import com.example.mypersonality.server.data.BackendRepository
import com.example.mypersonality.server.models.LoginRequest
import com.example.mypersonality.server.models.RegisterRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.authRoutes(repository: BackendRepository) {
    route("/auth") {
        post("/register") {
            runCatching {
                repository.register(call.receive<RegisterRequest>())
            }.onSuccess { response ->
                call.respond(HttpStatusCode.Created, response)
            }.onFailure { error ->
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to (error.message ?: "Ошибка авторизации"))
                )
            }
        }

        post("/login") {
            runCatching {
                repository.login(call.receive<LoginRequest>())
            }.onSuccess { response ->
                call.respond(response)
            }.onFailure { error ->
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to (error.message ?: "Ошибка авторизации"))
                )
            }
        }
    }
}

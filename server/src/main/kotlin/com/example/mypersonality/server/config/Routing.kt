package com.example.mypersonality.server.config

import com.example.mypersonality.server.data.BackendRepository
import com.example.mypersonality.server.routes.authRoutes
import com.example.mypersonality.server.routes.profileRoutes
import com.example.mypersonality.server.routes.vacancyRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.configureRouting(repository: BackendRepository) {
    routing {
        authRoutes(repository)
        profileRoutes(repository)
        vacancyRoutes(repository)
    }
}

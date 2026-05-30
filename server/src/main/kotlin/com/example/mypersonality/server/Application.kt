package com.example.mypersonality.server

import com.example.mypersonality.server.config.configureHttp
import com.example.mypersonality.server.config.configureRouting
import com.example.mypersonality.server.config.configureSerialization
import com.example.mypersonality.server.data.DatabaseFactory
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    install(CallLogging)
    install(StatusPages)
    val backend = DatabaseFactory.createRepositoryFromEnvironment()
    configureSerialization()
    configureHttp()
    configureRouting(backend)
    routing {
        get("/") {
            call.respond(mapOf("status" to "ok", "service" to "mypersonality-server"))
        }
    }
}

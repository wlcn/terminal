package org.now.terminal.infrastructure.config

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.now.terminal.boundedcontexts.terminalsession.infrastructure.api.terminalSessionRoutes

fun Application.configureRouting() {
    install(AutoHeadResponse)
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            // Log detailed error stack trace using Ktor's logging system
            call.application.log.error("Internal Server Error: ${cause.message}", cause)
            call.respondText(text = "500: ${cause.message}" , status = HttpStatusCode.InternalServerError)
        }
    }
    routing {
        get("/") {
            call.respondText("KT Terminal API")
        }
        
        // API routes with /api prefix
        route("/api") {
            terminalSessionRoutes()
        }
    }
}

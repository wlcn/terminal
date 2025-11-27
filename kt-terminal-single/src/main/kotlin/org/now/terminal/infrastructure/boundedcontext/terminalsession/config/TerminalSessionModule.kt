package org.now.terminal.infrastructure.boundedcontext.terminalsession.config

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject
import org.now.terminal.infrastructure.boundedcontext.terminalsession.web.controllers.TerminalSessionController

/**
 * Terminal Session Module Configuration
 *
 * Configures all terminal session related routes, controllers, and dependencies
 */
fun Application.configureTerminalSessionModule() {
    // Inject dependencies
    val terminalSessionController by inject<TerminalSessionController>()

    routing {
        route("/api/sessions") {
            // Get all sessions
            get {
                val sessions = terminalSessionController.getSessions()
                call.respond(sessions)
            }

            // Create new session
            post {
                val params = call.request.queryParameters
                val session = terminalSessionController.createSession(
                    userId = params["userId"] ?: "",
                    shellType = params["shellType"] ?: "BASH",
                    workingDirectory = params["workingDirectory"] ?: "/",
                    terminalWidth = params["terminalWidth"]?.toIntOrNull() ?: 80,
                    terminalHeight = params["terminalHeight"]?.toIntOrNull() ?: 24
                )
                call.respond(session)
            }

            route("/{sessionId}") {
                // Get session by ID
                get {
                    val sessionId = call.parameters["sessionId"] ?: ""
                    val session = terminalSessionController.getSessionById(sessionId)
                    if (session != null) {
                        call.respond(session)
                    } else {
                        call.respondText("Session not found", status = HttpStatusCode.NotFound)
                    }
                }

                // Terminate session
                delete {
                    val sessionId = call.parameters["sessionId"] ?: ""
                    val result = terminalSessionController.terminateSession(sessionId)
                    call.respond(result)
                }

                // Execute command in session
                post("/execute") {
                    val sessionId = call.parameters["sessionId"] ?: ""
                    val command = call.request.queryParameters["command"] ?: ""
                    val result = terminalSessionController.executeCommand(sessionId, command)
                    call.respond(result)
                }

                // Execute command and check success
                post("/execute-check") {
                    val sessionId = call.parameters["sessionId"] ?: ""
                    val command = call.request.queryParameters["command"] ?: ""
                    val result = terminalSessionController.executeCommandAndCheckSuccess(sessionId, command)
                    call.respond(result)
                }

                // Get session status
                get("/status") {
                    val sessionId = call.parameters["sessionId"] ?: ""
                    val status = terminalSessionController.getSessionStatus(sessionId)
                    call.respond(mapOf("status" to status))
                }
            }
        }

        // TODO: Add WebSocket endpoint when WebSocket support is implemented
        // route("/ws/sessions/{sessionId}") {
        //     webSocket {
        //         val sessionId = call.parameters["sessionId"] ?: ""
        //         terminalSessionController.handleWebSocketConnection(sessionId, this)
        //     }
        // }
    }
}
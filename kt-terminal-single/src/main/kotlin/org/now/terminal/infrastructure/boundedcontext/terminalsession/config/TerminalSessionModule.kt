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
 * Configures all terminal session related routes
 */
fun Application.configureTerminalSessionModule() {
    // Inject controller dependency
    val terminalSessionController by inject<TerminalSessionController>()

    routing {
        route("/api/sessions") {
            // Get all sessions
            get {
                // TODO: Implement proper session listing for all users
                // For now, return empty list as this is an admin function
                call.respond(emptyList<Any>())
            }

            // Create new session
            post {
                val params = call.request.queryParameters
                val session = terminalSessionController.createSession(
                    userId = params["userId"] ?: "",
                    title = params["title"],
                    workingDirectory = params["workingDirectory"]
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

        // User-specific endpoints
        route("/api/users/{userId}/sessions") {
            // Get all sessions for a user
            get {
                val userId = call.parameters["userId"] ?: ""
                val includeInactive = call.request.queryParameters["includeInactive"]?.toBoolean() ?: false
                val sessions = terminalSessionController.getUserSessions(userId, includeInactive)
                call.respond(sessions)
            }

            // Get active sessions for a user
            get("/active") {
                val userId = call.parameters["userId"] ?: ""
                val sessions = terminalSessionController.getActiveUserSessions(userId)
                call.respond(sessions)
            }

            // Count active sessions for a user
            get("/active/count") {
                val userId = call.parameters["userId"] ?: ""
                val count = terminalSessionController.countActiveSessions(userId)
                call.respond(mapOf("count" to count))
            }

            // Terminate all sessions for a user
            delete {
                val userId = call.parameters["userId"] ?: ""
                val count = terminalSessionController.terminateAllUserSessions(userId)
                call.respond(mapOf("terminatedCount" to count))
            }
        }

        // WebSocket endpoint for terminal session interaction (TODO: Implement proper WebSocket handling)
        // webSocket("/ws/sessions/{sessionId}") {
        //     val sessionId = call.parameters["sessionId"] ?: ""
        //     terminalSessionController.handleWebSocketConnection(sessionId, this)
        // }
    }
}
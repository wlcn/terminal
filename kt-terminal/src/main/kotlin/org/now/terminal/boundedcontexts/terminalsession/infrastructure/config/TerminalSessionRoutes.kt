package org.now.terminal.boundedcontexts.terminalsession.infrastructure.config

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import org.now.terminal.boundedcontexts.terminalsession.domain.TerminalSize
import org.now.terminal.boundedcontexts.terminalsession.domain.service.TerminalProcessService
import org.now.terminal.boundedcontexts.terminalsession.domain.service.TerminalSessionService

// 响应数据类

@Serializable
data class TerminalResizeResponse(
    val sessionId: String,
    val terminalSize: TerminalSize,
    val status: String
)

@Serializable
data class TerminalInterruptResponse(
    val sessionId: String,
    val status: String
)

@Serializable
data class TerminalTerminateResponse(
    val sessionId: String,
    val reason: String,
    val status: String
)

@Serializable
data class TerminalStatusResponse(
    val status: String
)

/**
 * Terminal session routes configuration
 * This follows the same pattern as other route configurations
 */
fun Application.configureTerminalSessionRoutes() {
    val log = this.log
    val terminalSessionService by inject<TerminalSessionService>()
    val terminalProcessService by inject<TerminalProcessService>()

    routing {
        // API routes with /api prefix
        route("/api") {
            route("/sessions") {
                // Create a new session
                post {
                    log.debug("Creating new terminal session")

                    val userId = call.request.queryParameters["userId"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        "Missing userId"
                    )
                    val title = call.request.queryParameters["title"]
                    val workingDirectory = call.request.queryParameters["workingDirectory"] ?: "."
                    val shellType = call.request.queryParameters["shellType"] ?: "powershell"

                    val columnsParam = call.request.queryParameters["columns"]
                    val rowsParam = call.request.queryParameters["rows"]

                    log.debug(
                        "Session creation parameters: userId={}, title={}, workingDirectory={}, shellType={}, columns={}, rows={}",
                        userId, title, workingDirectory, shellType, columnsParam, rowsParam
                    )

                    val terminalSize = if (columnsParam != null && rowsParam != null) {
                        TerminalSize(columnsParam.toInt(), rowsParam.toInt())
                    } else {
                        TerminalSize(80, 24)
                    }

                    val session = terminalSessionService.createSession(
                        userId,
                        title,
                        workingDirectory,
                        shellType,
                        terminalSize
                    )

                    log.info(
                        "Created new terminal session: {}, shellType: {}, workingDirectory: {}",
                        session.id, session.shellType, session.workingDirectory
                    )

                    call.respond(HttpStatusCode.Created, session)
                }

                // Get all sessions
                get {
                    log.debug("Getting all terminal sessions")
                    val sessions = terminalSessionService.getAllSessions()
                    log.debug("Found {} terminal sessions", sessions.size)
                    call.respond(HttpStatusCode.OK, sessions)
                }

                // Get session by ID
                get("/{id}") {
                    val id = call.parameters["id"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        "Invalid session ID"
                    )
                    log.debug("Getting session by ID: {}", id)
                    val session = terminalSessionService.getSessionById(id) ?: return@get call.respond(
                        HttpStatusCode.NotFound,
                        "Session not found"
                    )
                    log.debug("Found session: {}, status: {}", id, session.status)
                    call.respond(HttpStatusCode.OK, session)
                }

                // Resize terminal
                post("/{id}/resize") {
                    val id = call.parameters["id"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        "Invalid session ID"
                    )
                    val columns = call.request.queryParameters["cols"]?.toIntOrNull() ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        "Missing or invalid columns"
                    )
                    val rows = call.request.queryParameters["rows"]?.toIntOrNull() ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        "Missing or invalid rows"
                    )

                    log.debug("Resizing terminal session {} to columns: {}, rows: {}", id, columns, rows)
                    val session = terminalSessionService.resizeTerminal(id, columns, rows) ?: return@post call.respond(
                        HttpStatusCode.NotFound,
                        "Session not found"
                    )
                    terminalProcessService.resizeProcess(id, columns, rows)

                    // 使用专门的数据类响应，直接使用TerminalSize对象
                    val response = TerminalResizeResponse(
                        sessionId = session.id,
                        terminalSize = session.terminalSize,
                        status = session.status.name
                    )
                    log.debug("Resized terminal session {} successfully", id)
                    call.respond(HttpStatusCode.OK, response)
                }

                // Interrupt terminal (send Ctrl+C signal)
                post("/{id}/interrupt") {
                    val id = call.parameters["id"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        "Invalid session ID"
                    )
                    log.debug("Interrupting terminal session: {}", id)
                    val session = terminalSessionService.getSessionById(id) ?: return@post call.respond(
                        HttpStatusCode.NotFound,
                        "Session not found"
                    )

                    val success = terminalProcessService.interruptProcess(id)
                    if (success) {
                        val response = TerminalInterruptResponse(
                            sessionId = session.id,
                            status = "interrupted"
                        )
                        log.debug("Interrupted terminal session {} successfully", id)
                        call.respond(HttpStatusCode.OK, response)
                    } else {
                        log.error("Failed to interrupt session: {}", id)
                        call.respond(HttpStatusCode.InternalServerError, "Failed to interrupt session")
                    }
                }

                // Terminate session
                delete("/{id}") {
                    val id = call.parameters["id"] ?: return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        "Invalid session ID"
                    )
                    log.debug("Terminating terminal session: {}", id)
                    val session = terminalSessionService.terminateSession(id) ?: return@delete call.respond(
                        HttpStatusCode.NotFound,
                        "Session not found"
                    )
                    terminalProcessService.terminateProcess(id)

                    val response = TerminalTerminateResponse(
                        sessionId = session.id,
                        reason = "User terminated",
                        status = session.status.name
                    )
                    log.info("Terminated terminal session: {}", id)
                    call.respond(HttpStatusCode.OK, response)
                }

                // Get session status
                get("/{id}/status") {
                    val id = call.parameters["id"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        "Invalid session ID"
                    )
                    log.debug("Getting status for session: {}", id)
                    val session = terminalSessionService.getSessionById(id) ?: return@get call.respond(
                        HttpStatusCode.NotFound,
                        "Session not found"
                    )

                    val response = TerminalStatusResponse(
                        status = session.status.name
                    )
                    log.debug("Session {} status: {}", id, session.status)
                    call.respond(HttpStatusCode.OK, response)
                }

                // Execute command
                post("/{id}/execute") {
                    val id = call.parameters["id"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        "Invalid session ID"
                    )
                    val command = call.request.queryParameters["command"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        "Missing command"
                    )
                    val timeoutMs = call.request.queryParameters["timeoutMs"]?.toLongOrNull()

                    log.debug("Executing command for session {}: {}, timeout: {}", id, command, timeoutMs)
                    val success = terminalProcessService.writeToProcess(id, "$command\n")
                    if (success) {
                        log.debug("Executed command for session {}: {}", id, command)
                        call.respond(HttpStatusCode.OK, "Command executed: $command")
                    } else {
                        log.error("Failed to execute command for session {}: {}", id, command)
                        call.respond(HttpStatusCode.InternalServerError, "Failed to execute command")
                    }
                }

                // Execute command and check success
                post("/{id}/execute-check") {
                    val id = call.parameters["id"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        "Invalid session ID"
                    )
                    val command = call.request.queryParameters["command"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        "Missing command"
                    )

                    log.debug("Executing command with check for session {}: {}", id, command)
                    val success = terminalProcessService.writeToProcess(id, "$command\n")
                    log.debug("Command execution check for session {}: {}, success: {}", id, command, success)
                    call.respond(HttpStatusCode.OK, success)
                }
            }
        }
    }
}

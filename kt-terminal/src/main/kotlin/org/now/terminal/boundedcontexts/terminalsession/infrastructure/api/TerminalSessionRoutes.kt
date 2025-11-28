package org.now.terminal.boundedcontexts.terminalsession.infrastructure.api

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import org.now.terminal.boundedcontexts.terminalsession.domain.model.TerminalSessionStatus
import org.now.terminal.boundedcontexts.terminalsession.domain.model.TerminalSize
import org.now.terminal.boundedcontexts.terminalsession.domain.service.TerminalProcessService
import org.now.terminal.boundedcontexts.terminalsession.domain.service.TerminalSessionService
import org.now.terminal.boundedcontexts.terminalsession.infrastructure.service.TerminalConfigManager

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

fun Route.terminalSessionRoutes() {
    val terminalSessionService by inject<TerminalSessionService>()
    val terminalProcessService by inject<TerminalProcessService>()
    
    route("/sessions") {
        // Create a new session
        post {
            val terminalConfig = TerminalConfigManager.getTerminalConfig()

            val userId = call.request.queryParameters["userId"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing userId")
            val title = call.request.queryParameters["title"]
            val requestWorkingDirectory = call.request.queryParameters["workingDirectory"]
            val shellType = call.request.queryParameters["shellType"] ?: terminalConfig.defaultShellType

            val shellConfig = terminalConfig.shells[shellType.lowercase()]
                ?: terminalConfig.shells[terminalConfig.defaultShellType.lowercase()]
                ?: throw IllegalArgumentException("No shell configuration found for type: $shellType")
            
            val workingDirectory = requestWorkingDirectory
                ?: shellConfig.workingDirectory
                ?: terminalConfig.defaultWorkingDirectory
            
            val terminalSize = shellConfig.size?.let {
                TerminalSize(it.columns, it.rows) 
            } ?: TerminalSize(terminalConfig.defaultSize.columns, terminalConfig.defaultSize.rows)
            
            val session = terminalSessionService.createSession(
                userId,
                title, 
                workingDirectory, 
                shellType,
                terminalSize
            )
            
            call.respond(HttpStatusCode.Created, session)
        }
        
        // Get all sessions
        get { 
            val sessions = terminalSessionService.getAllSessions()
            call.respond(HttpStatusCode.OK, sessions)
        }
        
        // Get session by ID
        get("/{id}") { 
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid session ID")
            val session = terminalSessionService.getSessionById(id) ?: return@get call.respond(HttpStatusCode.NotFound, "Session not found")
            call.respond(HttpStatusCode.OK, session)
        }
        
        // Resize terminal
        post("/{id}/resize") { 
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid session ID")
            val columns = call.request.queryParameters["cols"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing or invalid columns")
            val rows = call.request.queryParameters["rows"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing or invalid rows")
            
            val session = terminalSessionService.resizeTerminal(id, columns, rows) ?: return@post call.respond(HttpStatusCode.NotFound, "Session not found")
            terminalProcessService.resizeProcess(id, columns, rows)
            
            // 使用专门的数据类响应，直接使用TerminalSize对象
            val response = TerminalResizeResponse(
                sessionId = session.id,
                terminalSize = session.terminalSize,
                status = session.status.name
            )
            call.respond(HttpStatusCode.OK, response)
        }
        
        // Interrupt terminal (send Ctrl+C signal)
        post("/{id}/interrupt") { 
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid session ID")
            val session = terminalSessionService.getSessionById(id) ?: return@post call.respond(HttpStatusCode.NotFound, "Session not found")
            
            val success = terminalProcessService.interruptProcess(id)
            if (success) {
                val response = TerminalInterruptResponse(
                    sessionId = session.id,
                    status = "interrupted"
                )
                call.respond(HttpStatusCode.OK, response)
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Failed to interrupt session")
            }
        }
        
        // Terminate session
        delete("/{id}") { 
            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid session ID")
            val session = terminalSessionService.terminateSession(id) ?: return@delete call.respond(HttpStatusCode.NotFound, "Session not found")
            terminalProcessService.terminateProcess(id)
            
            val response = TerminalTerminateResponse(
                sessionId = session.id,
                reason = "User terminated",
                status = session.status.name
            )
            call.respond(HttpStatusCode.OK, response)
        }
        
        // Get session status
        get("/{id}/status") { 
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid session ID")
            val session = terminalSessionService.getSessionById(id) ?: return@get call.respond(HttpStatusCode.NotFound, "Session not found")
            
            val response = TerminalStatusResponse(
                status = session.status.name
            )
            call.respond(HttpStatusCode.OK, response)
        }
        
        // Execute command
        post("/{id}/execute") { 
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid session ID")
            val command = call.request.queryParameters["command"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing command")
            val timeoutMs = call.request.queryParameters["timeoutMs"]?.toLongOrNull()
            
            val success = terminalProcessService.writeToProcess(id, "$command\n")
            if (success) {
                call.respond(HttpStatusCode.OK, "Command executed: $command")
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Failed to execute command")
            }
        }
        
        // Execute command and check success
        post("/{id}/execute-check") { 
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid session ID")
            val command = call.request.queryParameters["command"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing command")
            
            val success = terminalProcessService.writeToProcess(id, "$command\n")
            call.respond(HttpStatusCode.OK, success)
        }
    }
}

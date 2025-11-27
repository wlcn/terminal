package org.now.terminal.boundedcontexts.terminalsession.infrastructure.api

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import org.now.terminal.boundedcontexts.terminalsession.domain.model.TerminalSessionStatus
import org.now.terminal.boundedcontexts.terminalsession.domain.service.TerminalProcessService
import org.now.terminal.boundedcontexts.terminalsession.domain.service.TerminalSessionService

fun Route.terminalSessionRoutes() {
    val terminalSessionService by inject<TerminalSessionService>()
    val terminalProcessService by inject<TerminalProcessService>()
    
    route("/sessions") {
        // Create a new session
        post { 
            val userId = call.request.queryParameters["userId"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing userId")
            val title = call.request.queryParameters["title"]
            val workingDirectory = call.request.queryParameters["workingDirectory"] ?: System.getProperty("user.dir")
            
            val session = terminalSessionService.createSession(userId, title, workingDirectory)
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
            call.respond(HttpStatusCode.OK, mapOf(
                "sessionId" to session.id,
                "terminalSize" to session.terminalSize,
                "status" to session.status
            ))
        }
        
        // Interrupt terminal (send Ctrl+C signal)
        post("/{id}/interrupt") { 
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid session ID")
            val session = terminalSessionService.getSessionById(id) ?: return@post call.respond(HttpStatusCode.NotFound, "Session not found")
            
            val success = terminalProcessService.interruptProcess(id)
            if (success) {
                call.respond(HttpStatusCode.OK, mapOf(
                    "sessionId" to session.id,
                    "status" to "interrupted"
                ))
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Failed to interrupt session")
            }
        }
        
        // Terminate session
        delete("/{id}") { 
            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid session ID")
            val session = terminalSessionService.terminateSession(id) ?: return@delete call.respond(HttpStatusCode.NotFound, "Session not found")
            terminalProcessService.terminateProcess(id)
            call.respond(HttpStatusCode.OK, mapOf(
                "sessionId" to session.id,
                "reason" to "User terminated",
                "status" to session.status
            ))
        }
        
        // Get session status
        get("/{id}/status") { 
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid session ID")
            val session = terminalSessionService.getSessionById(id) ?: return@get call.respond(HttpStatusCode.NotFound, "Session not found")
            call.respond(HttpStatusCode.OK, mapOf("status" to session.status))
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

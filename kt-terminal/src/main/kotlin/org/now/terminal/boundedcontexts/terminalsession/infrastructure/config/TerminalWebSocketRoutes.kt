package org.now.terminal.boundedcontexts.terminalsession.infrastructure.config

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import org.koin.ktor.ext.inject
import org.now.terminal.boundedcontexts.terminalsession.domain.service.TerminalProcessService
import org.now.terminal.boundedcontexts.terminalsession.domain.service.TerminalSessionService
import org.now.terminal.boundedcontexts.terminalsession.infrastructure.protocol.TerminalCommunicationHandler
import org.now.terminal.boundedcontexts.terminalsession.infrastructure.protocol.WebSocketProtocol

fun Application.configureTerminalWebSocketRoutes() {
    routing {
        webSocket("/ws/{sessionId}") { // websocketSession
            val sessionId = call.parameters["sessionId"] ?: return@webSocket close(CloseReason(CloseReason.Codes.PROTOCOL_ERROR, "Invalid session ID"))
            
            val terminalSessionService by inject<TerminalSessionService>()
            val terminalProcessService by inject<TerminalProcessService>()
            
            // Create WebSocket protocol implementation
            val protocol = WebSocketProtocol(this)
            
            // Create communication handler
            val handler = TerminalCommunicationHandler(
                sessionId = sessionId,
                protocol = protocol,
                terminalSessionService = terminalSessionService,
                terminalProcessService = terminalProcessService
            )
            
            // Handle communication
            handler.handleCommunication()
        }
    }
}

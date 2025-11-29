package org.now.terminal.boundedcontexts.terminalsession.infrastructure.config

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import org.koin.ktor.ext.inject
import org.now.terminal.boundedcontexts.terminalsession.domain.service.TerminalProcessService
import org.now.terminal.boundedcontexts.terminalsession.domain.service.TerminalSessionService
import org.now.terminal.boundedcontexts.terminalsession.infrastructure.protocol.TerminalCommunicationHandler
import org.now.terminal.boundedcontexts.terminalsession.infrastructure.protocol.WebSocketProtocol

fun Application.configureTerminalWebSocketRoutes() {
    routing {
        webSocket("/ws/{sessionId}") { // websocketSession
            val sessionId = call.parameters["sessionId"] ?: return@webSocket close(
                CloseReason(
                    CloseReason.Codes.PROTOCOL_ERROR,
                    "Invalid session ID"
                )
            )

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

package org.now.terminal.boundedcontexts.terminalsession.infrastructure.api

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.launch
import org.koin.ktor.ext.inject
import org.now.terminal.boundedcontexts.terminalsession.domain.service.TerminalProcessService
import org.now.terminal.boundedcontexts.terminalsession.domain.service.TerminalSessionService
import java.util.UUID

fun Application.configureTerminalWebSocketRoutes() {
    routing {
        webSocket("/ws/{sessionId}") { // websocketSession
            val sessionId = call.parameters["sessionId"]?.let { UUID.fromString(it) } ?: return@webSocket close(CloseReason(CloseReason.Codes.PROTOCOL_ERROR, "Invalid session ID"))
            
            val terminalSessionService by inject<TerminalSessionService>()
            val terminalProcessService by inject<TerminalProcessService>()
            
            // Get or create terminal session
            val session = terminalSessionService.getSessionById(sessionId) ?: return@webSocket close(CloseReason(CloseReason.Codes.GOING_AWAY, "Session not found"))
            
            // Get or create terminal process
            var process = terminalProcessService.getProcess(sessionId)
            if (process == null) {
                process = terminalProcessService.createProcess(sessionId, session.workingDirectory, session.shellType)
            }
            
            // Add output listener to send data to client
            val outputListener: (String) -> Unit = { output ->
                try {
                    // Send output to client using coroutine scope
                    CoroutineScope(Dispatchers.IO).launch {
                        outgoing.send(Frame.Text(output))
                    }
                } catch (e: ClosedSendChannelException) {
                    // Client disconnected
                }
            }
            process.addOutputListener(outputListener)
            
            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        terminalProcessService.writeToProcess(sessionId, text)
                    }
                }
            } catch (e: ClosedReceiveChannelException) {
                // Client disconnected
            } finally {
                // Cleanup
                process.removeOutputListener(outputListener)
            }
        }
    }
}

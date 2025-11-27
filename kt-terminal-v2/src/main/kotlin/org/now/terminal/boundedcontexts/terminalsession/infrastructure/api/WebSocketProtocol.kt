package org.now.terminal.boundedcontexts.terminalsession.infrastructure.api

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException

/**
 * WebSocket implementation of TerminalCommunicationProtocol
 */
class WebSocketProtocol(
    private val session: DefaultWebSocketServerSession
) : TerminalCommunicationProtocol {
    override suspend fun send(data: String) {
        try {
            session.outgoing.send(Frame.Text(data))
        } catch (e: ClosedSendChannelException) {
            // Client disconnected
            throw e
        }
    }
    
    override suspend fun receive(): String? {
        return try {
            val frame = session.incoming.receive()
            if (frame is Frame.Text) {
                frame.readText()
            } else {
                null
            }
        } catch (e: ClosedReceiveChannelException) {
            // Client disconnected
            null
        }
    }
    
    override suspend fun close(reason: String?) {
        session.close(CloseReason(CloseReason.Codes.GOING_AWAY, reason ?: "Connection closed"))
    }
}

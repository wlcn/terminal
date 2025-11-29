package org.now.terminal.boundedcontexts.terminalsession.infrastructure.protocol

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import org.now.terminal.boundedcontexts.terminalsession.domain.service.TerminalCommunicationProtocol

/**
 * WebSocket implementation of TerminalCommunicationProtocol
 */
class WebSocketProtocol(
    private val session: DefaultWebSocketServerSession
) : TerminalCommunicationProtocol {
    private val log = org.slf4j.LoggerFactory.getLogger(WebSocketProtocol::class.java)
    
    override suspend fun send(data: String) {
        try {
            log.trace("Sending data to WebSocket client: {}", data)
            session.outgoing.send(Frame.Text(data))
        } catch (e: ClosedSendChannelException) {
            // Client disconnected
            log.debug("WebSocket client disconnected, cannot send data: {}", e.message)
            throw e
        } catch (e: Exception) {
            log.error("Error sending data to WebSocket client: {}", e.message, e)
            throw e
        }
    }
    
    override suspend fun receive(): String? {
        return try {
            val frame = session.incoming.receive()
            if (frame is Frame.Text) {
                val text = frame.readText()
                log.trace("Received data from WebSocket client: {}", text)
                text
            } else if (frame is Frame.Close) {
                log.debug("Received close frame from WebSocket client")
                null
            } else {
                log.debug("Received non-text frame from WebSocket client: {}", frame.frameType)
                null
            }
        } catch (e: ClosedReceiveChannelException) {
            // Client disconnected
            log.debug("WebSocket client disconnected, closing receive loop")
            null
        } catch (e: Exception) {
            log.error("Error receiving data from WebSocket client: {}", e.message, e)
            null
        }
    }
    
    override suspend fun close(reason: String?) {
        val closeReason = reason ?: "Connection closed"
        log.debug("Closing WebSocket connection: {}", closeReason)
        session.close(CloseReason(CloseReason.Codes.GOING_AWAY, closeReason))
    }
}

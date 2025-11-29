package org.now.terminal.boundedcontexts.terminalsession.infrastructure.protocol

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.now.terminal.boundedcontexts.terminalsession.domain.TerminalSessionStatus
import org.now.terminal.boundedcontexts.terminalsession.domain.service.TerminalProcessService
import org.now.terminal.boundedcontexts.terminalsession.domain.service.TerminalSessionService

/**
 * Terminal Communication Protocol Interface
 * Defines the contract for communication between terminal server and client
 * This allows us to support multiple protocols (WebSocket, WebTransport, etc.)
 */
interface TerminalCommunicationProtocol {
    /**
     * Send data to client
     */
    suspend fun send(data: String)
    
    /**
     * Receive data from client
     */
    suspend fun receive(): String?
    
    /**
     * Close the connection
     */
    suspend fun close(reason: String? = null)
}

/**
 * Terminal Communication Handler
 * Handles the communication between terminal server and client
 * This is the main entry point for terminal communication
 */
class TerminalCommunicationHandler(
    private val sessionId: String,
    private val protocol: TerminalCommunicationProtocol,
    private val terminalSessionService: TerminalSessionService,
    private val terminalProcessService: TerminalProcessService
) {
    private val log = org.slf4j.LoggerFactory.getLogger(TerminalCommunicationHandler::class.java)
    
    suspend fun handleCommunication() {
        log.debug("Starting terminal communication for session: {}", sessionId)
        
        // Get or create terminal session
        val session = terminalSessionService.getSessionById(sessionId) ?: run {
            log.warn("Session not found: {}", sessionId)
            protocol.close("Session not found")
            return
        }
        
        log.debug("Found session: {}, shellType: {}, workingDirectory: {}", 
            sessionId, session.shellType, session.workingDirectory)
        
        // Get or create terminal process
        var process = terminalProcessService.getProcess(sessionId)
        if (process == null) {
            log.debug("Creating new terminal process for session: {}", sessionId)
            process = terminalProcessService.createProcess(sessionId, session.workingDirectory, session.shellType, session.terminalSize)
            log.debug("Created terminal process for session: {}", sessionId)
        } else {
            log.debug("Found existing terminal process for session: {}", sessionId)
        }
        
        // Add output listener to send data to client
        val outputListener: (String) -> Unit = { output ->
            try {
                // 使用结构化并发，避免创建过多的协程
                // 使用Dispatchers.IO确保send操作不会阻塞业务线程
                CoroutineScope(Dispatchers.IO).launch {
                    protocol.send(output)
                }
            } catch (e: Exception) {
                // Ignore send errors, connection might be closed
                log.debug("Error sending output to client for session {}: {}", sessionId, e.message)
            }
        }
        
        log.debug("Adding output listener for session: {}", sessionId)
        process.addOutputListener(outputListener)
        
        try {
            // Receive data from client and send to terminal process
            log.debug("Starting to receive data from client for session: {}", sessionId)
            while (true) {
                val data = protocol.receive() ?: break
                log.trace("Received data from client for session {}: {}", sessionId, data)
                
                // Enhanced error handling for write operation
                try {
                    val success = terminalProcessService.writeToProcess(sessionId, data)
                    if (!success) {
                        log.warn("Failed to write data to terminal process for session: {}", sessionId)
                        protocol.send("ERROR: Failed to write to terminal process")
                    }
                } catch (e: Exception) {
                    log.error("Error writing data to terminal process for session {}: {}", sessionId, e.message, e)
                    protocol.send("ERROR: Internal server error")
                }
            }
            log.debug("Client disconnected gracefully for session: {}", sessionId)
        } catch (e: Exception) {
            log.error("Unexpected error in terminal communication for session {}: {}", sessionId, e.message, e)
            try {
                protocol.send("ERROR: Unexpected server error")
            } catch (sendError: Exception) {
                log.debug("Failed to send error message to client for session {}: {}", sessionId, sendError.message)
            }
        } finally {
            // Cleanup
            log.debug("Cleaning up terminal communication for session: {}", sessionId)
            process.removeOutputListener(outputListener)
            
            // Update session status to inactive
            try {
                terminalSessionService.updateSessionStatus(sessionId, TerminalSessionStatus.INACTIVE)
                log.debug("Updated session {} status to INACTIVE", sessionId)
            } catch (e: Exception) {
                log.error("Failed to update session status for session {}: {}", sessionId, e.message)
            }
            
            log.debug("Cleanup completed for session: {}", sessionId)
        }
    }
}

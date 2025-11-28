package org.now.terminal.boundedcontexts.terminalsession.infrastructure.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    suspend fun handleCommunication() {
        // Get or create terminal session
        val session = terminalSessionService.getSessionById(sessionId) ?: run {
            protocol.close("Session not found")
            return
        }
        
        // Get or create terminal process
        var process = terminalProcessService.getProcess(sessionId)
        if (process == null) {
            process = terminalProcessService.createProcess(sessionId, session.workingDirectory, session.shellType, session.terminalSize)
        }
        
        // Add output listener to send data to client
        val outputListener: (String) -> Unit = { output ->
            try {
                // Run send operation in coroutine to avoid blocking
                CoroutineScope(Dispatchers.IO).launch {
                    protocol.send(output)
                }
            } catch (e: Exception) {
                // Ignore send errors, connection might be closed
            }
        }
        process.addOutputListener(outputListener)
        
        try {
            // Receive data from client and send to terminal process
            while (true) {
                val data = protocol.receive() ?: break
                terminalProcessService.writeToProcess(sessionId, data)
            }
        } finally {
            // Cleanup
            process.removeOutputListener(outputListener)
        }
    }
}

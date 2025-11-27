package org.now.terminal.infrastructure.boundedcontext.terminalsession.web.websocket

import io.ktor.websocket.*
import io.ktor.server.websocket.*
import io.ktor.websocket.Frame
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.now.terminal.boundedcontext.terminalsession.domain.TerminalSession
import org.now.terminal.boundedcontext.terminalsession.domain.valueobjects.TerminalSessionId
import org.now.terminal.boundedcontext.terminalsession.infrastructure.TerminalProcess
import org.now.terminal.boundedcontext.terminalsession.infrastructure.ProcessFactory
import org.now.terminal.infrastructure.boundedcontext.terminalsession.web.controllers.TerminalSessionController
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Real-time terminal WebSocket handler
 * Handles true real-time terminal interaction through WebSocket
 */
class RealTimeTerminalWebSocketHandler(
    private val terminalSessionController: TerminalSessionController,
    private val processFactory: ProcessFactory
) {
    private val logger = LoggerFactory.getLogger(RealTimeTerminalWebSocketHandler::class.java)
    
    // Active WebSocket connections per session
    private val activeConnections = ConcurrentHashMap<TerminalSessionId, DefaultWebSocketSession>()
    
    // Output channels for terminal processes
    private val outputChannels = ConcurrentHashMap<TerminalSessionId, Channel<String>>()
    
    // Active terminal processes per session
    private val activeProcesses = ConcurrentHashMap<TerminalSessionId, TerminalProcess>()
    
    /**
     * Handle real-time terminal WebSocket connection
     */
    suspend fun handleRealTimeConnection(sessionId: String, webSocketSession: DefaultWebSocketSession) {
        val terminalSessionId = TerminalSessionId(sessionId)
        
        // Get the terminal session
        val session = terminalSessionController.getSessionById(sessionId)
        if (session == null) {
            webSocketSession.close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Session not found"))
            return
        }
        
        if (!session.isActive) {
            webSocketSession.close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Session is not active"))
            return
        }
        
        logger.info("🔗 Real-time WebSocket connection established for session: {}", sessionId)
        
        // Store the connection
        activeConnections[terminalSessionId] = webSocketSession
        
        try {
            // Create or get the terminal process
            val terminalProcess = getOrCreateTerminalProcess(session)
            
            // Create output channel for this session
            val outputChannel = Channel<String>(Channel.UNLIMITED)
            outputChannels[terminalSessionId] = outputChannel
            
            // Start output monitoring coroutine
            val outputJob = CoroutineScope(Dispatchers.IO).launch {
                monitorTerminalOutput(terminalProcess, outputChannel)
            }
            
            // Start output sending coroutine
            val sendJob = CoroutineScope(Dispatchers.IO).launch {
                sendTerminalOutput(outputChannel, webSocketSession)
            }
            
            // Handle incoming messages from the client
            for (frame in webSocketSession.incoming) {
                if (frame is Frame.Text) {
                    val input = frame.readText()
                    logger.debug("📨 Received real-time input from WebSocket: {}", input)
                    
                    // Send input directly to the terminal process
                    terminalProcess.writeInput(input)
                }
            }
            
        } catch (e: Exception) {
            logger.error("❌ Real-time WebSocket connection error: {}", e.message)
        } finally {
            // Cleanup
            activeConnections.remove(terminalSessionId)
            outputChannels[terminalSessionId]?.close()
            outputChannels.remove(terminalSessionId)
            
            logger.info("🔌 Real-time WebSocket connection closed for session: {}", sessionId)
        }
    }
    
    /**
     * Get or create terminal process for the session
     */
    private suspend fun getOrCreateTerminalProcess(session: TerminalSession): TerminalProcess {
        var process = activeProcesses[session.id]
        
        if (process == null || !process.isAlive) {
            logger.info("🔄 Creating new terminal process for session: {}", session.id.value)
            process = processFactory.createProcess(
                shellType = session.configuration.shellType,
                workingDirectory = session.configuration.workingDirectory,
                environment = session.configuration.environmentVariables,
                terminalSize = session.configuration.terminalSize
            )
            activeProcesses[session.id] = process
        }
        
        return process
    }
    
    /**
     * Monitor terminal process output and send to channel
     */
    private suspend fun monitorTerminalOutput(process: TerminalProcess, outputChannel: Channel<String>) {
        try {
            while (process.isAlive && outputChannel.isClosedForSend.not()) {
                // Read output from the process
                val output = process.readOutput()
                if (output.isNotBlank()) {
                    outputChannel.send(output)
                }
                
                // Read error output from the process
                val errorOutput = process.readError()
                if (errorOutput.isNotBlank()) {
                    outputChannel.send(errorOutput)
                }
                
                // Small delay to prevent busy waiting
                delay(10)
            }
        } catch (e: Exception) {
            logger.error("❌ Error monitoring terminal output: {}", e.message)
        }
    }
    
    /**
     * Send terminal output to WebSocket client
     */
    private suspend fun sendTerminalOutput(outputChannel: Channel<String>, webSocketSession: DefaultWebSocketSession) {
        try {
            for (output in outputChannel) {
                if (webSocketSession.outgoing.isClosedForSend.not()) {
                    webSocketSession.send(Frame.Text(output))
                    logger.debug("📤 Sent real-time output to WebSocket: {}", output.take(50))
                }
            }
        } catch (e: Exception) {
            logger.error("❌ Error sending terminal output: {}", e.message)
        }
    }
    
    /**
     * Resize terminal for a session
     */
    suspend fun resizeTerminal(sessionId: String, rows: Int, cols: Int) {
        val terminalSessionId = TerminalSessionId(sessionId)
        val process = activeProcesses[terminalSessionId]
        
        if (process != null && process.isAlive) {
            process.resizeTerminal(rows, cols)
            logger.info("📐 Resized terminal for session {} to {}x{}", sessionId, cols, rows)
        } else {
            logger.warn("⚠️ Cannot resize terminal for session {}: process not found or not alive", sessionId)
        }
    }
    
    /**
     * Terminate terminal process for a session
     */
    suspend fun terminateTerminal(sessionId: String) {
        val terminalSessionId = TerminalSessionId(sessionId)
        val process = activeProcesses[terminalSessionId]
        
        if (process != null && process.isAlive) {
            process.terminate()
            process.waitFor()
            activeProcesses.remove(terminalSessionId)
        }
        
        // Close WebSocket connection if active
        activeConnections[terminalSessionId]?.close()
        activeConnections.remove(terminalSessionId)
        
        // Close output channel
        outputChannels[terminalSessionId]?.close()
        outputChannels.remove(terminalSessionId)
        
        logger.info("🛑 Terminated terminal process for session: {}", sessionId)
    }
}
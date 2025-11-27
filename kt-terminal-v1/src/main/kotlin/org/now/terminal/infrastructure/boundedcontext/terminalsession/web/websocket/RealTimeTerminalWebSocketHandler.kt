package org.now.terminal.infrastructure.boundedcontext.terminalsession.web.websocket

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.now.terminal.boundedcontext.terminalsession.domain.TerminalSession
import org.now.terminal.boundedcontext.terminalsession.domain.valueobjects.TerminalSessionId
import org.now.terminal.boundedcontext.terminalsession.infrastructure.ProcessFactory
import org.now.terminal.boundedcontext.terminalsession.infrastructure.TerminalProcess
import org.now.terminal.infrastructure.boundedcontext.terminalsession.process.Pty4jTerminalProcess
import org.now.terminal.infrastructure.boundedcontext.terminalsession.web.controllers.TerminalSessionController
import org.slf4j.LoggerFactory

/**
 * Real-time terminal WebSocket handler
 * Handles true real-time terminal interaction through WebSocket
 *
 * Note: This handler only processes terminal input/output streams.
 * Control commands (resize, terminate, etc.) should be handled via API endpoints.
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
     * Monitor terminal process output and send to channel using async channels
     */
    private suspend fun monitorTerminalOutput(process: TerminalProcess, outputChannel: Channel<String>) {
        try {
            // Cast to Pty4jTerminalProcess to access async channels
            val ptyProcess = process as Pty4jTerminalProcess

            // Use async channels for real-time streaming
            val processOutputChannel = ptyProcess.getOutputChannel()
            val processErrorChannel = ptyProcess.getErrorChannel()

            // Launch separate coroutines for each channel
            val outputJob = CoroutineScope(Dispatchers.IO).launch {
                for (output in processOutputChannel) {
                    if (output.isNotBlank() && outputChannel.isClosedForSend.not()) {
                        outputChannel.send(output)
                        logger.debug("📤 Sent async output to WebSocket: {}", output.take(50))
                    }
                }
            }

            val errorJob = CoroutineScope(Dispatchers.IO).launch {
                for (error in processErrorChannel) {
                    if (error.isNotBlank() && outputChannel.isClosedForSend.not()) {
                        outputChannel.send(error)
                        logger.debug("📤 Sent async error to WebSocket: {}", error.take(50))
                    }
                }
            }

            // Wait for both jobs to complete or process to die
            joinAll(outputJob, errorJob)

            // Cancel the jobs when done
            outputJob.cancel()
            errorJob.cancel()

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
                webSocketSession.send(Frame.Text(output))
                logger.debug("📤 Sent real-time output to WebSocket: {}", output.take(50))
            }
        } catch (e: Exception) {
            logger.error("❌ Error sending terminal output: {}", e.message)
        }
    }


}
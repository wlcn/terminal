package org.now.terminal.infrastructure.boundedcontext.terminalsession.web.controllers

import io.ktor.websocket.*
import io.ktor.server.websocket.*
import io.ktor.websocket.Frame
import org.now.terminal.boundedcontext.terminalsession.application.usecases.dtos.ExecuteTerminalCommand
import org.now.terminal.boundedcontext.terminalsession.application.usecases.dtos.CreateTerminalSessionCommand
import org.now.terminal.boundedcontext.terminalsession.application.usecases.dtos.GetTerminalSessionByIdQuery
import org.now.terminal.boundedcontext.terminalsession.application.usecases.dtos.GetUserTerminalSessionsQuery
import org.now.terminal.boundedcontext.terminalsession.application.usecases.dtos.TerminateTerminalSessionCommand
import org.now.terminal.boundedcontext.terminalsession.application.usecases.TerminalSessionManagementUseCase
import org.now.terminal.boundedcontext.terminalsession.application.usecases.TerminalSessionQueryUseCase
import org.now.terminal.boundedcontext.terminalsession.application.usecases.ExecuteTerminalCommandUseCase
import org.now.terminal.boundedcontext.terminalsession.domain.TerminalSession
import org.now.terminal.boundedcontext.terminalsession.domain.valueobjects.TerminalSessionId
import org.now.terminal.boundedcontext.terminalsession.domain.valueobjects.SessionStatus
import org.now.terminal.boundedcontext.terminalsession.domain.valueobjects.ShellType
import org.now.terminal.infrastructure.boundedcontext.terminalsession.web.websocket.RealTimeTerminalWebSocketHandler
import org.now.terminal.boundedcontext.terminalsession.infrastructure.ProcessFactory
import org.now.terminal.shared.valueobjects.UserId
import org.slf4j.LoggerFactory

/**
 * Terminal Session Controller
 * Infrastructure layer implementation that handles HTTP requests for terminal sessions
 */
class TerminalSessionController(
    private val terminalSessionManagementUseCase: TerminalSessionManagementUseCase,
    private val terminalSessionQueryUseCase: TerminalSessionQueryUseCase,
    private val executeTerminalCommandUseCase: ExecuteTerminalCommandUseCase,
    private val processFactory: ProcessFactory
) {
    private val logger = LoggerFactory.getLogger(TerminalSessionController::class.java)

    
    /**
     * Create a new terminal session
     */
    suspend fun createSession(
        userId: String,
        title: String?,
        workingDirectory: String?
    ): TerminalSession {
        return terminalSessionManagementUseCase.createSession(
            userId = UserId(userId),
            title = title,
            workingDirectory = workingDirectory
        )
    }
    
    /**
     * Get terminal session by ID
     */
    suspend fun getSessionById(sessionId: String): TerminalSession? {
        return terminalSessionQueryUseCase.getSessionById(TerminalSessionId(sessionId))
    }
    
    /**
     * Get all terminal sessions for a user
     */
    suspend fun getUserSessions(
        userId: String,
        includeInactive: Boolean = false
    ): List<TerminalSession> {
        return terminalSessionQueryUseCase.getUserSessions(UserId(userId), includeInactive)
    }
    
    /**
     * Get active terminal sessions for a user
     */
    suspend fun getActiveUserSessions(userId: String): List<TerminalSession> {
        return terminalSessionQueryUseCase.getActiveUserSessions(userId)
    }
    
    /**
     * Terminate a terminal session
     */
    suspend fun terminateSession(sessionId: String): Boolean {
        return terminalSessionManagementUseCase.terminateSession(TerminalSessionId(sessionId))
    }
    
    /**
     * Terminate all active sessions for a user
     */
    suspend fun terminateAllUserSessions(userId: String): Int {
        return terminalSessionManagementUseCase.terminateAllUserSessions(UserId(userId))
    }
    
    /**
     * Count active sessions for a user
     */
    suspend fun countActiveSessions(userId: String): Int {
        return terminalSessionQueryUseCase.countActiveSessions(userId)
    }
    
    /**
     * Execute a command in a terminal session
     */
    suspend fun executeCommand(
        sessionId: String,
        command: String,
        timeoutMs: Long = 30000L
    ): String {
        val result = executeTerminalCommandUseCase.execute(
            sessionId = TerminalSessionId(sessionId),
            command = command,
            timeoutMs = timeoutMs
        )
        
        if (!result.isSuccess) {
            throw RuntimeException("Command execution failed with exit code ${result.exitCode}: ${result.errorOutput}")
        }
        
        return result.output
    }
    
    /**
     * Execute a command and check if it was successful
     */
    suspend fun executeCommandAndCheckSuccess(
        sessionId: String,
        command: String,
        timeoutMs: Long = 30000L
    ): Boolean {
        val result = executeTerminalCommandUseCase.execute(
            sessionId = TerminalSessionId(sessionId),
            command = command,
            timeoutMs = timeoutMs
        )
        return result.isSuccess
    }
    
    /**
     * Get session status
     */
    suspend fun getSessionStatus(sessionId: String): String {
        val session = getSessionById(sessionId)
        return session?.status?.name ?: "NOT_FOUND"
    }
    
    /**
     * Handle WebSocket connection for a terminal session
     */
    suspend fun handleWebSocketConnection(sessionId: String, webSocketSession: DefaultWebSocketSession) {
        val session = getSessionById(sessionId)
        if (session == null) {
            webSocketSession.close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Session not found"))
            return
        }
        
        if (!session.isActive) {
            webSocketSession.close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Session is not active"))
            return
        }
        
        logger.info("🔗 WebSocket connection established for session: {}", sessionId)
        
        // Use real-time terminal WebSocket handler for true terminal interaction
        val realTimeHandler = RealTimeTerminalWebSocketHandler(this, processFactory)
        realTimeHandler.handleRealTimeConnection(sessionId, webSocketSession)
    }
}
package org.now.terminal.boundedcontext.terminalsession.application.usecases

import org.now.terminal.boundedcontext.terminalsession.domain.TerminalSession
import org.now.terminal.boundedcontext.terminalsession.domain.repositories.TerminalSessionRepository
import org.now.terminal.boundedcontext.terminalsession.domain.valueobjects.SessionConfiguration
import org.now.terminal.boundedcontext.terminalsession.domain.valueobjects.SessionStatus
import org.now.terminal.boundedcontext.terminalsession.domain.valueobjects.ShellType
import org.now.terminal.boundedcontext.terminalsession.domain.valueobjects.TerminalSessionId
import org.now.terminal.boundedcontext.terminalsession.domain.valueobjects.TerminalSize
import org.now.terminal.shared.valueobjects.UserId
import java.time.Instant
import org.now.terminal.boundedcontext.terminalsession.application.usecases.dtos.CreateTerminalSessionCommand
import org.now.terminal.boundedcontext.terminalsession.application.usecases.dtos.TerminateTerminalSessionCommand

/**
 * Use case for managing terminal sessions (create, terminate, etc.)
 */
class TerminalSessionManagementUseCase(
    private val terminalSessionRepository: TerminalSessionRepository
) {
    
    /**
     * Create a new terminal session
     */
    suspend fun createSession(command: CreateTerminalSessionCommand): TerminalSession {
        val defaultConfiguration = SessionConfiguration(
            shellType = ShellType.BASH,
            workingDirectory = command.workingDirectory ?: System.getProperty("user.home"),
            terminalSize = TerminalSize.DEFAULT,
            environmentVariables = emptyMap()
        )
        
        val session = TerminalSession(
            id = TerminalSessionId.generate(),
            userId = command.userId,
            title = command.title ?: TerminalSession.generateDefaultTitle(),
            hostname = java.net.InetAddress.getLocalHost().hostName,
            configuration = defaultConfiguration,
            status = SessionStatus.ACTIVE,
            commandHistory = emptyList(),
            outputBuffer = "",
            startedAt = Instant.now(),
            lastActivityAt = Instant.now(),
            terminatedAt = null
        )
        
        return terminalSessionRepository.save(session)
    }
    
    /**
     * Create a new terminal session (direct call version)
     */
    suspend fun createSession(userId: UserId, 
                              title: String? = null, 
                              workingDirectory: String? = null): TerminalSession {
        val command = CreateTerminalSessionCommand(
            userId = userId,
            title = title,
            workingDirectory = workingDirectory
        )
        return createSession(command)
    }
    
    /**
     * Terminate a terminal session
     */
    suspend fun terminateSession(command: TerminateTerminalSessionCommand): Boolean {
        val session = terminalSessionRepository.findById(command.sessionId)
        
        if (session != null && session.isActive) {
            val terminatedSession = session.terminate()
            terminalSessionRepository.save(terminatedSession)
            return true
        }
        
        return false
    }
    
    /**
     * Terminate a terminal session (direct call version)
     */
    suspend fun terminateSession(sessionId: TerminalSessionId): Boolean {
        val command = TerminateTerminalSessionCommand(sessionId = sessionId)
        return terminateSession(command)
    }
    
    /**
     * Terminate all active sessions for a user
     */
    suspend fun terminateAllUserSessions(userId: UserId): Int {
        val activeSessions = terminalSessionRepository.findActiveSessionsByUserId(userId)
        
        activeSessions.forEach { session ->
            val terminatedSession = session.terminate()
            terminalSessionRepository.save(terminatedSession)
        }
        
        return activeSessions.size
    }
}
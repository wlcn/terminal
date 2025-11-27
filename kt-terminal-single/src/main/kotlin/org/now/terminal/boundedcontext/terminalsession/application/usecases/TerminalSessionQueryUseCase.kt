package org.now.terminal.boundedcontext.terminalsession.application.usecases

import org.now.terminal.boundedcontext.terminalsession.application.usecases.dtos.GetTerminalSessionByIdQuery
import org.now.terminal.boundedcontext.terminalsession.application.usecases.dtos.GetUserTerminalSessionsQuery
import org.now.terminal.boundedcontext.terminalsession.domain.TerminalSession
import org.now.terminal.boundedcontext.terminalsession.domain.repositories.TerminalSessionRepository

/**
 * Use case for querying terminal sessions
 */
class TerminalSessionQueryUseCase(
    private val terminalSessionRepository: TerminalSessionRepository
) {
    
    /**
     * Get terminal session by ID
     */
    suspend fun getSessionById(query: GetTerminalSessionByIdQuery): TerminalSession? {
        return terminalSessionRepository.findById(query.sessionId)
    }
    
    /**
     * Get all terminal sessions for a user
     */
    suspend fun getUserSessions(query: GetUserTerminalSessionsQuery): List<TerminalSession> {
        val sessions = terminalSessionRepository.findByUserId(query.userId)
        
        return if (query.includeInactive) {
            sessions
        } else {
            sessions.filter { it.isActive }
        }
    }
    
    /**
     * Get active terminal sessions for a user
     */
    suspend fun getActiveUserSessions(userId: String): List<TerminalSession> {
        return terminalSessionRepository.findActiveSessionsByUserId(org.now.terminal.shared.valueobjects.UserId(userId))
    }
    
    /**
     * Count active sessions for a user
     */
    suspend fun countActiveSessions(userId: String): Int {
        return terminalSessionRepository.countActiveSessionsByUserId(org.now.terminal.shared.valueobjects.UserId(userId))
    }
}
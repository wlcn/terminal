package org.now.terminal.boundedcontext.terminalsession.domain.repositories

import org.now.terminal.boundedcontext.terminalsession.domain.TerminalSession
import org.now.terminal.boundedcontext.terminalsession.domain.valueobjects.TerminalSessionId
import org.now.terminal.shared.valueobjects.UserId

/**
 * Terminal Session Repository Interface
 */
interface TerminalSessionRepository {

    /**
     * Save a terminal session
     */
    suspend fun save(session: TerminalSession): TerminalSession

    /**
     * Find session by ID
     */
    suspend fun findById(sessionId: TerminalSessionId): TerminalSession?

    /**
     * Find all sessions for a user
     */
    suspend fun findByUserId(userId: UserId): List<TerminalSession>

    /**
     * Find active sessions for a user
     */
    suspend fun findActiveSessionsByUserId(userId: UserId): List<TerminalSession>

    /**
     * Delete a session
     */
    suspend fun delete(sessionId: TerminalSessionId): Boolean

    /**
     * Count active sessions for a user
     */
    suspend fun countActiveSessionsByUserId(userId: UserId): Int
}
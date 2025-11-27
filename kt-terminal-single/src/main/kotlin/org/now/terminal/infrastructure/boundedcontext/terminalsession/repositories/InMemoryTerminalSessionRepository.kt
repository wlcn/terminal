package org.now.terminal.infrastructure.boundedcontext.terminalsession.repositories

import org.now.terminal.boundedcontext.terminalsession.domain.TerminalSession
import org.now.terminal.boundedcontext.terminalsession.domain.repositories.TerminalSessionRepository
import org.now.terminal.boundedcontext.terminalsession.domain.valueobjects.TerminalSessionId
import org.now.terminal.shared.valueobjects.UserId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Simple in-memory terminal session repository implementation
 * Uses ConcurrentHashMap for thread-safe operations
 */
class InMemoryTerminalSessionRepository : TerminalSessionRepository {
    
    private val sessions = ConcurrentHashMap<String, TerminalSession>()
    private val activeSessionCounts = ConcurrentHashMap<String, AtomicInteger>()
    
    override suspend fun save(session: TerminalSession): TerminalSession {
        sessions[session.id.value] = session
        
        // Update active session count for the user
        if (session.isActive) {
            activeSessionCounts.computeIfAbsent(session.userId.value) { AtomicInteger(0) }.incrementAndGet()
        }
        
        return session
    }
    
    override suspend fun findById(sessionId: TerminalSessionId): TerminalSession? {
        return sessions[sessionId.value]
    }
    
    override suspend fun findByUserId(userId: UserId): List<TerminalSession> {
        return sessions.values.filter { it.userId == userId }
    }
    
    override suspend fun findActiveSessionsByUserId(userId: UserId): List<TerminalSession> {
        return sessions.values.filter { it.userId == userId && it.isActive }
    }
    
    override suspend fun delete(sessionId: TerminalSessionId): Boolean {
        val session = sessions[sessionId.value]
        if (session != null) {
            sessions.remove(sessionId.value)
            
            // Update active session count for the user
            if (session.isActive) {
                activeSessionCounts[session.userId.value]?.decrementAndGet()
            }
            
            return true
        }
        return false
    }
    
    override suspend fun countActiveSessionsByUserId(userId: UserId): Int {
        return activeSessionCounts[userId.value]?.get() ?: 0
    }
    
    /**
     * Clear all sessions (useful for testing)
     */
    suspend fun clear() {
        sessions.clear()
        activeSessionCounts.clear()
    }
    
    /**
     * Get total number of sessions
     */
    suspend fun countAll(): Int {
        return sessions.size
    }
    
    /**
     * Get all sessions (for debugging and testing)
     */
    suspend fun findAll(): List<TerminalSession> {
        return sessions.values.toList()
    }
    
    /**
     * Find sessions by status
     */
    suspend fun findByStatus(active: Boolean): List<TerminalSession> {
        return sessions.values.filter { it.isActive == active }
    }
}
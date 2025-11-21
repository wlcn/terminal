package org.now.terminal.session.infrastructure.repositories

import org.now.terminal.session.domain.entities.TerminalSession
import org.now.terminal.session.domain.repositories.TerminalSessionRepository
import org.now.terminal.shared.valueobjects.SessionId
import org.now.terminal.shared.valueobjects.UserId
import jakarta.inject.Singleton

/**
 * 内存终端会话仓储实现
 */
@Singleton
class InMemoryTerminalSessionRepository : TerminalSessionRepository {
    
    private val sessions = mutableMapOf<SessionId, TerminalSession>()
    
    override fun save(session: TerminalSession): TerminalSession {
        sessions[session.sessionId] = session
        return session
    }
    
    override fun findById(sessionId: SessionId): TerminalSession? {
        return sessions[sessionId]
    }
    
    override fun findByUserId(userId: UserId): List<TerminalSession> {
        return sessions.values.filter { it.userId == userId }
    }
    
    override fun delete(sessionId: SessionId) {
        sessions.remove(sessionId)
    }
    
    override fun findAllActive(): List<TerminalSession> {
        return sessions.values.filter { it.isAlive() }
    }
}
package org.now.terminal.boundedcontexts.terminalsession.domain.service

import org.now.terminal.boundedcontexts.terminalsession.domain.model.TerminalSession
import org.now.terminal.boundedcontexts.terminalsession.domain.model.TerminalSessionStatus
import org.now.terminal.boundedcontexts.terminalsession.domain.model.TerminalSize
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class TerminalSessionService {
    private val sessions = ConcurrentHashMap<UUID, TerminalSession>()
    
    fun createSession(
        userId: String,
        title: String?,
        workingDirectory: String,
        shellType: String = "bash"
    ): TerminalSession {
        val session = TerminalSession(
            id = UUID.randomUUID(),
            userId = userId,
            title = title,
            workingDirectory = workingDirectory,
            shellType = shellType,
            status = TerminalSessionStatus.ACTIVE
        )
        sessions[session.id] = session
        return session
    }
    
    fun getSessionById(id: UUID): TerminalSession? {
        return sessions[id]
    }
    
    fun getSessionsByUserId(userId: String): List<TerminalSession> {
        return sessions.values.filter { it.userId == userId }
    }
    
    fun getAllSessions(): List<TerminalSession> {
        return sessions.values.toList()
    }
    
    fun resizeTerminal(id: UUID, columns: Int, rows: Int): TerminalSession? {
        return sessions[id]?.also {
            it.terminalSize = TerminalSize(columns, rows)
            it.updatedAt = System.currentTimeMillis()
        }
    }
    
    fun terminateSession(id: UUID, reason: String? = null): TerminalSession? {
        return sessions[id]?.also {
            it.status = TerminalSessionStatus.TERMINATED
            it.updatedAt = System.currentTimeMillis()
        }
    }
    
    fun updateSessionStatus(id: UUID, status: TerminalSessionStatus): TerminalSession? {
        return sessions[id]?.also {
            it.status = status
            it.updatedAt = System.currentTimeMillis()
        }
    }
    
    fun deleteSession(id: UUID): Boolean {
        return sessions.remove(id) != null
    }
}

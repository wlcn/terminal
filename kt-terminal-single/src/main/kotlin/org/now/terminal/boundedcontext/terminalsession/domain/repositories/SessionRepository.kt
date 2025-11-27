package org.now.terminal.boundedcontext.terminalsession.domain.repositories

import org.now.terminal.boundedcontext.terminalsession.domain.SessionId
import org.now.terminal.boundedcontext.terminalsession.domain.TerminalSession
import org.now.terminal.boundedcontext.user.domain.UserId

/**
 * 会话仓库接口
 */
interface SessionRepository {
    suspend fun save(session: TerminalSession)
    suspend fun findById(id: SessionId): TerminalSession?
    suspend fun getActiveSessionsByUser(userId: UserId): List<TerminalSession>
    suspend fun delete(id: SessionId)
}
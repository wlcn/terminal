package org.now.terminal.session.domain.repositories

import org.now.terminal.session.domain.entities.TerminalSession
import org.now.terminal.shared.valueobjects.SessionId
import org.now.terminal.shared.valueobjects.UserId

/**
 * 终端会话仓储接口
 */
interface TerminalSessionRepository {
    
    /**
     * 保存会话
     */
    fun save(session: TerminalSession): TerminalSession
    
    /**
     * 根据ID查找会话
     */
    fun findById(sessionId: SessionId): TerminalSession?
    
    /**
     * 根据用户ID查找活跃会话
     */
    fun findByUserId(userId: UserId): List<TerminalSession>
    
    /**
     * 删除会话
     */
    fun delete(sessionId: SessionId)
    
    /**
     * 获取所有活跃会话
     */
    fun findAllActive(): List<TerminalSession>
}
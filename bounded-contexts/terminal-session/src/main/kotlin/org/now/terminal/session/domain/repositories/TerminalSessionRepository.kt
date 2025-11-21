package org.now.terminal.session.domain.repositories

import org.now.terminal.session.domain.aggregates.SessionAggregate
import org.now.terminal.shared.valueobjects.SessionId

/**
 * 终端会话仓储接口
 * 定义会话聚合根的持久化操作
 */
interface TerminalSessionRepository {
    
    /**
     * 根据ID查找会话
     */
    fun findById(sessionId: SessionId): SessionAggregate?
    
    /**
     * 保存会话
     */
    fun save(session: SessionAggregate): SessionAggregate
    
    /**
     * 删除会话
     */
    fun delete(sessionId: SessionId)
    
    /**
     * 获取所有会话
     */
    fun findAll(): List<SessionAggregate>
    
    /**
     * 根据用户ID查找会话
     */
    fun findByUserId(userId: UserId): List<SessionAggregate>
    
    /**
     * 检查会话是否存在
     */
    fun existsById(sessionId: SessionId): Boolean
    
    /**
     * 获取活跃会话数量
     */
    fun countActiveSessions(): Long
    
    /**
     * 获取用户活跃会话数量
     */
    fun countActiveSessionsByUser(userId: UserId): Long
}

/**
 * 用户ID值对象（仓储接口中使用）
 */
@JvmInline
value class UserId(val value: String)
package org.now.terminal.session.domain.services

import org.now.terminal.shared.valueobjects.SessionId

/**
 * 终端输出发布器接口
 * 业务模块只依赖此接口，不关心具体实现
 */
interface TerminalOutputPublisher {
    
    /**
     * 发布终端输出
     * @param sessionId 会话ID
     * @param output 输出内容
     */
    suspend fun publishOutput(sessionId: SessionId, output: String)
    
    /**
     * 获取活跃会话数量
     * @return 活跃会话数量
     */
    suspend fun getActiveSessionCount(): Int
    
    /**
     * 检查会话是否已连接
     * @param sessionId 会话ID
     * @return 是否已连接
     */
    suspend fun isSessionConnected(sessionId: SessionId): Boolean
    
    /**
     * 关闭所有会话连接
     */
    suspend fun closeAllSessions()
}
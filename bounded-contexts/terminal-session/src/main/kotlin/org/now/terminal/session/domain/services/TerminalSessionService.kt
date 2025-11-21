package org.now.terminal.session.domain.services

import org.now.terminal.shared.valueobjects.SessionId
import org.now.terminal.shared.valueobjects.UserId
import org.now.terminal.session.domain.valueobjects.PtyConfiguration
import org.now.terminal.session.domain.valueobjects.TerminalSize
import org.now.terminal.session.domain.valueobjects.TerminationReason

/**
 * 终端会话服务接口
 * 定义会话生命周期管理的核心业务操作
 */
interface TerminalSessionService {
    
    /**
     * 创建新的终端会话
     */
    fun createSession(userId: UserId, ptyConfig: PtyConfiguration): SessionId
    
    /**
     * 终止会话
     */
    fun terminateSession(sessionId: SessionId, reason: TerminationReason)
    
    /**
     * 处理终端输入
     */
    fun handleInput(sessionId: SessionId, input: String)
    
    /**
     * 调整终端尺寸
     */
    fun resizeTerminal(sessionId: SessionId, size: TerminalSize)
    
    /**
     * 列出活跃会话
     */
    fun listActiveSessions(userId: UserId): List<org.now.terminal.session.domain.entities.TerminalSession>
    
    /**
     * 读取会话输出
     */
    fun readOutput(sessionId: SessionId): String
    
    /**
     * 获取会话统计信息
     */
    fun getSessionStatistics(sessionId: SessionId): org.now.terminal.session.domain.entities.SessionStatistics
    
    /**
     * 强制终止所有用户会话
     */
    fun terminateAllUserSessions(userId: UserId, reason: TerminationReason)
    
    /**
     * 检查会话是否存在且活跃
     */
    fun isSessionActive(sessionId: SessionId): Boolean
    
    /**
     * 获取会话配置
     */
    fun getSessionConfiguration(sessionId: SessionId): PtyConfiguration
}
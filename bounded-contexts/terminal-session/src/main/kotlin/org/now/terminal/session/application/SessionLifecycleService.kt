package org.now.terminal.session.application

import org.now.terminal.infrastructure.eventbus.EventBus
import org.now.terminal.session.domain.entities.SessionStatistics
import org.now.terminal.session.domain.entities.TerminalSession
import org.now.terminal.session.domain.repositories.TerminalSessionRepository
import org.now.terminal.session.domain.services.ProcessFactory
import org.now.terminal.session.domain.services.TerminalSessionService
import org.now.terminal.shared.valueobjects.SessionId
import org.now.terminal.shared.valueobjects.UserId
import org.now.terminal.session.domain.valueobjects.TerminationReason
import org.now.terminal.session.domain.valueobjects.PtyConfiguration
import org.now.terminal.session.domain.valueobjects.TerminalSize
import jakarta.inject.Singleton

/**
 * 会话生命周期管理服务
 * 实现TerminalSessionService接口
 */
@Singleton
class SessionLifecycleService(
    private val eventBus: EventBus,
    private val sessionRepository: TerminalSessionRepository,
    private val processFactory: ProcessFactory
) : TerminalSessionService {
    
    /**
     * 创建新的终端会话
     */
    override fun createSession(
        userId: UserId,
        ptyConfig: PtyConfiguration
    ): SessionId {
        val sessionId = SessionId.generate()
        val session = TerminalSession(
            sessionId = sessionId,
            userId = userId,
            ptyConfig = ptyConfig,
            eventBus = eventBus,
            processFactory = processFactory
        )
        
        session.start()
        sessionRepository.save(session)
        return sessionId
    }
    
    /**
     * 终止会话
     */
    override fun terminateSession(sessionId: SessionId, reason: TerminationReason) {
        val session = sessionRepository.findById(sessionId)
            ?: throw IllegalArgumentException("Session not found: $sessionId")
        
        session.terminate(reason)
    }
    
    /**
     * 处理终端输入
     */
    override fun handleInput(sessionId: SessionId, input: String) {
        val session = sessionRepository.findById(sessionId)
            ?: throw IllegalArgumentException("Session not found: $sessionId")
        
        session.handleInput(input)
    }
    
    /**
     * 调整终端尺寸
     */
    override fun resizeTerminal(sessionId: SessionId, size: TerminalSize) {
        val session = sessionRepository.findById(sessionId)
            ?: throw IllegalArgumentException("Session not found: $sessionId")
        
        session.resize(size)
    }
    
    /**
     * 列出活跃会话
     */
    override fun listActiveSessions(userId: UserId): List<TerminalSession> {
        return sessionRepository.findByUserId(userId)
            .filter { it.isAlive() }
    }
    
    /**
     * 读取会话输出
     */
    fun readOutput(sessionId: SessionId): String {
        val session = sessionRepository.findById(sessionId)
            ?: throw IllegalArgumentException("Session not found: $sessionId")
        
        return session.readOutput()
    }
    
    /**
     * 获取会话统计信息
     */
    fun getSessionStatistics(sessionId: SessionId): SessionStatistics {
        val session = sessionRepository.findById(sessionId)
            ?: throw IllegalArgumentException("Session not found: $sessionId")
        
        return session.getStatistics()
    }
    
    /**
     * 强制终止所有用户会话
     */
    fun terminateAllUserSessions(userId: UserId, reason: TerminationReason) {
        val userSessions = sessionRepository.findByUserId(userId)
        userSessions.forEach { session ->
            if (session.canTerminate()) {
                session.terminate(reason)
            }
        }
    }
    
    /**
     * 检查会话是否存在且活跃
     */
    override fun isSessionActive(sessionId: SessionId): Boolean {
        val session = sessionRepository.findById(sessionId)
        return session?.isAlive() ?: false
    }
    
    /**
     * 获取会话配置
     */
    override fun getSessionConfiguration(sessionId: SessionId): PtyConfiguration {
        val session = sessionRepository.findById(sessionId)
            ?: throw IllegalArgumentException("Session not found: $sessionId")
        
        return session.getConfiguration()
    }
}
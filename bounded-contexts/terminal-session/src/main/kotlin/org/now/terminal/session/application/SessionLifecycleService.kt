package org.now.terminal.session.application

import org.now.terminal.infrastructure.eventbus.EventBus
import org.now.terminal.session.domain.entities.TerminalSession
import org.now.terminal.session.domain.repositories.TerminalSessionRepository
import org.now.terminal.shared.valueobjects.SessionId
import org.now.terminal.shared.valueobjects.UserId
import org.now.terminal.session.domain.valueobjects.TerminationReason
import org.now.terminal.session.domain.valueobjects.PtyConfiguration
import org.now.terminal.session.domain.valueobjects.TerminalSize

/**
 * 会话生命周期管理服务
 */
class SessionLifecycleService(
    private val eventBus: EventBus,
    private val sessionRepository: TerminalSessionRepository
) {
    
    /**
     * 创建新的终端会话
     */
    fun createSession(
        userId: UserId,
        ptyConfig: PtyConfiguration
    ): SessionId {
        val sessionId = SessionId.generate()
        val session = TerminalSession(
            sessionId = sessionId,
            userId = userId,
            ptyConfig = ptyConfig,
            eventBus = eventBus,
            processFactory = TODO("需要注入ProcessFactory")
        )
        
        session.start()
        sessionRepository.save(session)
        return sessionId
    }
    
    /**
     * 终止会话
     */
    fun terminateSession(sessionId: SessionId, reason: TerminationReason) {
        val session = sessionRepository.findById(sessionId)
            ?: throw IllegalArgumentException("Session not found: $sessionId")
        
        session.terminate(reason)
    }
    
    /**
     * 处理终端输入
     */
    fun handleInput(sessionId: SessionId, input: String) {
        val session = sessionRepository.findById(sessionId)
            ?: throw IllegalArgumentException("Session not found: $sessionId")
        
        session.handleInput(input)
    }
    
    /**
     * 调整终端尺寸
     */
    fun resizeTerminal(sessionId: SessionId, size: TerminalSize) {
        val session = sessionRepository.findById(sessionId)
            ?: throw IllegalArgumentException("Session not found: $sessionId")
        
        session.resize(size)
    }
    
    /**
     * 列出活跃会话
     */
    fun listActiveSessions(userId: UserId): List<TerminalSession> {
        return sessionRepository.findByUserId(userId)
            .filter { it.isAlive() }
    }
}
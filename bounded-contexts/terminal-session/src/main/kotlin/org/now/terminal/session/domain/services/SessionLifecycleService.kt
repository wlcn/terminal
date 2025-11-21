package org.now.terminal.session.domain.services

import org.now.sharedkernel.domain.DomainEventPublisher
import org.now.sharedkernel.domain.SessionId
import org.now.sharedkernel.domain.UserId
import org.now.terminal.session.domain.TerminalSession
import org.now.terminal.session.domain.TerminationReason
import org.now.terminal.session.domain.valueobjects.PtyConfiguration

/**
 * 会话生命周期管理服务
 */
class SessionLifecycleService(
    private val eventPublisher: DomainEventPublisher
) {
    
    /**
     * 创建新的终端会话
     */
    fun createSession(
        sessionId: SessionId,
        userId: UserId,
        ptyConfig: PtyConfiguration
    ): TerminalSession {
        val session = TerminalSession(
            sessionId = sessionId,
            userId = userId,
            ptyConfig = ptyConfig,
            eventPublisher = eventPublisher
        )
        
        session.start()
        return session
    }
    
    /**
     * 终止会话
     */
    fun terminateSession(session: TerminalSession, reason: TerminationReason) {
        session.terminate(reason)
    }
}
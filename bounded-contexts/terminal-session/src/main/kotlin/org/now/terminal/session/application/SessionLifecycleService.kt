package org.now.terminal.session.application

import org.now.terminal.infrastructure.eventbus.EventBus
import org.now.terminal.shared.events.Event
import org.now.terminal.shared.valueobjects.SessionId
import org.now.terminal.shared.valueobjects.UserId
import org.now.terminal.session.domain.entities.TerminalSession
import org.now.terminal.session.domain.valueobjects.TerminationReason
import org.now.terminal.session.domain.valueobjects.PtyConfiguration

/**
 * 会话生命周期管理服务
 */
class SessionLifecycleService(
    private val eventBus: EventBus
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
            ptyConfig = ptyConfig
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
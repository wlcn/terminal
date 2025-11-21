package org.now.terminal.session.application.usecases

import org.now.terminal.session.domain.entities.TerminalSession
import org.now.terminal.session.domain.services.SessionLifecycleService
import org.now.terminal.shared.valueobjects.SessionId
import org.now.terminal.shared.valueobjects.UserId
import org.now.terminal.session.domain.valueobjects.PtyConfiguration
import jakarta.inject.Singleton

/**
 * 创建会话用例
 */
@Singleton
class CreateSessionUseCase(
    private val sessionLifecycleService: SessionLifecycleService
) {
    
    fun execute(
        userId: UserId,
        ptyConfig: PtyConfiguration
    ): SessionId {
        return sessionLifecycleService.createSession(userId, ptyConfig)
    }
}
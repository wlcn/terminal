package org.now.terminal.session.application.usecases

import org.now.terminal.session.domain.entities.TerminalSession
import org.now.terminal.session.domain.services.SessionLifecycleService
import org.now.terminal.shared.valueobjects.UserId
import jakarta.inject.Singleton

/**
 * 列出活跃会话用例
 */
@Singleton
class ListActiveSessionsUseCase(
    private val sessionLifecycleService: SessionLifecycleService
) {
    
    fun execute(userId: UserId): List<TerminalSession> {
        return sessionLifecycleService.listActiveSessions(userId)
    }
}
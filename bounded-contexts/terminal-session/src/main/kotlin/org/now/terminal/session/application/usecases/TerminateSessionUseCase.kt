package org.now.terminal.session.application.usecases

import org.now.terminal.session.domain.services.SessionLifecycleService
import org.now.terminal.shared.valueobjects.SessionId
import org.now.terminal.session.domain.valueobjects.TerminationReason
import jakarta.inject.Singleton

/**
 * 终止会话用例
 */
@Singleton
class TerminateSessionUseCase(
    private val sessionLifecycleService: SessionLifecycleService
) {
    
    fun execute(sessionId: SessionId, reason: TerminationReason) {
        sessionLifecycleService.terminateSession(sessionId, reason)
    }
}
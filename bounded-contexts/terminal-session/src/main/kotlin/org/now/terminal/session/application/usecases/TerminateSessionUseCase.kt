package org.now.terminal.session.application.usecases

import org.now.terminal.session.domain.services.TerminalSessionService
import org.now.terminal.shared.valueobjects.SessionId
import org.now.terminal.session.domain.valueobjects.TerminationReason
import jakarta.inject.Singleton

/**
 * 终止会话用例
 */
@Singleton
class TerminateSessionUseCase(
    private val terminalSessionService: TerminalSessionService
) {
    
    suspend fun execute(sessionId: SessionId, reason: TerminationReason) {
        terminalSessionService.terminateSession(sessionId, reason)
    }
}
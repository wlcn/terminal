package org.now.terminal.session.application.usecases

import org.now.terminal.session.domain.services.TerminalSessionService
import org.now.terminal.shared.valueobjects.SessionId
import org.now.terminal.session.domain.valueobjects.TerminationReason
/**
 * 终止会话用例
 */
class TerminateSessionUseCase(
    private val terminalSessionService: TerminalSessionService
) {
    
    suspend fun execute(sessionId: SessionId, reason: TerminationReason) {
        terminalSessionService.terminateSession(sessionId, reason)
    }
}
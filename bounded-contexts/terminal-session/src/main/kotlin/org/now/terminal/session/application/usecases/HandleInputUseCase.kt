package org.now.terminal.session.application.usecases

import org.now.terminal.session.domain.services.TerminalSessionService
import org.now.terminal.shared.valueobjects.SessionId
/**
 * 处理输入用例
 */
class HandleInputUseCase(
    private val terminalSessionService: TerminalSessionService
) {
    
    suspend fun execute(sessionId: SessionId, input: String) {
        terminalSessionService.handleInput(sessionId, input)
    }
}
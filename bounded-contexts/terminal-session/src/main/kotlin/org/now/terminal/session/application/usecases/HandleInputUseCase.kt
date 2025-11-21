package org.now.terminal.session.application.usecases

import org.now.terminal.session.domain.services.TerminalSessionService
import org.now.terminal.shared.valueobjects.SessionId
import jakarta.inject.Singleton

/**
 * 处理终端输入用例
 */
@Singleton
class HandleInputUseCase(
    private val terminalSessionService: TerminalSessionService
) {
    
    fun execute(sessionId: SessionId, input: String) {
        terminalSessionService.handleInput(sessionId, input)
    }
}
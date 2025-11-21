package org.now.terminal.session.application.usecases

import org.now.terminal.session.domain.services.TerminalSessionService
import org.now.terminal.shared.valueobjects.SessionId
import org.now.terminal.session.domain.valueobjects.TerminalSize
/**
 * 调整终端尺寸用例
 */
class ResizeTerminalUseCase(
    private val terminalSessionService: TerminalSessionService
) {
    
    suspend fun execute(sessionId: SessionId, size: TerminalSize) {
        terminalSessionService.resizeTerminal(sessionId, size)
    }
}
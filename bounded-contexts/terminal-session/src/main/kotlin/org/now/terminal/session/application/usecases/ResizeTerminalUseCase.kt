package org.now.terminal.session.application.usecases

import org.now.terminal.session.domain.services.SessionLifecycleService
import org.now.terminal.shared.valueobjects.SessionId
import org.now.terminal.session.domain.valueobjects.TerminalSize
import jakarta.inject.Singleton

/**
 * 调整终端尺寸用例
 */
@Singleton
class ResizeTerminalUseCase(
    private val sessionLifecycleService: SessionLifecycleService
) {
    
    fun execute(sessionId: SessionId, size: TerminalSize) {
        sessionLifecycleService.resizeTerminal(sessionId, size)
    }
}
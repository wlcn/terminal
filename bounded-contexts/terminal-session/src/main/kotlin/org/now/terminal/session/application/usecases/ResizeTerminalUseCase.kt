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
    
    /**
     * 使用SessionId对象调整终端尺寸
     */
    suspend fun execute(sessionId: SessionId, size: TerminalSize) {
        terminalSessionService.resizeTerminal(sessionId, size)
    }
    
    /**
     * 使用字符串sessionId调整终端尺寸
     * 如果sessionId格式无效，抛出IllegalArgumentException
     */
    suspend fun execute(sessionId: String, size: TerminalSize) {
        val sessionIdObj = SessionId.create(sessionId)
        terminalSessionService.resizeTerminal(sessionIdObj, size)
    }
}
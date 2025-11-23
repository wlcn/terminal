package org.now.terminal.session.application.usecases

import org.now.terminal.session.domain.services.TerminalSessionService
import org.now.terminal.shared.valueobjects.SessionId

/**
 * 处理输入用例
 */
class HandleInputUseCase(
    private val terminalSessionService: TerminalSessionService
) {
    
    /**
     * 使用SessionId对象处理输入
     */
    suspend fun execute(sessionId: SessionId, input: String) {
        terminalSessionService.handleInput(sessionId, input)
    }
    
    /**
     * 使用字符串sessionId处理输入
     * 如果sessionId格式无效，抛出IllegalArgumentException
     */
    suspend fun execute(sessionId: String, input: String) {
        val sessionIdObj = SessionId.create(sessionId)
        terminalSessionService.handleInput(sessionIdObj, input)
    }
}
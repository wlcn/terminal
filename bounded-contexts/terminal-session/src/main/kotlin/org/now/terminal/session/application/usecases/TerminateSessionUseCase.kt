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
    
    /**
     * 使用SessionId对象终止会话
     */
    suspend fun execute(sessionId: SessionId, reason: TerminationReason) {
        terminalSessionService.terminateSession(sessionId, reason)
    }
    
    /**
     * 使用字符串sessionId终止会话
     * 如果sessionId格式无效，抛出IllegalArgumentException
     */
    suspend fun execute(sessionId: String, reason: TerminationReason) {
        val sessionIdObj = SessionId.create(sessionId)
        terminalSessionService.terminateSession(sessionIdObj, reason)
    }
}
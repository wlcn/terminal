package org.now.terminal.session.application.usecases

import org.now.terminal.session.domain.services.TerminalSessionService
import org.now.terminal.shared.valueobjects.SessionId

/**
 * 检查会话是否活跃用例
 */
class CheckSessionActiveUseCase(
    private val terminalSessionService: TerminalSessionService
) {
    
    /**
     * 使用SessionId对象检查会话是否活跃
     */
    suspend fun execute(sessionId: SessionId): Boolean {
        return terminalSessionService.isSessionActive(sessionId)
    }
    
    /**
     * 使用字符串sessionId检查会话是否活跃
     * 如果sessionId格式无效，返回false
     */
    suspend fun execute(sessionId: String): Boolean {
        return try {
            val sessionIdObj = SessionId.create(sessionId)
            terminalSessionService.isSessionActive(sessionIdObj)
        } catch (e: IllegalArgumentException) {
            false // sessionId格式无效，会话不存在
        }
    }
}
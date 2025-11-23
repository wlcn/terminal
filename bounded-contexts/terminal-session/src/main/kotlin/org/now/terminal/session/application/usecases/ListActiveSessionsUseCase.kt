package org.now.terminal.session.application.usecases

import org.now.terminal.session.domain.services.TerminalSessionService
import org.now.terminal.session.domain.entities.TerminalSession
import org.now.terminal.shared.valueobjects.UserId
/**
 * 列出活跃会话用例
 */
class ListActiveSessionsUseCase(
    private val terminalSessionService: TerminalSessionService
) {
    
    suspend fun execute(userId: UserId): List<TerminalSession> {
        return terminalSessionService.listActiveSessions(userId)
    }
    
    /**
     * 列出所有活跃会话（无用户过滤）
     */
    suspend fun execute(): List<TerminalSession> {
        // 使用默认用户ID生成一个用户ID来调用服务
        // 由于服务层需要UserId参数，我们生成一个默认ID
        val defaultUserId = UserId.generate()
        return terminalSessionService.listActiveSessions(defaultUserId)
    }
}
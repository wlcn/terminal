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
}
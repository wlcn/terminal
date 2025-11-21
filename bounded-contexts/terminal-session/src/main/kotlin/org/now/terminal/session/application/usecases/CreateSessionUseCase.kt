package org.now.terminal.session.application.usecases

import org.now.terminal.session.domain.services.TerminalSessionService
import org.now.terminal.shared.valueobjects.SessionId
import org.now.terminal.shared.valueobjects.UserId
import org.now.terminal.session.domain.valueobjects.PtyConfiguration
import jakarta.inject.Singleton

/**
 * 创建会话用例
 */
@Singleton
class CreateSessionUseCase(
    private val terminalSessionService: TerminalSessionService
) {
    
    suspend fun execute(
        userId: UserId,
        ptyConfig: PtyConfiguration
    ): SessionId {
        return terminalSessionService.createSession(userId, ptyConfig)
    }
}
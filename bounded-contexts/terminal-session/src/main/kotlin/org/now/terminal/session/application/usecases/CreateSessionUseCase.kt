package org.now.terminal.session.application.usecases

import org.now.terminal.infrastructure.configuration.ConfigurationManager
import org.now.terminal.session.domain.services.TerminalSessionService
import org.now.terminal.shared.valueobjects.SessionId
import org.now.terminal.shared.valueobjects.UserId
import org.now.terminal.session.domain.valueobjects.PtyConfiguration
import org.now.terminal.session.domain.valueobjects.TerminalCommand
/**
 * 创建会话用例
 */
class CreateSessionUseCase(
    private val terminalSessionService: TerminalSessionService
) {
    
    suspend fun execute(
        userId: UserId,
        ptyConfig: PtyConfiguration
    ): SessionId {
        return terminalSessionService.createSession(userId, ptyConfig)
    }
    
    /**
     * 使用默认配置创建会话
     */
    suspend fun execute(): SessionId {
        val defaultUserId = UserId.generate()
        val defaultCommand = ConfigurationManager
            .getTerminalConfig().pty.defaultCommand
        val terminalCommand = TerminalCommand(defaultCommand)
        val ptyConfig = PtyConfiguration.createDefault(terminalCommand)
        
        return terminalSessionService.createSession(defaultUserId, ptyConfig)
    }
}
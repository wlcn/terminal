package org.now.terminal.boundedcontext.terminalsession.application.usecases

import org.now.terminal.boundedcontext.terminalsession.application.dtos.ExecuteTerminalCommand
import org.now.terminal.boundedcontext.terminalsession.domain.repositories.TerminalSessionRepository
import org.now.terminal.boundedcontext.terminalsession.domain.services.command.Command
import org.now.terminal.boundedcontext.terminalsession.domain.services.command.CommandResult
import org.now.terminal.boundedcontext.terminalsession.domain.services.executor.CommandExecutor
import org.now.terminal.boundedcontext.terminalsession.domain.valueobjects.TerminalSessionId

/**
 * Execute terminal command use case
 * 
 * This is an application service that coordinates the execution of commands
 * within terminal sessions. It belongs in the application layer because it
 * coordinates multiple domain objects to fulfill a use case.
 */
class ExecuteTerminalCommandUseCase(
    private val sessionRepository: TerminalSessionRepository,
    private val commandExecutor: CommandExecutor
) {
    
    /**
     * Execute a command in a terminal session
     */
    suspend fun execute(command: ExecuteTerminalCommand): CommandResult {
        // 1. Validate session exists and is active
        val session = sessionRepository.findById(command.sessionId)
            ?: throw IllegalArgumentException("Terminal session not found: ${command.sessionId}")
        
        if (!session.isActive) {
            throw IllegalStateException("Terminal session is not active: ${command.sessionId}")
        }
        
        // 2. Create command object (only dynamic parameters)
        val commandObj = Command(
            value = command.command,
            timeoutMs = command.timeoutMs
        )
        
        // 3. Execute command
        val result = commandExecutor.executeCommand(commandObj)
        
        // 4. Update session state
        val updatedSession = session.recordCommand(command.command)
        
        // 5. Save session changes
        sessionRepository.save(updatedSession)
        
        return result
    }
    
    /**
     * Execute multiple commands in sequence
     */
    suspend fun executeCommands(commands: List<ExecuteTerminalCommand>): List<CommandResult> {
        return commands.map { execute(it) }
    }
}
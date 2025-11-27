package org.now.terminal.boundedcontext.terminalsession.application.services

import org.now.terminal.boundedcontext.terminalsession.application.usecases.dtos.ExecuteTerminalCommand
import org.now.terminal.boundedcontext.terminalsession.application.usecases.ExecuteTerminalCommandUseCase
import org.now.terminal.boundedcontext.terminalsession.domain.services.command.CommandResult

/**
 * Terminal command service (application service)
 * 
 * This service provides a higher-level API for terminal command execution.
 * It coordinates use cases and provides additional functionality like
 * command history, session management, etc.
 */
class TerminalCommandService(
    private val executeTerminalCommandUseCase: ExecuteTerminalCommandUseCase
) {
    
    /**
     * Execute a single command in a terminal session
     */
    suspend fun executeCommand(command: ExecuteTerminalCommand): CommandResult {
        return executeTerminalCommandUseCase.execute(command)
    }
    
    /**
     * Execute multiple commands in sequence
     */
    suspend fun executeCommands(commands: List<ExecuteTerminalCommand>): List<CommandResult> {
        return executeTerminalCommandUseCase.executeCommands(commands)
    }
    
    /**
     * Execute a command and return only the output if successful
     */
    suspend fun executeCommandAndGetOutput(command: ExecuteTerminalCommand): String {
        val result = executeTerminalCommandUseCase.execute(command)
        
        if (!result.isSuccess) {
            throw RuntimeException("Command execution failed with exit code ${result.exitCode}: ${result.errorOutput}")
        }
        
        return result.output
    }
    
    /**
     * Execute a command and check if it was successful
     */
    suspend fun executeCommandAndCheckSuccess(command: ExecuteTerminalCommand): Boolean {
        val result = executeTerminalCommandUseCase.execute(command)
        return result.isSuccess
    }
}
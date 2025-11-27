package org.now.terminal.boundedcontext.terminalsession.domain.services.executor

import org.now.terminal.boundedcontext.terminalsession.domain.services.command.Command
import org.now.terminal.boundedcontext.terminalsession.domain.services.command.CommandResult

/**
 * Command executor interface (domain service)
 * 
 * This is a domain interface that defines the business concept of command execution.
 * Implementations will be provided by the infrastructure layer.
 */
interface CommandExecutor {
    
    /**
     * Execute a command with the given configuration
     */
    fun executeCommand(command: Command): CommandResult
    
    /**
     * Check if the executor is available
     */
    fun isAvailable(): Boolean
    
    /**
     * Get the executor type/name
     */
    fun getExecutorType(): String
}
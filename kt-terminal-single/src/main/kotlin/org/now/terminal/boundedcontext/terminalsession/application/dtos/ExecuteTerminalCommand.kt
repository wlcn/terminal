package org.now.terminal.boundedcontext.terminalsession.application.dtos

import org.now.terminal.boundedcontext.terminalsession.domain.valueobjects.TerminalSessionId

/**
 * Execute terminal command DTO
 */
data class ExecuteTerminalCommand(
    val sessionId: TerminalSessionId,
    val command: String,
    val timeoutMs: Long? = null
) {
    init {
        require(command.isNotBlank()) { "Command cannot be empty" }
    }
}
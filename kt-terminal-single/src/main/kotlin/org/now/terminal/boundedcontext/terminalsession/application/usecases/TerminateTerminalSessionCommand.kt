package org.now.terminal.boundedcontext.terminalsession.application.usecases.dtos

import org.now.terminal.boundedcontext.terminalsession.domain.valueobjects.TerminalSessionId

/**
 * Command to terminate a terminal session
 */
data class TerminateTerminalSessionCommand(
    val sessionId: TerminalSessionId
)
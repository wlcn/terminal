package org.now.terminal.boundedcontext.terminalsession.application.commands

import kotlinx.serialization.Serializable
import org.now.terminal.boundedcontext.terminalsession.domain.valueobjects.TerminalSessionId

/**
 * Command to terminate a terminal session
 */
@Serializable
data class TerminateTerminalSessionCommand(
    val sessionId: TerminalSessionId
)
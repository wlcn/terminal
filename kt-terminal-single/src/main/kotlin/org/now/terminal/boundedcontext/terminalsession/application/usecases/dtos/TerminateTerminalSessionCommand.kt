package org.now.terminal.boundedcontext.terminalsession.application.usecases.dtos

import kotlinx.serialization.Serializable
import org.now.terminal.boundedcontext.terminalsession.domain.valueobjects.TerminalSessionId

/**
 * DTO to terminate a terminal session
 */
@Serializable
data class TerminateTerminalSessionCommand(
    val sessionId: TerminalSessionId
)


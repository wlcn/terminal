package org.now.terminal.boundedcontext.terminalsession.application.usecases.dtos

import org.now.terminal.boundedcontext.terminalsession.domain.valueobjects.TerminalSessionId

/**
 * Query to get a terminal session by ID
 */
data class GetTerminalSessionByIdQuery(
    val sessionId: TerminalSessionId
)
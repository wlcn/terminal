package org.now.terminal.boundedcontext.terminalsession.application.usecases.dtos

import org.now.terminal.shared.valueobjects.UserId

/**
 * Query to get all terminal sessions for a user
 */
data class GetUserTerminalSessionsQuery(
    val userId: UserId,
    val includeInactive: Boolean = false
)
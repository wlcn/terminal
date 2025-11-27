package org.now.terminal.boundedcontext.terminalsession.application.usecases.dtos

import org.now.terminal.shared.valueobjects.UserId

/**
 * Command to create a new terminal session
 */
data class CreateTerminalSessionCommand(
    val userId: UserId,
    val title: String? = null,
    val workingDirectory: String? = null
)
package org.now.terminal.infrastructure.boundedcontext.user.web.dto.requests

import kotlinx.serialization.Serializable
import org.now.terminal.boundedcontext.user.application.command.UpdateUserCommand
import org.now.terminal.interfaces.user.dto.requests.UpdateUserRequest

/**
 * Update user request DTO implementation
 */
@Serializable
data class UpdateUserRequestImpl(
    override val displayName: String? = null,
    override val email: String? = null,
    override val role: String? = null,
    override val status: String? = null
) : UpdateUserRequest {

    /**
     * Convert to domain command
     */
    fun toCommand(userId: String): UpdateUserCommand {
        return UpdateUserCommand(
            userId = userId,
            displayName = displayName,
            email = email,
            role = role,
            status = status
        )
    }
}
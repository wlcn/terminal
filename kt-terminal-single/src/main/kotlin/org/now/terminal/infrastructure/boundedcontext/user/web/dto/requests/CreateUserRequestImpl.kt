package org.now.terminal.infrastructure.boundedcontext.user.web.dto.requests

import kotlinx.serialization.Serializable
import org.now.terminal.boundedcontext.user.application.command.CreateUserCommand
import org.now.terminal.interfaces.user.dto.requests.CreateUserRequest

/**
 * Create user request DTO implementation
 */
@Serializable
data class CreateUserRequestImpl(
    override val username: String,
    override val email: String,
    override val password: String,
    override val displayName: String? = null,
    override val role: String? = null
) : CreateUserRequest {

    /**
     * Convert to domain command
     */
    fun toCommand(): CreateUserCommand {
        return CreateUserCommand(
            username = username,
            email = email,
            password = password,
            displayName = displayName,
            role = role
        )
    }
}
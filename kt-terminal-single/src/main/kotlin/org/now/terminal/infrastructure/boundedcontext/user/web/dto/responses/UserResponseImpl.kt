package org.now.terminal.infrastructure.boundedcontext.user.web.dto.responses

import kotlinx.serialization.Serializable
import org.now.terminal.boundedcontext.user.domain.User
import org.now.terminal.interfaces.user.dto.responses.UserResponse

/**
 * User response DTO implementation
 */
@Serializable
data class UserResponseImpl(
    override val id: String,
    override val username: String,
    override val email: String,
    override val displayName: String?,
    override val role: String,
    override val status: String,
    override val createdAt: Long,
    override val updatedAt: Long
) : UserResponse {

    companion object {
        /**
         * Convert from domain model
         */
        fun fromDomain(user: User): UserResponseImpl {
            return UserResponseImpl(
                id = user.id.value,
                username = user.username,
                email = user.email,
                displayName = user.displayName,
                role = user.role,
                status = user.status.toString(),
                createdAt = user.createdAt,
                updatedAt = user.updatedAt
            )
        }
    }
}
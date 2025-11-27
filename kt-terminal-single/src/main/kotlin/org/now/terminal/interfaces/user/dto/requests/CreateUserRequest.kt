package org.now.terminal.interfaces.user.dto.requests

/**
 * Create user request DTO
 */
interface CreateUserRequest {
    val username: String
    val email: String
    val password: String
    val displayName: String?
    val role: String?
}
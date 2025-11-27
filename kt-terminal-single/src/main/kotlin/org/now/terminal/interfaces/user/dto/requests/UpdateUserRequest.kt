package org.now.terminal.interfaces.user.dto.requests

/**
 * Update user request DTO
 */
interface UpdateUserRequest {
    val displayName: String?
    val email: String?
    val role: String?
    val status: String?
}
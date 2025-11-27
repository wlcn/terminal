package org.now.terminal.interfaces.user.dto.responses

/**
 * User response DTO
 */
interface UserResponse {
    val id: String
    val username: String
    val email: String
    val displayName: String?
    val role: String
    val status: String
    val createdAt: Long
    val updatedAt: Long
}
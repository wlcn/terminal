package org.now.terminal.boundedcontext.user.application.usecase

import org.now.terminal.boundedcontext.user.domain.User
import org.now.terminal.boundedcontext.user.domain.UserRepository
import org.now.terminal.boundedcontext.user.domain.valueobjects.*
import org.now.terminal.shared.valueobjects.UserId

/**
 * User Management Use Case - Handles user creation, update, and deletion operations
 */
interface UserManagementUseCase {
    
    /**
     * Create new user
     */
    suspend fun createUser(command: CreateUserCommand): User
    
    /**
     * Update existing user
     */
    suspend fun updateUser(command: UpdateUserCommand): User
    
    /**
     * Delete user
     */
    suspend fun deleteUser(command: DeleteUserCommand)
    
    /**
     * Change user password
     */
    suspend fun changePassword(command: ChangePasswordCommand)
    
    /**
     * Update user role
     */
    suspend fun updateUserRole(command: UpdateUserRoleCommand): User
}

/**
 * Commands for User Management Use Case
 */
data class CreateUserCommand(
    val username: String,
    val email: Email,
    val passwordHash: String,
    val role: UserRole,
    val phoneNumber: PhoneNumber? = null,
    val sessionLimit: SessionLimit
)

data class UpdateUserCommand(
    val userId: UserId,
    val username: String? = null,
    val email: Email? = null,
    val phoneNumber: PhoneNumber? = null
)

data class DeleteUserCommand(
    val userId: UserId
)

data class ChangePasswordCommand(
    val userId: UserId,
    val currentPasswordHash: String,
    val newPasswordHash: String
)

data class UpdateUserRoleCommand(
    val userId: UserId,
    val newRole: UserRole
)

/**
 * Implementation of User Management Use Case
 */
class UserManagementUseCaseImpl(
    private val userRepository: UserRepository
) : UserManagementUseCase {
    
    override suspend fun createUser(command: CreateUserCommand): User {
        // Check if user already exists
        if (userRepository.existsByUsername(command.username)) {
            throw IllegalArgumentException("Username already exists")
        }
        if (userRepository.existsByEmail(command.email)) {
            throw IllegalArgumentException("Email already exists")
        }
        
        // Create new user
        val user = User(
            id = UserId.generate(),
            username = command.username,
            email = command.email,
            phoneNumber = command.phoneNumber,
            role = command.role,
            sessionLimit = command.sessionLimit,
            passwordHash = command.passwordHash,
            isActive = true
        )
        
        // Save user
        userRepository.save(user)
        
        return user
    }
    
    override suspend fun updateUser(command: UpdateUserCommand): User {
        // Find existing user
        val existingUser = userRepository.findById(command.userId)
            ?: throw IllegalArgumentException("User not found")
        
        // Update user properties
        val updatedUser = existingUser.copy(
            username = command.username ?: existingUser.username,
            email = command.email ?: existingUser.email,
            phoneNumber = command.phoneNumber ?: existingUser.phoneNumber
        )
        
        // Save updated user
        userRepository.save(updatedUser)
        
        return updatedUser
    }
    
    override suspend fun deleteUser(command: DeleteUserCommand) {
        // Check if user exists
        if (!userRepository.existsById(command.userId)) {
            throw IllegalArgumentException("User not found")
        }
        
        // Delete user
        userRepository.delete(command.userId)
    }
    
    override suspend fun changePassword(command: ChangePasswordCommand) {
        val user = userRepository.findById(command.userId)
            ?: throw IllegalArgumentException("User not found with ID: ${command.userId}")
        
        val updatedUser = user.resetPassword(command.newPasswordHash)
        userRepository.save(updatedUser)
    }
    
    override suspend fun updateUserRole(command: UpdateUserRoleCommand): User {
        val user = userRepository.findById(command.userId)
            ?: throw IllegalArgumentException("User not found with ID: ${command.userId}")
        
        val updatedUser = user.updateRole(command.newRole)
        userRepository.save(updatedUser)
        return updatedUser
    }
}
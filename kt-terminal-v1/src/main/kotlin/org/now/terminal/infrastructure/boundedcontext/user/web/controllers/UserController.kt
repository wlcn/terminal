package org.now.terminal.infrastructure.boundedcontext.user.web.controllers

import org.now.terminal.boundedcontext.user.application.usecase.CreateUserCommand
import org.now.terminal.boundedcontext.user.application.usecase.DeleteUserCommand
import org.now.terminal.boundedcontext.user.application.usecase.GetUserByIdQuery
import org.now.terminal.boundedcontext.user.application.usecase.SearchUsersQuery
import org.now.terminal.boundedcontext.user.application.usecase.UpdateUserCommand
import org.now.terminal.boundedcontext.user.application.usecase.UserManagementUseCase
import org.now.terminal.boundedcontext.user.application.usecase.UserQueryUseCase
import org.now.terminal.boundedcontext.user.domain.User
import org.now.terminal.boundedcontext.user.domain.valueobjects.Email
import org.now.terminal.boundedcontext.user.domain.valueobjects.PhoneNumber
import org.now.terminal.boundedcontext.user.domain.valueobjects.SessionLimit
import org.now.terminal.shared.valueobjects.UserId
import org.now.terminal.boundedcontext.user.domain.valueobjects.UserRole

/**
 * User Controller
 * Infrastructure layer implementation that handles HTTP requests
 */
class UserController(
    private val userManagementUseCase: UserManagementUseCase,
    private val userQueryUseCase: UserQueryUseCase
) {
    
    suspend fun createUser(
        username: String,
        email: String,
        passwordHash: String,
        role: String,
        phoneNumber: String?,
        sessionLimit: Int
    ): User {
        val command = CreateUserCommand(
            username = username,
            email = Email.create(email),
            passwordHash = passwordHash,
            role = UserRole.valueOf(role.uppercase()),
            phoneNumber = phoneNumber?.let { PhoneNumber.create(it) },
            sessionLimit = SessionLimit(
                maxConcurrentSessions = sessionLimit,
                maxSessionDuration = 2 * 60 * 60 * 1000 // Default 2 hours
            )
        )
        return userManagementUseCase.createUser(command)
    }
    
    suspend fun updateUser(
        userId: String,
        username: String?,
        email: String?,
        phoneNumber: String?
    ): User {
        val command = UpdateUserCommand(
            userId = UserId(userId),
            username = username,
            email = email?.let { Email.create(it) },
            phoneNumber = phoneNumber?.let { PhoneNumber.create(it) }
        )
        return userManagementUseCase.updateUser(command)
    }
    
    suspend fun deleteUser(userId: String) {
        val command = DeleteUserCommand(userId = UserId(userId))
        userManagementUseCase.deleteUser(command)
    }
    
    suspend fun getUserById(userId: String): User? {
        val query = GetUserByIdQuery(userId = UserId(userId))
        return userQueryUseCase.getUserById(query)
    }
    
    suspend fun getUsers(): List<User> {
        val result = userQueryUseCase.searchUsers(SearchUsersQuery())
        return result.users
    }
    
    suspend fun searchUsers(keyword: String?): List<User> {
        val query = SearchUsersQuery(keyword = keyword)
        val result = userQueryUseCase.searchUsers(query)
        return result.users
    }
    
    // Route configuration has been moved to UserModule, only business logic methods remain here
}
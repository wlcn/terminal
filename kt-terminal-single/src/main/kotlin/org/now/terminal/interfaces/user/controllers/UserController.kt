package org.now.terminal.interfaces.user.controllers

import org.now.terminal.boundedcontext.user.application.usecase.UserManagementUseCase
import org.now.terminal.boundedcontext.user.application.usecase.UserQueryUseCase
import org.now.terminal.interfaces.ApiResponse
import org.now.terminal.interfaces.user.dto.requests.CreateUserRequest
import org.now.terminal.interfaces.user.dto.requests.UpdateUserRequest
import org.now.terminal.interfaces.user.dto.responses.UserResponse

/**
 * User management controller interface
 * Defines user-related API interfaces, implementations are placed in infrastructure layer
 */
interface UserController {
    
    /**
     * Create a new user
     */
    fun createUser(request: CreateUserRequest): ApiResponse<UserResponse>
    
    /**
     * Update user information
     */
    fun updateUser(userId: String, request: UpdateUserRequest): ApiResponse<UserResponse>
    
    /**
     * Delete a user
     */
    fun deleteUser(userId: String): ApiResponse<Unit>
    
    /**
     * Get user details by ID
     */
    fun getUserById(userId: String): ApiResponse<UserResponse>
    
    /**
     * Get all users
     */
    fun getUsers(): ApiResponse<List<UserResponse>>
    
    /**
     * Search users by keyword
     */
    fun searchUsers(keyword: String): ApiResponse<List<UserResponse>>
}
package org.now.terminal.infrastructure.boundedcontext.user.web.controllers

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.now.terminal.boundedcontext.user.application.usecase.UserManagementUseCase
import org.now.terminal.boundedcontext.user.application.usecase.UserQueryUseCase
import org.now.terminal.infrastructure.boundedcontext.user.web.dto.requests.CreateUserRequestImpl
import org.now.terminal.infrastructure.boundedcontext.user.web.dto.requests.UpdateUserRequestImpl
import org.now.terminal.infrastructure.web.responses.ApiResponseImpl
import org.now.terminal.interfaces.user.controllers.UserController
import org.now.terminal.interfaces.user.dto.responses.UserResponse

/**
 * User management controller implementation
 * Ktor-based implementation of UserController interface
 */
class UserControllerImpl(
    private val userManagementUseCase: UserManagementUseCase,
    private val userQueryUseCase: UserQueryUseCase
) : UserController {

    override fun createUser(request: CreateUserRequestImpl): ApiResponseImpl<UserResponse> {
        return try {
            val command = request.toCommand()
            val user = userManagementUseCase.createUser(command)
            ApiResponseImpl.success(
                data = user,
                message = "User created successfully"
            )
        } catch (e: Exception) {
            ApiResponseImpl.error(
                message = "Failed to create user: ${e.message}",
                code = 400
            )
        }
    }

    override fun updateUser(userId: String, request: UpdateUserRequestImpl): ApiResponseImpl<UserResponse> {
        return try {
            val command = request.toCommand(userId)
            val user = userManagementUseCase.updateUser(command)
            ApiResponseImpl.success(
                data = user,
                message = "User updated successfully"
            )
        } catch (e: Exception) {
            ApiResponseImpl.error(
                message = "Failed to update user: ${e.message}",
                code = 400
            )
        }
    }

    override fun deleteUser(userId: String): ApiResponseImpl<Unit> {
        return try {
            userManagementUseCase.deleteUser(
                org.now.terminal.boundedcontext.user.application.usecase.DeleteUserCommand(
                    userId = org.now.terminal.boundedcontext.user.domain.valueobjects.UserId.fromString(userId)
                )
            )
            ApiResponseImpl.success(
                data = Unit,
                message = "User deleted successfully"
            )
        } catch (e: Exception) {
            ApiResponseImpl.error(
                message = "Failed to delete user: ${e.message}",
                code = 400
            )
        }
    }

    override fun getUserById(userId: String): ApiResponseImpl<UserResponse> {
        return try {
            val user = userQueryUseCase.getUserById(org.now.terminal.boundedcontext.user.application.usecase.GetUserByIdQuery(
                userId = org.now.terminal.boundedcontext.user.domain.valueobjects.UserId.fromString(userId)
            ))
            if (user != null) {
                ApiResponseImpl.success(
                    data = user,
                    message = "User retrieved successfully"
                )
            } else {
                ApiResponseImpl.error(
                    message = "User not found",
                    code = 404
                )
            }
        } catch (e: Exception) {
            ApiResponseImpl.error(
                message = "Failed to get user: ${e.message}",
                code = 500
            )
        }
    }

    override fun getUsers(): ApiResponseImpl<List<UserResponse>> {
        return try {
            val result = userQueryUseCase.searchUsers(
                org.now.terminal.boundedcontext.user.application.usecase.SearchUsersQuery(
                    keyword = null,
                    role = null,
                    page = 0,
                    size = 100
                )
            )
            ApiResponseImpl.success(
                data = result.users,
                message = "Users retrieved successfully"
            )
        } catch (e: Exception) {
            ApiResponseImpl.error(
                message = "Failed to get users: ${e.message}",
                code = 500
            )
        }
    }

    override fun searchUsers(keyword: String): ApiResponseImpl<List<UserResponse>> {
        return try {
            val result = userQueryUseCase.searchUsers(
                org.now.terminal.boundedcontext.user.application.usecase.SearchUsersQuery(
                    keyword = keyword,
                    role = null,
                    page = 0,
                    size = 20
                )
            )
            ApiResponseImpl.success(
                data = result.users,
                message = "Users search completed successfully"
            )
        } catch (e: Exception) {
            ApiResponseImpl.error(
                message = "Failed to search users: ${e.message}",
                code = 400
            )
        }
    }
}

/**
 * Ktor routing configuration for user endpoints
 */
fun Application.configureUserRoutes(userController: UserControllerImpl) {
    routing {
        route("/api/users") {
            
            // Create user
            post {
                val request = call.receive<CreateUserRequestImpl>()
                val response = userController.createUser(request)
                call.respond(response.code ?: 200, response)
            }
            
            // Get all users
            get {
                val response = userController.getUsers()
                call.respond(response.code ?: 200, response)
            }
            
            // Search users
            get("/search") {
                val keyword = call.request.queryParameters["keyword"] ?: ""
                val response = userController.searchUsers(keyword)
                call.respond(response.code ?: 200, response)
            }
            
            route("/{userId}") {
                
                // Get user by ID
                get {
                    val userId = call.parameters["userId"] ?: throw IllegalArgumentException("User ID is required")
                    val response = userController.getUserById(userId)
                    call.respond(response.code ?: 200, response)
                }
                
                // Update user
                put {
                    val userId = call.parameters["userId"] ?: throw IllegalArgumentException("User ID is required")
                    val request = call.receive<UpdateUserRequestImpl>()
                    val response = userController.updateUser(userId, request)
                    call.respond(response.code ?: 200, response)
                }
                
                // Delete user
                delete {
                    val userId = call.parameters["userId"] ?: throw IllegalArgumentException("User ID is required")
                    val response = userController.deleteUser(userId)
                    call.respond(response.code ?: 200, response)
                }
            }
        }
    }
}
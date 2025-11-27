package org.now.terminal.infrastructure.boundedcontext.user.config

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.ktor.ext.get
import org.now.terminal.boundedcontext.user.application.usecase.UserManagementUseCase
import org.now.terminal.boundedcontext.user.application.usecase.UserManagementUseCaseImpl
import org.now.terminal.boundedcontext.user.application.usecase.UserQueryUseCase
import org.now.terminal.boundedcontext.user.application.usecase.UserQueryUseCaseImpl
import org.now.terminal.boundedcontext.user.domain.UserRepository
import org.now.terminal.infrastructure.boundedcontext.user.web.controllers.UserController

/**
 * User Module Dependency Injection Configuration
 * Configures user-related dependencies in Koin DI container
 */
val userModule: Module = module {

    /**
     * User Controller
     */
    single<UserController> {
        UserController(
            userManagementUseCase = get<UserManagementUseCase>(),
            userQueryUseCase = get<UserQueryUseCase>()
        )
    }

    /**
     * User Repository (to be implemented)
     */
    single<UserRepository> {
        TODO("Implement actual UserRepository implementation")
    }

    /**
     * User Management Use Case - using implemented UseCaseImpl class
     */
    single<UserManagementUseCase> { UserManagementUseCaseImpl(get()) }

    /**
     * User Query Use Case - using implemented UseCaseImpl class
     */
    single<UserQueryUseCase> { UserQueryUseCaseImpl(get()) }
}

/**
 * User Routing Configuration
 * Configures Ktor user endpoint routes
 */
fun Application.configureUserModule() {
    // Get user controller from DI container
    val userController = get<UserController>()

    // Configure user routes
    routing {
        configureUserRoutes(userController)
    }
}

/**
 * Configure User Routes
 */
fun Routing.configureUserRoutes(userController: UserController) {
    route("/api/users") {
        // Create user
        post {
            val params = call.request.queryParameters
            val result = userController.createUser(
                username = params["username"] ?: "",
                email = params["email"] ?: "",
                passwordHash = params["passwordHash"] ?: "",
                role = params["role"] ?: "USER",
                phoneNumber = params["phoneNumber"],
                sessionLimit = params["sessionLimit"]?.toIntOrNull() ?: 10
            )
            call.respond(HttpStatusCode.Created, result)
        }


        // Search users
        get("/search") {
            val keyword = call.request.queryParameters["keyword"]
            val users = userController.searchUsers(keyword)
            call.respond(HttpStatusCode.OK, users)
        }

        route("/{userId}") {
            // Get user by ID


            // Update user
            put {
                val userId = call.parameters["userId"] ?: ""
                val params = call.request.queryParameters
                val user = userController.updateUser(
                    userId = userId,
                    username = params["username"],
                    email = params["email"],
                    phoneNumber = params["phoneNumber"]
                )
                call.respond(HttpStatusCode.OK, user)
            }

            // Delete user
            delete {
                val userId = call.parameters["userId"] ?: ""
                userController.deleteUser(userId)
                call.respond(HttpStatusCode.OK, mapOf("message" to "User deleted successfully"))
            }
        }
    }
}
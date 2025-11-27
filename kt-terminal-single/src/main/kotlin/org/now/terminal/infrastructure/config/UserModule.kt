package org.now.terminal.infrastructure.config

import io.ktor.server.application.*
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.ktor.ext.get
import org.now.terminal.boundedcontext.user.application.usecase.*
import org.now.terminal.boundedcontext.user.domain.valueobjects.*
import org.now.terminal.boundedcontext.user.domain.UserRepository
import org.now.terminal.infrastructure.boundedcontext.user.web.controllers.UserControllerImpl
import org.now.terminal.infrastructure.boundedcontext.user.web.controllers.configureUserRoutes

/**
 * User module dependency injection configuration
 * Configures all user-related dependencies for Koin DI container
 */
val userModule: Module = module {
    
    /**
     * User controller
     */
    single<UserControllerImpl> {
        UserControllerImpl(
            userManagementUseCase = get(),
            userQueryUseCase = get()
        )
    }
    
    /**
     * User Repository (需要后续实现)
     */
    single<UserRepository> { 
        TODO("Implement actual UserRepository implementation")
    }
    
    /**
     * User management use case - 使用已实现的UseCaseImpl类
     */
    single<UserManagementUseCase> { UserManagementUseCaseImpl(get()) }
    
    /**
     * User query use case - 使用已实现的UseCaseImpl类
     */
    single<UserQueryUseCase> { UserQueryUseCaseImpl(get()) }
}

/**
 * User routing configuration
 * Configures Ktor routes for user endpoints
 */
fun Application.configureUserModule() {
    // Get user controller from DI container
    val userController = get<UserControllerImpl>()
    
    // Configure user routes
    configureUserRoutes(userController)
}
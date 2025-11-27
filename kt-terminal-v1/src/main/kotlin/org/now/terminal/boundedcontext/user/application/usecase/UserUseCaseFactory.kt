package org.now.terminal.boundedcontext.user.application.usecase

import org.now.terminal.boundedcontext.user.domain.UserRepository

/**
 * User Use Case Factory - Provides unified creation and management of user use cases
 */
class UserUseCaseFactory(
    private val userRepository: UserRepository
) {
    
    /**
     * Create user management use case
     */
    fun createUserManagementUseCase(): UserManagementUseCase {
        return UserManagementUseCaseImpl(userRepository)
    }
    
    /**
     * Create user query use case
     */
    fun createUserQueryUseCase(): UserQueryUseCase {
        return UserQueryUseCaseImpl(userRepository)
    }
    
    /**
     * Create collection of all user use cases
     */
    fun createAllUseCases(): UserUseCaseCollection {
        return UserUseCaseCollection(
            management = createUserManagementUseCase(),
            query = createUserQueryUseCase()
        )
    }
}

/**
 * Collection of user use cases
 */
data class UserUseCaseCollection(
    val management: UserManagementUseCase,
    val query: UserQueryUseCase
)

/**
 * Utility class for quick use case creation
 */
object UserUseCaseUtils {
    
    /**
     * Quick creation of user management use case
     */
    fun quickUserManagement(repository: UserRepository): UserManagementUseCase {
        return UserManagementUseCaseImpl(repository)
    }
    
    /**
     * Quick creation of user query use case
     */
    fun quickUserQuery(repository: UserRepository): UserQueryUseCase {
        return UserQueryUseCaseImpl(repository)
    }
}
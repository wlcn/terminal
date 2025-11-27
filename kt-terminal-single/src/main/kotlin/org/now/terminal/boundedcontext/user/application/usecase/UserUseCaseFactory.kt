package org.now.terminal.boundedcontext.user.application.usecase

import org.now.terminal.boundedcontext.user.application.UserMessageProvider
import org.now.terminal.boundedcontext.user.application.UserPreferenceService
import org.now.terminal.boundedcontext.user.application.UserPreferenceServiceImpl
import org.now.terminal.boundedcontext.user.domain.UserRepository
import org.now.terminal.shared.kernel.Language

/**
 * User Use Case Factory - Provides unified creation and management of user use cases
 */
class UserUseCaseFactory(
    private val userRepository: UserRepository,
    private val userPreferenceService: UserPreferenceService,
    private val messageProviders: Map<Language, UserMessageProvider>
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
     * Create user message use case
     */
    fun createUserMessageUseCase(): UserMessageUseCase {
        return UserMessageUseCaseImpl(userPreferenceService, messageProviders)
    }
    
    /**
     * Create collection of all user use cases
     */
    fun createAllUseCases(): UserUseCaseCollection {
        return UserUseCaseCollection(
            management = createUserManagementUseCase(),
            query = createUserQueryUseCase(),
            message = createUserMessageUseCase()
        )
    }
}

/**
 * Collection of user use cases
 */
data class UserUseCaseCollection(
    val management: UserManagementUseCase,
    val query: UserQueryUseCase,
    val message: UserMessageUseCase
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
    
    /**
     * Quick creation of user message use case
     */
    fun quickUserMessage(
        preferenceService: UserPreferenceService,
        messageProviders: Map<Language, UserMessageProvider> = createDefaultMessageProviders()
    ): UserMessageUseCase {
        return UserMessageUseCaseImpl(preferenceService, messageProviders)
    }
    
    /**
     * Quick creation of user message use case with default preference service
     */
    fun quickUserMessage(
        messageProviders: Map<Language, UserMessageProvider> = createDefaultMessageProviders()
    ): UserMessageUseCase {
        // Create a simple in-memory preference service for default usage
        val preferenceService = object : UserPreferenceService {
            private val preferences = mutableMapOf<String, Language>()
            
            override fun getUserLanguage(userId: String): Language {
                return preferences[userId] ?: Language.ENGLISH
            }
            
            override fun updateUserLanguage(userId: String, language: Language) {
                preferences[userId] = language
            }
        }
        
        return UserMessageUseCaseImpl(preferenceService, messageProviders)
    }
    
    /**
     * Create default message providers
     */
    private fun createDefaultMessageProviders(): Map<Language, UserMessageProvider> {
        return mapOf(
            Language.ENGLISH to UserMessageProvider.English,
            Language.CHINESE to UserMessageProvider.Chinese
        )
    }
}
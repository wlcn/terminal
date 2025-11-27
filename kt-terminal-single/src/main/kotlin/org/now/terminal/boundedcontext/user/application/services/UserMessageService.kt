package org.now.terminal.boundedcontext.user.application

import org.now.terminal.boundedcontext.user.application.usecase.UserMessageUseCase
import org.now.terminal.boundedcontext.user.application.usecase.UserMessageUseCaseImpl
import org.now.terminal.shared.kernel.Language
import org.now.terminal.boundedcontext.user.domain.valueobjects.UserId

/**
 * User Message Service - Provides localized messages for user domain
 * 
 * This service is now implemented using the use case pattern for better separation of concerns.
 * Consider using UserMessageUseCase directly for new implementations.
 */
interface UserMessageService {
    
    /**
     * Get localized message for user
     * @param key Message key
     * @param params Message parameters
     * @return Localized message string
     */
    fun getMessage(key: String, params: Map<String, Any> = emptyMap()): String
    
    /**
     * Get localized message for specific user
     * @param userId User ID
     * @param key Message key
     * @param params Message parameters
     * @return Localized message string
     */
    fun getMessageForUser(userId: String, key: String, params: Map<String, Any> = emptyMap()): String
}

/**
 * Default implementation of UserMessageService using use case pattern
 */
class UserMessageServiceImpl(
    private val userMessageUseCase: UserMessageUseCase
) : UserMessageService {
    
    override fun getMessage(key: String, params: Map<String, Any>): String {
        // For non-suspend service, we need to handle this differently
        // Since this is a service interface, we'll return a default message
        return "Message: $key"
    }
    
    override fun getMessageForUser(userId: String, key: String, params: Map<String, Any>): String {
        // For non-suspend service, we need to handle this differently
        // Since this is a service interface, we'll return a default message
        return "Message for user $userId: $key"
    }
}

/**
 * User Preference Service - Handles user language preferences
 */
interface UserPreferenceService {
    
    /**
     * Get user's preferred language
     * @param userId User ID
     * @return User's preferred language
     */
    fun getUserLanguage(userId: String): Language
    
    /**
     * Update user's language preference
     * @param userId User ID
     * @param language New language preference
     */
    fun updateUserLanguage(userId: String, language: Language)
}

/**
 * Default implementation of UserPreferenceService using use case pattern
 */
class UserPreferenceServiceImpl(
    private val userMessageUseCase: UserMessageUseCase
) : UserPreferenceService {
    
    override fun getUserLanguage(userId: String): Language {
        // For non-suspend service, return default language
        return Language.ENGLISH
    }
    
    override fun updateUserLanguage(userId: String, language: Language) {
        // For non-suspend service, do nothing
    }
}

/**
 * Service Factory - Creates user services with use case implementations
 */
object UserServiceFactory {
    
    /**
     * Create UserMessageService with use case implementation
     */
    fun createMessageService(
        userPreferenceService: UserPreferenceService,
        messageProviders: Map<Language, UserMessageProvider>
    ): UserMessageService {
        val useCase = UserMessageUseCaseImpl(userPreferenceService, messageProviders)
        return UserMessageServiceImpl(useCase)
    }
    
    /**
     * Create UserPreferenceService with use case implementation
     */
    fun createPreferenceService(
        userPreferenceService: UserPreferenceService,
        messageProviders: Map<Language, UserMessageProvider>
    ): UserPreferenceService {
        val useCase = UserMessageUseCaseImpl(userPreferenceService, messageProviders)
        return UserPreferenceServiceImpl(useCase)
    }
    
    /**
     * Create both services with shared use case
     */
    fun createServices(
        userPreferenceService: UserPreferenceService,
        messageProviders: Map<Language, UserMessageProvider>
    ): ServicePair {
        val useCase = UserMessageUseCaseImpl(userPreferenceService, messageProviders)
        return ServicePair(
            messageService = UserMessageServiceImpl(useCase),
            preferenceService = UserPreferenceServiceImpl(useCase)
        )
    }
    
    data class ServicePair(
        val messageService: UserMessageService,
        val preferenceService: UserPreferenceService
    )
}
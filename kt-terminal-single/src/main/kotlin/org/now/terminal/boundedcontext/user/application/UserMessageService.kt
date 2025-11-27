package org.now.terminal.boundedcontext.user.application

import org.now.terminal.shared.kernel.Language

/**
 * User Message Service - Provides localized messages for user domain
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
 * Default implementation of UserMessageService
 */
class UserMessageServiceImpl(
    private val userPreferenceService: UserPreferenceService,
    private val messageProviders: Map<Language, UserMessageProvider>
) : UserMessageService {
    
    override fun getMessage(key: String, params: Map<String, Any>): String {
        // Default to English if no specific user context
        val provider = messageProviders[Language.ENGLISH] ?: UserMessageProvider.English
        return provider.getMessage(key, params)
    }
    
    override fun getMessageForUser(userId: String, key: String, params: Map<String, Any>): String {
        val language = userPreferenceService.getUserLanguage(userId)
        val provider = messageProviders[language] ?: UserMessageProvider.English
        return provider.getMessage(key, params)
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
 * Default implementation of UserPreferenceService
 */
class UserPreferenceServiceImpl : UserPreferenceService {
    
    // In a real implementation, this would be stored in database
    private val userPreferences = mutableMapOf<String, Language>()
    
    override fun getUserLanguage(userId: String): Language {
        return userPreferences[userId] ?: Language.ENGLISH
    }
    
    override fun updateUserLanguage(userId: String, language: Language) {
        userPreferences[userId] = language
    }
}
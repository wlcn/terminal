package org.now.terminal.boundedcontext.user.application.usecase

import org.now.terminal.boundedcontext.user.application.UserMessageProvider
import org.now.terminal.boundedcontext.user.application.UserPreferenceService
import org.now.terminal.boundedcontext.user.domain.valueobjects.UserId
import org.now.terminal.shared.kernel.Language

/**
 * User Message Use Case - Handles localized message retrieval and user language preferences
 */
interface UserMessageUseCase {
    
    /**
     * Get localized message for a specific language
     */
    suspend fun getLocalizedMessage(query: GetLocalizedMessageQuery): LocalizedMessageResult
    
    /**
     * Get user's preferred language
     */
    suspend fun getUserLanguage(query: GetUserLanguageQuery): UserLanguageResult
    
    /**
     * Update user's language preference
     */
    suspend fun updateUserLanguage(command: UpdateUserLanguageCommand)
    
    /**
     * Get available languages
     */
    suspend fun getAvailableLanguages(): AvailableLanguagesResult
    
    /**
     * Validate message key
     */
    suspend fun validateMessageKey(query: ValidateMessageKeyQuery): MessageKeyValidationResult
}

/**
 * Commands and Queries for User Message Use Case
 */
data class GetLocalizedMessageQuery(
    val key: String,
    val language: Language,
    val params: Map<String, Any> = emptyMap()
)

data class GetUserLanguageQuery(
    val userId: UserId
)

data class UpdateUserLanguageCommand(
    val userId: UserId,
    val language: Language
)

data class ValidateMessageKeyQuery(
    val key: String,
    val language: Language
)

/**
 * Results for User Message Use Case
 */
data class LocalizedMessageResult(
    val message: String,
    val key: String,
    val language: Language
)

data class UserLanguageResult(
    val userId: UserId,
    val language: Language
)

data class AvailableLanguagesResult(
    val languages: List<Language>,
    val defaultLanguage: Language
)

data class MessageKeyValidationResult(
    val key: String,
    val language: Language,
    val isValid: Boolean,
    val message: String?
)

/**
 * Implementation of User Message Use Case
 */
class UserMessageUseCaseImpl(
    private val userPreferenceService: UserPreferenceService,
    private val messageProviders: Map<Language, UserMessageProvider>
) : UserMessageUseCase {
    
    override suspend fun getLocalizedMessage(query: GetLocalizedMessageQuery): LocalizedMessageResult {
        val provider = messageProviders[query.language] ?: UserMessageProvider.English
        val message = provider.getMessage(query.key, query.params)
        
        return LocalizedMessageResult(
            message = message,
            key = query.key,
            language = query.language
        )
    }
    
    override suspend fun getUserLanguage(query: GetUserLanguageQuery): UserLanguageResult {
        val language = userPreferenceService.getUserLanguage(query.userId.value)
        
        return UserLanguageResult(
            userId = query.userId,
            language = language
        )
    }
    
    override suspend fun updateUserLanguage(command: UpdateUserLanguageCommand) {
        userPreferenceService.updateUserLanguage(command.userId.value, command.language)
    }
    
    override suspend fun getAvailableLanguages(): AvailableLanguagesResult {
        val languages = messageProviders.keys.toList()
        
        return AvailableLanguagesResult(
            languages = languages,
            defaultLanguage = Language.ENGLISH
        )
    }
    
    override suspend fun validateMessageKey(query: ValidateMessageKeyQuery): MessageKeyValidationResult {
        val provider = messageProviders[query.language] ?: UserMessageProvider.English
        
        return try {
            val message = provider.getMessage(query.key)
            MessageKeyValidationResult(
                key = query.key,
                language = query.language,
                isValid = true,
                message = message
            )
        } catch (e: Exception) {
            MessageKeyValidationResult(
                key = query.key,
                language = query.language,
                isValid = false,
                message = null
            )
        }
    }
}
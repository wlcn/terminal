package org.now.terminal.boundedcontext.user.domain.valueobjects

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * User ID Value Object - ID generation with prefix marker
 */
@Serializable
@JvmInline
value class UserId(val value: String) {
    
    companion object {
        private const val USER_ID_REQUIRED = "user.id.required"
        private const val USER_ID_FORMAT_INVALID = "user.id.format.invalid"
        
        /**
         * Generate new user ID (with usr_ prefix)
         */
        fun generate(): UserId {
            val uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 12)
            return UserId("usr_${uuid}")
        }
        
        /**
         * Create user ID from string (validate prefix)
         */
        fun fromString(value: String): UserId {
            return UserId(value)
        }
    }
    
    init {
        require(value.isNotBlank()) { USER_ID_REQUIRED }
        require(value.startsWith("usr_")) { USER_ID_FORMAT_INVALID }
    }
    
    /**
     * Get short format of user ID (without prefix)
     */
    fun shortId(): String = value.substringAfter("usr_")
    
    /**
     * Check if user ID has valid format
     */
    fun isValidFormat(): Boolean = value.matches(Regex("^usr_[a-f0-9]{12}$"))
}
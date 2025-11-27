package org.now.terminal.boundedcontext.user.domain.valueobjects

import org.now.terminal.shared.kernel.utils.ValidationUtils

/**
 * Email Value Object
 */
@JvmInline
value class Email private constructor(val value: String) {
    
    companion object {
        private const val EMAIL_REQUIRED = "user.email.required"
        private const val EMAIL_FORMAT_INVALID = "user.email.format.invalid"
        
        private val EMAIL_REGEX = """^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$""".toRegex()
        
        /**
         * Create email value object
         */
        fun create(value: String): Email {
            ValidationUtils.validateString(value, EMAIL_REQUIRED)
            ValidationUtils.validatePattern(value, EMAIL_REGEX, EMAIL_FORMAT_INVALID)
            return Email(value)
        }
    }
    
    init {
        ValidationUtils.validateString(value, EMAIL_REQUIRED)
        ValidationUtils.validatePattern(value, EMAIL_REGEX, EMAIL_FORMAT_INVALID)
    }
    
    /**
     * Get email domain
     */
    fun getDomain(): String = value.substringAfter("@")
    
    /**
     * Get email username part
     */
    fun getUsername(): String = value.substringBefore("@")
    
    /**
     * Check if email is from common free email service
     */
    fun isFreeEmail(): Boolean {
        val domain = getDomain().lowercase()
        return domain.contains("gmail") || domain.contains("yahoo") || 
               domain.contains("hotmail") || domain.contains("outlook") ||
               domain.contains("icloud") || domain.contains("aol")
    }
    
    /**
     * Get email service provider
     */
    fun getProvider(): String {
        val domain = getDomain().lowercase()
        return when {
            domain.contains("gmail") -> "Google"
            domain.contains("yahoo") -> "Yahoo"
            domain.contains("hotmail") || domain.contains("outlook") -> "Microsoft"
            domain.contains("icloud") -> "Apple"
            domain.contains("aol") -> "AOL"
            else -> "Other"
        }
    }
    
    /**
     * Check if email is corporate email
     */
    fun isCorporateEmail(): Boolean = !isFreeEmail()
    
    /**
     * Get masked email (privacy protection)
     */
    fun getMasked(): String {
        val username = getUsername()
        val domain = getDomain()
        return if (username.length > 2) {
            "${username.take(2)}****@$domain"
        } else {
            "****@$domain"
        }
    }
}
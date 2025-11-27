package org.now.terminal.boundedcontext.user.domain.valueobjects

import org.now.terminal.shared.kernel.utils.ValidationUtils

/**
 * Phone Number Value Object
 */
data class PhoneNumber private constructor(val value: String) {
    
    companion object {
        private const val PHONE_REQUIRED = "user.phone.required"
        private const val PHONE_FORMAT_INVALID = "user.phone.format.invalid"
        
        private val PHONE_REGEX = """^[+]?[0-9]{10,15}$""".toRegex()
        
        fun create(value: String): PhoneNumber {
            val validatedValue = ValidationUtils.validateString(
                value = value,
                minLength = 10,
                maxLength = 15,
                errorMessageProvider = { PHONE_REQUIRED }
            )
            
            ValidationUtils.validatePattern(
                value = validatedValue,
                pattern = PHONE_REGEX,
                errorMessage = PHONE_FORMAT_INVALID
            )
            
            return PhoneNumber(validatedValue)
        }
    }
    
    init {
        ValidationUtils.validateString(
            value = value,
            minLength = 10,
            maxLength = 15,
            errorMessageProvider = { PHONE_REQUIRED }
        )
        
        ValidationUtils.validatePattern(
            value = value,
            pattern = PHONE_REGEX,
            errorMessage = PHONE_FORMAT_INVALID
        )
    }
    
    override fun toString(): String = value
}
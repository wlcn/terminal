package org.now.terminal.boundedcontext.user.domain.valueobjects

import org.now.terminal.shared.kernel.utils.ValidationUtils

/**
 * Phone Number Value Object
 */
data class PhoneNumber(val value: String) {
    
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
    
    /**
     * Get masked phone number for privacy protection
     */
    fun getMasked(): String {
        return if (value.length >= 7) {
            "${value.substring(0, 3)}****${value.substring(value.length - 4)}"
        } else {
            "****"
        }
    }
    
    /**
     * Get phone carrier information based on prefix
     */
    fun getCarrier(): String {
        val prefix = value.take(3)
        return when {
            prefix.startsWith("130") || prefix.startsWith("131") || prefix.startsWith("132") || 
            prefix.startsWith("155") || prefix.startsWith("156") || prefix.startsWith("185") || 
            prefix.startsWith("186") -> "China Unicom"
            prefix.startsWith("133") || prefix.startsWith("153") || prefix.startsWith("180") || 
            prefix.startsWith("181") || prefix.startsWith("189") -> "China Telecom"
            prefix.startsWith("134") || prefix.startsWith("135") || prefix.startsWith("136") || 
            prefix.startsWith("137") || prefix.startsWith("138") || prefix.startsWith("139") || 
            prefix.startsWith("150") || prefix.startsWith("151") || prefix.startsWith("152") || 
            prefix.startsWith("157") || prefix.startsWith("158") || prefix.startsWith("159") || 
            prefix.startsWith("182") || prefix.startsWith("183") || prefix.startsWith("184") || 
            prefix.startsWith("187") || prefix.startsWith("188") -> "China Mobile"
            else -> "Unknown Carrier"
        }
    }
}
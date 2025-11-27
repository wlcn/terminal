package org.now.terminal.shared.kernel.utils

/**
 * Validation utility class - Provides generic validation logic without hard-coded business rules
 */
object ValidationUtils {
    
    /**
     * Validate that string is not empty and length is within range
     * @param errorMessageProvider Error message provider function, allows custom error messages
     */
    fun validateString(
        value: String?,
        minLength: Int = 1,
        maxLength: Int = 255,
        errorMessageProvider: (String) -> String = { "Validation failed for: $it" }
    ): String {
        requireNotNull(value) { errorMessageProvider("Value cannot be null") }
        require(value.isNotBlank()) { errorMessageProvider("Value cannot be blank") }
        require(value.length in minLength..maxLength) { 
            errorMessageProvider("Length must be between $minLength and $maxLength") 
        }
        return value
    }
    
    /**
     * Validate regex pattern matching
     */
    fun validatePattern(
        value: String,
        pattern: Regex,
        errorMessage: String = "Pattern validation failed"
    ): String {
        require(pattern.matches(value)) { errorMessage }
        return value
    }
    
    /**
     * Validate number range
     */
    fun validateNumber(
        value: Number?,
        min: Number? = null,
        max: Number? = null,
        errorMessageProvider: (String) -> String = { "Validation failed for: $it" }
    ): Number {
        requireNotNull(value) { errorMessageProvider("Value cannot be null") }
        
        min?.let { 
            require(value.toDouble() >= it.toDouble()) { 
                errorMessageProvider("Value must be >= $min") 
            } 
        }
        max?.let { 
            require(value.toDouble() <= it.toDouble()) { 
                errorMessageProvider("Value must be <= $max") 
            } 
        }
        
        return value
    }
    
    /**
     * Validate collection is not empty
     */
    fun <T> validateNotEmpty(
        collection: Collection<T>?,
        errorMessage: String = "Collection cannot be empty"
    ): Collection<T> {
        requireNotNull(collection) { "Collection cannot be null" }
        require(collection.isNotEmpty()) { errorMessage }
        return collection
    }
    
    /**
     * Validate boolean condition
     */
    fun validateCondition(
        condition: Boolean,
        errorMessage: String
    ) {
        require(condition) { errorMessage }
    }
}
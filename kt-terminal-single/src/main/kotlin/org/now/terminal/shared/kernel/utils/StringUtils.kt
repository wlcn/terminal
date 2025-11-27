package org.now.terminal.shared.kernel.utils

/**
 * String utility class
 */
object StringUtils {
    
    /**
     * Generate random string
     */
    fun generateRandomString(length: Int = 16): String {
        val charPool = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..length)
            .map { charPool.random() }
            .joinToString("")
    }
    
    /**
     * Generate UUID
     */
    fun generateUUID(): String = java.util.UUID.randomUUID().toString()
    
    /**
     * Convert camel case to snake case
     */
    fun camelToSnakeCase(input: String): String {
        return input.replace(
            Regex("(?<=[a-z])[A-Z]"), "_$0"
        ).lowercase()
    }
    
    /**
     * Convert snake case to camel case
     */
    fun snakeToCamelCase(input: String): String {
        return input.split('_')
            .mapIndexed { index, part ->
                if (index == 0) part.lowercase() 
                else part.replaceFirstChar { it.uppercase() }
            }
            .joinToString("")
    }
    
    /**
     * Truncate string and add ellipsis
     */
    fun truncateWithEllipsis(input: String, maxLength: Int): String {
        return if (input.length <= maxLength) input 
        else input.substring(0, maxLength - 3) + "..."
    }
}
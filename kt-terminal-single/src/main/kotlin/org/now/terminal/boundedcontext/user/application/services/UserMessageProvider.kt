package org.now.terminal.boundedcontext.user.application

import org.now.terminal.shared.kernel.MessageProvider

/**
 * User Application Layer Message Provider - User Domain Specific
 */
sealed class UserMessageProvider : MessageProvider {
    
    /**
     * Chinese User Message Provider
     */
    object Chinese : UserMessageProvider() {
        private val messages = mapOf(
            // User related messages
            "user.username.required" to "Username is required",
            "user.username.length.invalid" to "Username length must be between {min}-{max} characters",
            "user.username.contains.space" to "Username cannot contain spaces",
            "user.email.required" to "Email is required",
            "user.email.format.invalid" to "Email format is invalid",
            "user.phone.required" to "Phone number is required",
            "user.phone.format.invalid" to "Phone number format is invalid",
            "user.role.invalid" to "User role is invalid",
            "user.id.format.invalid" to "User ID format is invalid",
            
            // Session limit related messages
            "session.limit.concurrent.invalid" to "Max concurrent sessions must be positive",
            "session.limit.concurrent.exceeded" to "Max concurrent sessions cannot exceed {max}",
            "session.limit.duration.invalid" to "Max session duration must be positive",
            "session.limit.duration.exceeded" to "Max session duration cannot exceed {maxHours} hours",
            
            // Business operation messages
            "user.created.success" to "User created successfully",
            "user.updated.success" to "User information updated successfully"
        )
        
        override fun getMessage(key: String, params: Map<String, Any>): String {
            val message = messages[key] ?: "Unknown message: $key"
            var formatted = message
            params.forEach { (paramKey, value) ->
                formatted = formatted.replace("{$paramKey}", value.toString())
            }
            return formatted
        }
    }
    
    /**
     * English User Message Provider
     */
    object English : UserMessageProvider() {
        private val messages = mapOf(
            // User related messages
            "user.username.required" to "Username is required",
            "user.username.length.invalid" to "Username length must be between {min}-{max} characters",
            "user.username.contains.space" to "Username cannot contain spaces",
            "user.email.required" to "Email is required",
            "user.email.format.invalid" to "Email format is invalid",
            "user.phone.required" to "Phone number is required",
            "user.phone.format.invalid" to "Phone number format is invalid",
            "user.role.invalid" to "User role is invalid",
            "user.id.format.invalid" to "User ID format is invalid",
            
            // Session limit related messages
            "session.limit.concurrent.invalid" to "Max concurrent sessions must be positive",
            "session.limit.concurrent.exceeded" to "Max concurrent sessions cannot exceed {max}",
            "session.limit.duration.invalid" to "Max session duration must be positive",
            "session.limit.duration.exceeded" to "Max session duration cannot exceed {maxHours} hours",
            
            // Business operation messages
            "user.created.success" to "User created successfully",
            "user.updated.success" to "User information updated successfully"
        )
        
        override fun getMessage(key: String, params: Map<String, Any>): String {
            val message = messages[key] ?: "Unknown message: $key"
            var formatted = message
            params.forEach { (paramKey, value) ->
                formatted = formatted.replace("{$paramKey}", value.toString())
            }
            return formatted
        }
    }
}
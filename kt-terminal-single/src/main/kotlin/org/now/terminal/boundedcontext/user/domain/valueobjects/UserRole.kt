package org.now.terminal.boundedcontext.user.domain.valueobjects

/**
 * User Role Enumeration
 */
enum class UserRole(val allowedShells: Set<String>) {
    ADMIN(setOf("bash", "cmd", "powershell", "zsh")),
    DEVELOPER(setOf("bash", "cmd", "powershell")),
    GUEST(setOf("bash"));
    
    /**
     * Check if role allows using specific shell
     */
    fun canUseShell(shell: String): Boolean {
        return allowedShells.contains(shell)
    }
    
    /**
     * Check if role has specific permission
     */
    fun hasPermission(permission: String): Boolean {
        return when (this) {
            ADMIN -> true // Administrator has all permissions
            DEVELOPER -> permission in setOf("create_session", "execute_commands", "manage_own_sessions")
            GUEST -> permission == "execute_commands"
        }
    }
    
    /**
     * Safely create user role from string
     */
    companion object {
        fun fromString(value: String): UserRole? {
            return try {
                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                null
            }
        }
        
        /**
         * Check if string is valid role name
         */
        fun isValidRole(value: String): Boolean {
            return fromString(value) != null
        }
    }
}
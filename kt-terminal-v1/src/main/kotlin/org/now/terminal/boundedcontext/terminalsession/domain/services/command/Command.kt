package org.now.terminal.boundedcontext.terminalsession.domain.services.command

/**
 * Command value object - represents a command to be executed
 * 
 * This class only contains the truly dynamic parameters that vary per command.
 * Static parameters like working directory, environment variables, and shell type
 * should be obtained from the TerminalSession context.
 */
data class Command(
    val value: String,
    val timeoutMs: Long? = null
) {
    init {
        require(value.isNotBlank()) { "Command cannot be empty" }
        timeoutMs?.let {
            require(it > 0) { "Timeout must be positive" }
        }
    }
    
    /**
     * Check if this command has a timeout
     */
    val hasTimeout: Boolean
        get() = timeoutMs != null
    
    /**
     * Get the command as a string suitable for execution
     */
    fun toExecutableString(): String {
        return value.trim()
    }
    
    /**
     * Create a copy with a timeout
     */
    fun withTimeout(timeoutMs: Long?): Command {
        return this.copy(timeoutMs = timeoutMs)
    }
}
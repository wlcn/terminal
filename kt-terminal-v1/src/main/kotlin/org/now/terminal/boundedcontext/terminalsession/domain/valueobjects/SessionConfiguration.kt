package org.now.terminal.boundedcontext.terminalsession.domain.valueobjects

import kotlinx.serialization.Serializable

/**
 * Terminal Session Configuration Value Object
 * Contains user-configurable properties that are set at session creation
 */
@Serializable
data class SessionConfiguration(
    val shellType: ShellType,
    val workingDirectory: String,
    val terminalSize: TerminalSize = TerminalSize.DEFAULT,
    val environmentVariables: Map<String, String> = emptyMap()
) {
    
    companion object {
        private const val WORKING_DIRECTORY_REQUIRED = "session.working.directory.required"
        
        /**
         * Create default configuration
         */
        fun default(): SessionConfiguration {
            return SessionConfiguration(
                shellType = ShellType.BASH,
                workingDirectory = "/",
                terminalSize = TerminalSize.DEFAULT,
                environmentVariables = emptyMap()
            )
        }
    }
    
    init {
        require(workingDirectory.isNotBlank()) { WORKING_DIRECTORY_REQUIRED }
    }
    
    /**
     * Update working directory
     */
    fun updateWorkingDirectory(newDirectory: String): SessionConfiguration {
        return this.copy(workingDirectory = newDirectory)
    }
    
    /**
     * Update environment variables
     */
    fun updateEnvironmentVariables(newVariables: Map<String, String>): SessionConfiguration {
        return this.copy(environmentVariables = newVariables)
    }
    
    /**
     * Add or update a single environment variable
     */
    fun setEnvironmentVariable(key: String, value: String): SessionConfiguration {
        val updatedVariables = environmentVariables.toMutableMap()
        updatedVariables[key] = value
        return this.copy(environmentVariables = updatedVariables)
    }
    
    /**
     * Remove an environment variable
     */
    fun removeEnvironmentVariable(key: String): SessionConfiguration {
        val updatedVariables = environmentVariables.toMutableMap()
        updatedVariables.remove(key)
        return this.copy(environmentVariables = updatedVariables)
    }
    
    /**
     * Update terminal size
     */
    fun updateTerminalSize(newSize: TerminalSize): SessionConfiguration {
        return this.copy(terminalSize = newSize)
    }
    
    /**
     * Resize terminal
     */
    fun resizeTerminal(rows: Int, cols: Int): SessionConfiguration {
        val newSize = TerminalSize.create(rows, cols)
        return this.copy(terminalSize = newSize)
    }
}
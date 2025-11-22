package org.now.terminal.session.domain.valueobjects

import kotlinx.serialization.Serializable
import org.now.terminal.infrastructure.configuration.ConfigurationManager

/**
 * PTY配置值对象
 */
@Serializable
data class PtyConfiguration(
    val command: TerminalCommand,
    val environment: Map<String, String>,
    val size: TerminalSize,
    val workingDirectory: String? = null,
    val initialTerm: String = ConfigurationManager.getTerminalConfig().defaultTerm
) {
    init {
        require(environment.isNotEmpty()) {
            "Environment cannot be empty"
        }
        require(environment.all { it.key.isNotBlank() && it.value.isNotBlank() }) {
            "Environment variables cannot be blank"
        }
    }
    
    companion object {
        fun createDefault(command: TerminalCommand): PtyConfiguration {
            val terminalConfig = ConfigurationManager.getTerminalConfig()
            val env = mutableMapOf<String, String>()
            env.putAll(terminalConfig.pty.defaultEnvironment)
            
            return PtyConfiguration(
                command = command,
                environment = env,
                size = TerminalSize.DEFAULT,
                workingDirectory = terminalConfig.pty.defaultWorkingDirectory
            )
        }
    }
}
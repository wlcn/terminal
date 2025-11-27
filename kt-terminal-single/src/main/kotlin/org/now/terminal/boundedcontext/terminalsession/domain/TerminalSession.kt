package org.now.terminal.boundedcontext.terminalsession.domain

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.now.terminal.boundedcontext.terminalsession.domain.valueobjects.SessionConfiguration
import org.now.terminal.boundedcontext.terminalsession.domain.valueobjects.SessionStatus
import org.now.terminal.boundedcontext.terminalsession.domain.valueobjects.ShellType
import org.now.terminal.boundedcontext.terminalsession.domain.valueobjects.TerminalSessionId
import org.now.terminal.shared.valueobjects.UserId
import java.time.Instant
import org.now.terminal.boundedcontext.terminalsession.domain.valueobjects.TerminalSize

/**
 * Terminal Session Aggregate Root
 */
@Serializable
data class TerminalSession(
    val id: TerminalSessionId,
    val userId: UserId,
    val title: String,
    val hostname: String,
    val configuration: SessionConfiguration,
    val status: SessionStatus,
    val commandHistory: List<String> = emptyList(),
    val outputBuffer: String = "",
    @Contextual val startedAt: Instant = Instant.now(),
    @Contextual val lastActivityAt: Instant = Instant.now(),
    @Contextual val terminatedAt: Instant? = null
) {
    
    companion object {
        private const val TITLE_REQUIRED = "session.title.required"
        private const val TITLE_LENGTH_INVALID = "session.title.length.invalid"
        private const val HOSTNAME_REQUIRED = "session.hostname.required"
        private const val CONFIGURATION_REQUIRED = "session.configuration.required"
        
        /**
         * Generate a default session title based on timestamp
         */
        fun generateDefaultTitle(): String {
            return "Terminal Session ${java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}"
        }
    }
    
    init {
        require(title.isNotBlank()) { TITLE_REQUIRED }
        require(title.length in 1..100) { TITLE_LENGTH_INVALID }
        require(hostname.isNotBlank()) { HOSTNAME_REQUIRED }
        require(configuration != null) { CONFIGURATION_REQUIRED }
    }
    
    /**
     * Check if session is active
     */
    val isActive: Boolean
        get() = status.isActive
    
    /**
     * Get session duration in seconds
     */
    val durationSeconds: Long
        get() = if (terminatedAt != null) {
            terminatedAt.epochSecond - startedAt.epochSecond
        } else {
            Instant.now().epochSecond - startedAt.epochSecond
        }
    
    /**
     * Update session status
     */
    fun updateStatus(newStatus: SessionStatus): TerminalSession {
        val now = Instant.now()
        return this.copy(
            status = newStatus,
            lastActivityAt = now,
            terminatedAt = if (newStatus == SessionStatus.TERMINATED) now else terminatedAt
        )
    }
    
    /**
     * Terminate the session
     */
    fun terminate(): TerminalSession {
        return updateStatus(SessionStatus.TERMINATED)
    }
    
    /**
     * Record command execution
     */
    fun recordCommand(command: String): TerminalSession {
        val updatedHistory = commandHistory + command
        return this.copy(
            commandHistory = updatedHistory,
            lastActivityAt = Instant.now()
        )
    }
    
    /**
     * Append output to buffer
     */
    fun appendOutput(output: String): TerminalSession {
        val updatedBuffer = if (outputBuffer.isBlank()) output else "$outputBuffer\n$output"
        return this.copy(
            outputBuffer = updatedBuffer,
            lastActivityAt = Instant.now()
        )
    }
    
    /**
     * Clear output buffer
     */
    fun clearOutputBuffer(): TerminalSession {
        return this.copy(
            outputBuffer = "",
            lastActivityAt = Instant.now()
        )
    }
    
    /**
     * Update working directory
     */
    fun updateWorkingDirectory(newDirectory: String): TerminalSession {
        val updatedConfig = configuration.updateWorkingDirectory(newDirectory)
        return this.copy(
            configuration = updatedConfig,
            lastActivityAt = Instant.now()
        )
    }
    
    /**
     * Update environment variables
     */
    fun updateEnvironmentVariables(newVariables: Map<String, String>): TerminalSession {
        val updatedConfig = configuration.updateEnvironmentVariables(newVariables)
        return this.copy(
            configuration = updatedConfig,
            lastActivityAt = Instant.now()
        )
    }
    
    /**
     * Set a single environment variable
     */
    fun setEnvironmentVariable(key: String, value: String): TerminalSession {
        val updatedConfig = configuration.setEnvironmentVariable(key, value)
        return this.copy(
            configuration = updatedConfig,
            lastActivityAt = Instant.now()
        )
    }
    
    /**
     * Remove an environment variable
     */
    fun removeEnvironmentVariable(key: String): TerminalSession {
        val updatedConfig = configuration.removeEnvironmentVariable(key)
        return this.copy(
            configuration = updatedConfig,
            lastActivityAt = Instant.now()
        )
    }
    
    /**
     * Update session title
     */
    fun updateTitle(newTitle: String): TerminalSession {
        return this.copy(
            title = newTitle,
            lastActivityAt = Instant.now()
        )
    }
    
    /**
     * Resize terminal
     */
    fun resizeTerminal(rows: Int, cols: Int): TerminalSession {
        val updatedConfig = configuration.resizeTerminal(rows, cols)
        return this.copy(
            configuration = updatedConfig,
            lastActivityAt = Instant.now()
        )
    }
    
    /**
     * Update terminal size
     */
    fun updateTerminalSize(newSize: TerminalSize): TerminalSession {
        val updatedConfig = configuration.updateTerminalSize(newSize)
        return this.copy(
            configuration = updatedConfig,
            lastActivityAt = Instant.now()
        )
    }
    
    /**
     * Get session summary
     */
    fun getSummary(): SessionSummary {
        return SessionSummary(
            id = id,
            title = title,
            shellType = configuration.shellType,
            status = status,
            durationSeconds = durationSeconds,
            commandCount = commandHistory.size
        )
    }
    
    /**
     * Check if session has been inactive for too long
     */
    fun isInactiveFor(minutes: Long): Boolean {
        val inactiveDuration = Instant.now().epochSecond - lastActivityAt.epochSecond
        return inactiveDuration > minutes * 60
    }
}

/**
 * Session summary data class
 */
@Serializable
data class SessionSummary(
    val id: TerminalSessionId,
    val title: String,
    val shellType: ShellType,
    val status: SessionStatus,
    val durationSeconds: Long,
    val commandCount: Int
)
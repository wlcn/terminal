package org.now.terminal.boundedcontexts.terminalsession.domain.model

/**
 * Terminal Configuration Model
 * Represents the configuration for terminal sessions
 */
data class TerminalConfig(
    val defaultShellType: String,
    val sessionTimeoutMs: Long,
    val defaultTerminalSize: TerminalSize = TerminalSize(80, 24)
)


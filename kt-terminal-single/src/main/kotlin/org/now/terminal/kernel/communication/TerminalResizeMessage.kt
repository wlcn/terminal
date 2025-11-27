package org.now.terminal.kernel.communication

/**
 * Terminal resize message - represents terminal size change
 */
data class TerminalResizeMessage(
    val columns: Int,
    val rows: Int,
    val sessionId: String
)
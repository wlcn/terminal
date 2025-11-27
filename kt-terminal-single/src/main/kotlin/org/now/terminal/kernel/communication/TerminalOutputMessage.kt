package org.now.terminal.kernel.communication

/**
 * Terminal output message - represents output from terminal to client
 */
data class TerminalOutputMessage(
    val output: String,
    val sessionId: String
)
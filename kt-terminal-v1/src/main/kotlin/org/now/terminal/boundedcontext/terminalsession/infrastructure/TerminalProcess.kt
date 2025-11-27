package org.now.terminal.boundedcontext.terminalsession.infrastructure

/**
 * Terminal process abstraction - defines the contract for terminal process operations
 */
interface TerminalProcess {
    val isAlive: Boolean
    
    fun writeInput(input: String)
    fun readOutput(): String
    fun readError(): String
    fun resizeTerminal(rows: Int, cols: Int)
    fun terminate()
    fun waitFor(): Int
}
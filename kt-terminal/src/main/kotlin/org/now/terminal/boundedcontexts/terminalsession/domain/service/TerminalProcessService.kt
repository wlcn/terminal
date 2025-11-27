package org.now.terminal.boundedcontexts.terminalsession.domain.service

// Define abstract interface for terminal process management - should be in domain layer
interface TerminalProcessManager {
    fun createProcess(sessionId: String, workingDirectory: String, shellType: String = "bash"): TerminalProcess
    fun getProcess(sessionId: String): TerminalProcess?
    fun writeToProcess(sessionId: String, data: String): Boolean
    fun resizeProcess(sessionId: String, columns: Int, rows: Int): Boolean
    fun terminateProcess(sessionId: String): Boolean
    fun interruptProcess(sessionId: String): Boolean
}

// Define abstract interface for terminal process - should be in domain layer
interface TerminalProcess {
    fun write(data: String): Boolean
    fun resize(columns: Int, rows: Int)
    fun terminate()
    fun interrupt()
    fun addOutputListener(listener: (String) -> Unit)
    fun removeOutputListener(listener: (String) -> Unit)
    fun isAlive(): Boolean
    fun startReading()
}

// TerminalProcessService now depends on the abstract interface instead of concrete implementation
class TerminalProcessService(private val processManager: TerminalProcessManager) : TerminalProcessManager by processManager



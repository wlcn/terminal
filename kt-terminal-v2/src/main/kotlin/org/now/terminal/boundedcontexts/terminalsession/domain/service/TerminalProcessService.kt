package org.now.terminal.boundedcontexts.terminalsession.domain.service

import java.util.UUID

// Define abstract interface for terminal process management - should be in domain layer
interface TerminalProcessManager {
    fun createProcess(sessionId: UUID, workingDirectory: String, shellType: String = "bash"): TerminalProcess
    fun getProcess(sessionId: UUID): TerminalProcess?
    fun writeToProcess(sessionId: UUID, data: String): Boolean
    fun resizeProcess(sessionId: UUID, columns: Int, rows: Int): Boolean
    fun terminateProcess(sessionId: UUID): Boolean
    fun interruptProcess(sessionId: UUID): Boolean
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



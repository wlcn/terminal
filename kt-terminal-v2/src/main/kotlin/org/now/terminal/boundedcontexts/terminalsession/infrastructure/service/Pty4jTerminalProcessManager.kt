package org.now.terminal.boundedcontexts.terminalsession.infrastructure.service

import com.pty4j.PtyProcess
import com.pty4j.PtyProcessBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.now.terminal.boundedcontexts.terminalsession.domain.service.TerminalProcess
import org.now.terminal.boundedcontexts.terminalsession.domain.service.TerminalProcessManager
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

// Concrete implementation using pty4j - should be in infrastructure layer
class Pty4jTerminalProcessManager : TerminalProcessManager {
    private val processes = ConcurrentHashMap<UUID, TerminalProcess>()
    
    override fun createProcess(sessionId: UUID, workingDirectory: String, shellType: String): TerminalProcess {
        val process = Pty4jTerminalProcess(sessionId, workingDirectory, shellType)
        processes[sessionId] = process
        process.startReading()
        return process
    }
    
    override fun getProcess(sessionId: UUID): TerminalProcess? {
        return processes[sessionId]
    }
    
    override fun writeToProcess(sessionId: UUID, data: String): Boolean {
        val process = processes[sessionId] ?: return false
        return process.write(data)
    }
    
    override fun resizeProcess(sessionId: UUID, columns: Int, rows: Int): Boolean {
        val process = processes[sessionId] ?: return false
        process.resize(columns, rows)
        return true
    }
    
    override fun terminateProcess(sessionId: UUID): Boolean {
        val process = processes.remove(sessionId) ?: return false
        process.terminate()
        return true
    }
    
    override fun interruptProcess(sessionId: UUID): Boolean {
        val process = processes[sessionId] ?: return false
        process.interrupt()
        return true
    }
}

// Concrete terminal process implementation using pty4j - should be in infrastructure layer
class Pty4jTerminalProcess(
    private val sessionId: UUID,
    workingDirectory: String,
    shellType: String
) : TerminalProcess {
    private val process: PtyProcess
    private val inputStream: InputStream
    private val outputStream: OutputStream
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val outputListeners = mutableListOf<(String) -> Unit>()
    private var readJob: Job? = null
    private var isTerminated = false
    
    init {
        val shell = when (shellType.lowercase()) {
            "bash" -> arrayOf("bash")
            "sh" -> arrayOf("sh")
            "cmd" -> arrayOf("cmd.exe")
            "powershell" -> arrayOf("powershell.exe")
            else -> arrayOf("bash")
        }
        
        process = PtyProcessBuilder()
            .setCommand(shell)
            .setDirectory(workingDirectory)
            .setEnvironment(System.getenv())
            .start()
        
        inputStream = process.inputStream
        outputStream = process.outputStream
    }
    
    override fun startReading() {
        // Use coroutine and flow to read from process output
        readJob = scope.launch {
            createOutputFlow()
                .buffer(1024) // Buffer to handle burst output
                .onEach { output ->
                    // Send output to all listeners
                    outputListeners.forEach { it(output) }
                }
                .onCompletion {
                    // Cleanup when flow completes
                    cleanupResources()
                }
                .collect { }
        }
    }
    
    /**
     * Create a flow that emits terminal output
     */
    private fun createOutputFlow(): Flow<String> = flow {
        val buffer = ByteArray(1024)
        var len: Int
        
        try {
            while (process.isAlive && !isTerminated) {
                len = inputStream.read(buffer)
                if (len == -1) break
                val output = String(buffer, 0, len)
                emit(output)
            }
        } catch (e: Exception) {
            // Process closed or error occurred
            if (!isTerminated) {
                throw e // Re-throw if not explicitly terminated
            }
        }
    }.flowOn(Dispatchers.IO)
    
    override fun write(data: String): Boolean {
        if (isTerminated) return false
        
        return try {
            outputStream.write(data.toByteArray())
            outputStream.flush()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override fun resize(columns: Int, rows: Int) {
        if (isTerminated) return
        
        // TODO: Fix this method - find the correct method name for pty4j
        // For now, this method is not implemented
    }
    
    override fun terminate() {
        if (isTerminated) return
        
        isTerminated = true
        cleanupResources()
    }
    
    /**
     * Cleanup all resources safely
     */
    private fun cleanupResources() {
        // Cancel all coroutines
        scope.cancel()
        
        // Remove all listeners to prevent memory leaks
        outputListeners.clear()
        
        try {
            // Close streams explicitly
            inputStream.close()
            outputStream.close()
        } catch (e: Exception) {
            // Ignore stream close errors
        }
        
        try {
            // Destroy the process
            if (process.isAlive) {
                process.destroy()
            }
        } catch (e: Exception) {
            // Ignore process destroy errors
        }
    }
    
    override fun interrupt() {
        if (isTerminated) return
        
        // Send Ctrl+C signal
        write("\u0003")
    }
    
    override fun addOutputListener(listener: (String) -> Unit) {
        if (isTerminated) return
        
        outputListeners.add(listener)
    }
    
    override fun removeOutputListener(listener: (String) -> Unit) {
        outputListeners.remove(listener)
    }
    
    override fun isAlive(): Boolean {
        return !isTerminated && process.isAlive
    }
    
    /**
     * Finalizer as a safety net to ensure resources are released
     */
    @Suppress("deprecation")
    protected fun finalize() {
        if (!isTerminated) {
            cleanupResources()
        }
    }
}

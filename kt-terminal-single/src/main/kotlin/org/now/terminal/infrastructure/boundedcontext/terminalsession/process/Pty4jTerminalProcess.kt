package org.now.terminal.infrastructure.boundedcontext.terminalsession.process

import com.pty4j.PtyProcess
import com.pty4j.PtyProcessBuilder
import com.pty4j.WinSize
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.now.terminal.boundedcontext.terminalsession.domain.TerminalSession
import org.now.terminal.boundedcontext.terminalsession.domain.valueobjects.ShellType
import org.now.terminal.boundedcontext.terminalsession.infrastructure.TerminalProcess
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.channels.onClosed

/**
 * Real-time terminal process implementation using Pty4j
 * Provides true interactive terminal functionality
 */
class Pty4jTerminalProcess(
    private val shellType: ShellType,
    private val workingDirectory: String,
    private val environment: Map<String, String>,
    private val terminalSize: org.now.terminal.boundedcontext.terminalsession.domain.valueobjects.TerminalSize
) : TerminalProcess {
    
    private val logger = LoggerFactory.getLogger(Pty4jTerminalProcess::class.java)
    
    private var ptyProcess: PtyProcess? = null
    
    // Coroutine scope for async operations
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Channels for async output streaming
    private val outputChannel = Channel<String>(Channel.UNLIMITED)
    private val errorChannel = Channel<String>(Channel.UNLIMITED)
    
    // Buffers for immediate read operations
    private val outputBuffer = ConcurrentLinkedQueue<String>()
    private val errorBuffer = ConcurrentLinkedQueue<String>()
    
    override val isAlive: Boolean
        get() = ptyProcess?.isAlive ?: false
    
    init {
        createProcess()
    }
    
    private fun createProcess() {
        try {
            // Build the command based on shell type
            val command = when (shellType) {
                ShellType.BASH -> arrayOf("bash")
                ShellType.ZSH -> arrayOf("zsh")
                ShellType.FISH -> arrayOf("fish")
                ShellType.POWERSHELL -> arrayOf("powershell.exe")
                ShellType.CMD -> arrayOf("cmd.exe")
            }
            
            // Create PtyProcess
            val processBuilder = PtyProcessBuilder(command)
                .setDirectory(workingDirectory)
                .setEnvironment(environment)
                .setInitialColumns(terminalSize.cols)
                .setInitialRows(terminalSize.rows)
            
            ptyProcess = processBuilder.start()
            
            // Start output and error monitoring threads
            startOutputMonitoring()
            startErrorMonitoring()
            
            logger.info("✅ Created real-time terminal process for shell type: {}", shellType)
            
        } catch (e: Exception) {
            logger.error("❌ Failed to create terminal process: {}", e.message)
            throw RuntimeException("Failed to create terminal process", e)
        }
    }
    
    override fun writeInput(input: String) {
        try {
            ptyProcess?.outputStream?.write(input.toByteArray(StandardCharsets.UTF_8))
            ptyProcess?.outputStream?.flush()
            logger.debug("📝 Wrote input to terminal process: {}", input.take(50))
        } catch (e: Exception) {
            logger.error("❌ Failed to write input to terminal process: {}", e.message)
        }
    }
    
    override fun readOutput(): String {
        return synchronized(outputBuffer) {
            if (outputBuffer.isNotEmpty()) {
                val output = outputBuffer.joinToString("")
                outputBuffer.clear()
                output
            } else {
                ""
            }
        }
    }
    
    override fun readError(): String {
        return synchronized(errorBuffer) {
            if (errorBuffer.isNotEmpty()) {
                val error = errorBuffer.joinToString("")
                errorBuffer.clear()
                error
            } else {
                ""
            }
        }
    }
    
    /**
     * Get the output channel for async streaming
     */
    fun getOutputChannel(): Channel<String> = outputChannel
    
    /**
     * Get the error channel for async streaming
     */
    fun getErrorChannel(): Channel<String> = errorChannel
    
    override fun resizeTerminal(rows: Int, cols: Int) {
        try {
            ptyProcess?.winSize = WinSize(cols, rows)
            logger.info("📐 Resized terminal to {}x{}", cols, rows)
        } catch (e: Exception) {
            logger.error("❌ Failed to resize terminal: {}", e.message)
        }
    }
    
    override fun terminate() {
        try {
            // Cancel coroutines
            coroutineScope.cancel("Terminal process terminated")
            
            // Close channels
            outputChannel.close()
            errorChannel.close()
            
            // Terminate the process
            ptyProcess?.destroy()
            logger.info("🛑 Terminated terminal process")
        } catch (e: Exception) {
            logger.error("❌ Failed to terminate terminal process: {}", e.message)
        }
    }
    
    override fun waitFor(): Int {
        return ptyProcess?.waitFor() ?: -1
    }
    
    private fun startOutputMonitoring() {
        coroutineScope.launch {
            try {
                val inputStream: InputStream = ptyProcess?.inputStream ?: return@launch
                val buffer = ByteArray(1024)
                
                while (isAlive && isActive) {
                    val bytesRead = withContext(Dispatchers.IO) {
                        inputStream.read(buffer)
                    }
                    if (bytesRead > 0) {
                        val output = String(buffer, 0, bytesRead, StandardCharsets.UTF_8)
                        
                        // Send to async channel for real-time streaming
                        outputChannel.trySend(output).onClosed {
                            logger.info("Can not send: the outputChannel is closed")
                        }
                        
                        // Also buffer for immediate read operations
                        synchronized(outputBuffer) {
                            outputBuffer.add(output)
                        }
                        logger.debug("📥 Received output from terminal: {}", output.take(50))
                    }
                }
            } catch (e: CancellationException) {
                // Normal cancellation, ignore
            } catch (e: Exception) {
                if (isActive) {
                    logger.error("❌ Error monitoring terminal output: {}", e.message)
                }
            }
        }
    }
    
    private fun startErrorMonitoring() {
        coroutineScope.launch {
            try {
                val errorStream: InputStream = ptyProcess?.errorStream ?: return@launch
                val buffer = ByteArray(1024)
                
                while (isAlive && isActive) {
                    val bytesRead = withContext(Dispatchers.IO) {
                        errorStream.read(buffer)
                    }
                    if (bytesRead > 0) {
                        val error = String(buffer, 0, bytesRead, StandardCharsets.UTF_8)
                        

                        errorChannel.trySend(error).onClosed {
                            logger.info("Can not send: the channel is closed")
                        }
                        
                        // Also buffer for immediate read operations
                        synchronized(errorBuffer) {
                            errorBuffer.add(error)
                        }
                        logger.debug("📥 Received error from terminal: {}", error.take(50))
                    }
                }
            } catch (e: CancellationException) {
                // Normal cancellation, ignore
            } catch (e: Exception) {
                if (isActive) {
                    logger.error("❌ Error monitoring terminal error: {}", e.message)
                }
            }
        }
    }
}
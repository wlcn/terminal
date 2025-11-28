package org.now.terminal.boundedcontexts.terminalsession.infrastructure.service

import com.pty4j.PtyProcess
import com.pty4j.PtyProcessBuilder
import com.pty4j.WinSize
import io.ktor.server.config.*
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
import java.util.concurrent.ConcurrentHashMap

// 配置类，用于封装终端配置
class TerminalConfig(
    val defaultShellType: String,
    val defaultSize: TerminalSizeConfig,
    val defaultWorkingDirectory: String,
    val shells: Map<String, ShellConfig>
)

// 终端尺寸配置
class TerminalSizeConfig(
    val columns: Int,
    val rows: Int
)

// Shell配置
class ShellConfig(
    val command: List<String>,
    val environment: Map<String, String>
)

// 配置管理器，用于读取application.conf中的终端配置
object TerminalConfigManager {
    private lateinit var config: ApplicationConfig
    
    fun init(config: ApplicationConfig) {
        this.config = config
    }
    
    fun getTerminalConfig(): TerminalConfig {
        val terminalConfig = config.config("terminal")
        
        val defaultShellType = terminalConfig.property("defaultShellType").getString()
        val defaultSize = TerminalSizeConfig(
            terminalConfig.config("defaultSize").property("columns").getString().toInt(),
            terminalConfig.config("defaultSize").property("rows").getString().toInt()
        )
        val defaultWorkingDirectory = terminalConfig.property("defaultWorkingDirectory").getString()
        
        val shells = mutableMapOf<String, ShellConfig>()
        
        // 遍历所有shell配置
        terminalConfig.config("shells").keys().forEach { shellName ->
            val shellConfig = terminalConfig.config("shells").config(shellName)
            val command = shellConfig.property("command").getList()
            
            // 获取环境变量配置
            val environment = mutableMapOf<String, String>()
            try {
                val envConfig = shellConfig.config("environment")
                envConfig.keys().forEach { key ->
                    environment[key] = envConfig.property(key).getString()
                }
            } catch (e: Exception) {
                // 环境配置不存在，使用空的环境变量
            }
            
            shells[shellName] = ShellConfig(command, environment)
        }
        
        return TerminalConfig(defaultShellType, defaultSize, defaultWorkingDirectory, shells)
    }
}

// Concrete implementation using pty4j - should be in infrastructure layer
class Pty4jTerminalProcessManager : TerminalProcessManager {
    private val processes = ConcurrentHashMap<String, TerminalProcess>()
    
    override fun createProcess(sessionId: String, workingDirectory: String, shellType: String): TerminalProcess {
        val process = Pty4jTerminalProcess(sessionId, workingDirectory, shellType)
        processes[sessionId] = process
        process.startReading()
        return process
    }
    
    override fun getProcess(sessionId: String): TerminalProcess? {
        return processes[sessionId]
    }
    
    override fun writeToProcess(sessionId: String, data: String): Boolean {
        val process = processes[sessionId] ?: return false
        return process.write(data)
    }
    
    override fun resizeProcess(sessionId: String, columns: Int, rows: Int): Boolean {
        val process = processes[sessionId] ?: return false
        process.resize(columns, rows)
        return true
    }
    
    override fun terminateProcess(sessionId: String): Boolean {
        val process = processes.remove(sessionId) ?: return false
        process.terminate()
        return true
    }
    
    override fun interruptProcess(sessionId: String): Boolean {
        val process = processes[sessionId] ?: return false
        process.interrupt()
        return true
    }
}

// Concrete terminal process implementation using pty4j - should be in infrastructure layer
class Pty4jTerminalProcess(
    private val sessionId: String,
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
        // 获取终端配置
        val terminalConfig = TerminalConfigManager.getTerminalConfig()
        
        // 获取shell配置，如果没有找到则使用默认配置
        val shellConfig = terminalConfig.shells[shellType.lowercase()] ?: 
            terminalConfig.shells[terminalConfig.defaultShellType.lowercase()] ?: 
            throw IllegalArgumentException("No shell configuration found for type: $shellType")
        
        // 合并系统环境变量和配置文件中的环境变量
        val environment = System.getenv().toMutableMap()
        environment.putAll(shellConfig.environment)
        
        // 使用配置文件中的命令
        val command = shellConfig.command.toTypedArray()
        
        process = PtyProcessBuilder()
            .setCommand(command)
            .setDirectory(workingDirectory)
            .setEnvironment(environment)
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
            // Use direct method call instead of reflection for write operation
            // since write is a standard method in OutputStream
            outputStream.write(data.toByteArray())
            outputStream.flush()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override fun resize(columns: Int, rows: Int) {
        if (isTerminated) return
        
        // Direct call to pty4j 0.13.11 resize method
        process.winSize = WinSize(columns, rows)
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

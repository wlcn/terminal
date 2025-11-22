package org.now.terminal.session.infrastructure.process

import com.pty4j.PtyProcess
import com.pty4j.PtyProcessBuilder
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import org.now.terminal.session.domain.valueobjects.ShellType
import org.now.terminal.session.domain.services.Process
import org.now.terminal.session.domain.valueobjects.PtyConfiguration
import org.now.terminal.session.domain.valueobjects.TerminalSize
import org.now.terminal.shared.valueobjects.SessionId
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import kotlin.coroutines.CoroutineContext

/**
 * 基于Pty4j的Process接口实现
 * 提供伪终端进程管理功能
 */
class Pty4jProcess(
    private val ptyConfig: PtyConfiguration,
    private val sessionId: SessionId,
    private val customCoroutineContext: CoroutineContext = Dispatchers.IO
) : Process, CoroutineScope {
    
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext = customCoroutineContext + job
    
    private lateinit var ptyProcess: PtyProcess
    private lateinit var outputStream: OutputStream
    private lateinit var inputStream: InputStream
    
    private val outputBuffer = StringBuilder()
    private val outputChannel = Channel<String>(Channel.BUFFERED)
    
    override fun writeInput(input: String) {
        if (::outputStream.isInitialized && isAlive()) {
            try {
                outputStream.write(input.toByteArray(StandardCharsets.UTF_8))
                outputStream.flush()
            } catch (e: Exception) {
                throw RuntimeException("Failed to write input to process", e)
            }
        }
    }
    
    override fun readOutput(): String {
        return synchronized(outputBuffer) {
            val output = outputBuffer.toString()
            outputBuffer.clear()
            output
        }
    }
    
    override fun resize(size: TerminalSize) {
        if (::ptyProcess.isInitialized && isAlive()) {
            ptyProcess.setWinSize(
                com.pty4j.WinSize(
                    size.columns,
                    size.rows
                )
            )
        }
    }
    
    override fun terminate() {
        if (::ptyProcess.isInitialized && isAlive()) {
            ptyProcess.destroy()
        }
        job.cancel()
    }
    
    override fun isAlive(): Boolean {
        return if (::ptyProcess.isInitialized) {
            ptyProcess.isAlive
        } else {
            false
        }
    }
    
    override fun getExitCode(): Int? {
        return if (::ptyProcess.isInitialized && !isAlive()) {
            ptyProcess.exitValue()
        } else {
            null
        }
    }
    
    /**
     * 启动PTY进程
     */
    override fun start() {
        try {
            // 构建命令和环境变量 - 根据ShellType配置选择不同的命令
            val command = when (ptyConfig.shellType) {
                ShellType.AUTO -> detectShellCommand(ptyConfig.command.value)
                ShellType.UNIX -> arrayOf("sh", "-c", ptyConfig.command.value)
                ShellType.WINDOWS_CMD -> arrayOf("cmd", "/c", ptyConfig.command.value)
                ShellType.WINDOWS_POWERSHELL -> arrayOf("powershell", "-Command", ptyConfig.command.value)
                ShellType.CUSTOM -> detectCustomShellCommand(ptyConfig.command.value)
                ShellType.DIRECT -> parseDirectCommand(ptyConfig.command.value)
            }
            val environment = ptyConfig.environment
            
            // 创建PTY进程
            ptyProcess = PtyProcessBuilder()
                .setCommand(command)
                .setEnvironment(environment)
                .setDirectory(ptyConfig.workingDirectory)
                .setInitialColumns(ptyConfig.size.columns)
                .setInitialRows(ptyConfig.size.rows)
                .start()
            
            // 获取输入输出流
            outputStream = ptyProcess.outputStream
            inputStream = ptyProcess.inputStream
            
            // 启动输出读取协程
            launch {
                readProcessOutput()
            }
            
        } catch (e: Exception) {
            throw RuntimeException("Failed to start PTY process", e)
        }
    }
    
    /**
     * 读取进程输出
     */
    private suspend fun readProcessOutput() {
        val buffer = ByteArray(1024)
        while (isActive && isAlive()) {
            try {
                val bytesRead = inputStream.read(buffer)
                if (bytesRead > 0) {
                    val output = String(buffer, 0, bytesRead, StandardCharsets.UTF_8)
                    
                    synchronized(outputBuffer) {
                        outputBuffer.append(output)
                    }
                    
                    // 发送到输出通道
                    outputChannel.send(output)
                    
                    // 输出已缓存到缓冲区，由领域层负责事件发布
                } else if (bytesRead == -1) {
                    // 流结束
                    break
                }
            } catch (e: Exception) {
                if (isActive) {
                    // 只有在协程仍然活跃时才重新抛出异常
                    throw e
                }
                break
            }
        }
        
        // 关闭输出通道
        outputChannel.close()
    }
    
    // 事件发布由领域层负责，基础设施层只负责进程管理
    
    /**
     * 监听输出事件
     */
    suspend fun onOutput(block: suspend (String) -> Unit) {
        outputChannel.consumeEach { output ->
            block(output)
        }
    }
    
    /**
     * 等待进程结束
     */
    suspend fun waitFor(): Int {
        return withContext(Dispatchers.IO) {
            ptyProcess.waitFor()
        }
    }
    
    /**
     * 自动检测shell命令
     */
    private fun detectShellCommand(command: String): Array<String> {
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        
        // 检查命令是否已经是完整的可执行文件路径
        if (command.contains(File.separator) || (isWindows && command.contains("\\"))) {
            return parseDirectCommand(command)
        }
        
        // 检查命令是否包含shell特定的语法
        val hasShellSyntax = command.contains("&&") || command.contains("||") || 
                            command.contains("|") || command.contains(";") ||
                            command.contains("$") || command.contains("`")
        
        return if (isWindows) {
            // 在Windows上，优先使用配置的自定义shell路径
            if (ptyConfig.customShellPath.isNotBlank() && File(ptyConfig.customShellPath).exists()) {
                // 如果配置了自定义shell路径且存在，使用该shell
                arrayOf(ptyConfig.customShellPath, "-c", command)
            } else if (hasShellSyntax || command.trim().startsWith("echo")) {
                // 如果没有配置自定义shell路径，使用cmd
                arrayOf("cmd", "/c", command)
            } else {
                // 尝试直接执行
                parseDirectCommand(command)
            }
        } else {
            if (hasShellSyntax) {
                arrayOf("sh", "-c", command)
            } else {
                // 尝试直接执行
                parseDirectCommand(command)
            }
        }
    }
    

    
    /**
     * 解析直接执行的命令
     */
    private fun parseDirectCommand(command: String): Array<String> {
        // 简单的命令分割，支持带引号的参数
        val args = mutableListOf<String>()
        var currentArg = StringBuilder()
        var inQuotes = false
        var quoteChar: Char? = null
        
        for (char in command) {
            when {
                (char == '"' || char == '\'') && !inQuotes -> {
                    inQuotes = true
                    quoteChar = char
                }
                char == quoteChar && inQuotes -> {
                    inQuotes = false
                    quoteChar = null
                }
                char.isWhitespace() && !inQuotes -> {
                    if (currentArg.isNotEmpty()) {
                        args.add(currentArg.toString())
                        currentArg = StringBuilder()
                    }
                }
                else -> {
                    currentArg.append(char)
                }
            }
        }
        
        if (currentArg.isNotEmpty()) {
            args.add(currentArg.toString())
        }
        
        return if (args.isEmpty()) arrayOf(command) else args.toTypedArray()
    }
    
    /**
     * 检测自定义shell命令
     * 使用配置的自定义shell路径
     */
    private fun detectCustomShellCommand(command: String): Array<String> {
        // 使用配置的自定义shell路径
        if (ptyConfig.customShellPath.isNotBlank()) {
            val customShellFile = File(ptyConfig.customShellPath)
            if (customShellFile.exists()) {
                return arrayOf(ptyConfig.customShellPath, "-c", command)
            }
        }
        
        // 如果配置路径不存在，回退到默认的bash
        return arrayOf("bash", "-c", command)
    }
}
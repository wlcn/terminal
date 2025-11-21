package org.now.terminal.session.infrastructure.process

import com.pty4j.PtyProcess
import com.pty4j.PtyProcessBuilder
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import org.now.terminal.session.domain.services.Process
import org.now.terminal.session.domain.valueobjects.PtyConfiguration
import org.now.terminal.session.domain.valueobjects.TerminalSize
import org.now.terminal.shared.valueobjects.SessionId
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
            // 构建命令和环境变量
            val command = arrayOf("sh", "-c", ptyConfig.command.value)
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
}
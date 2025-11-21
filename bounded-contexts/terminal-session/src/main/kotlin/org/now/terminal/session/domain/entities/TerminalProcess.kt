package org.now.terminal.session.domain.entities

import org.now.terminal.session.domain.valueobjects.TerminalCommand
import org.now.terminal.session.domain.valueobjects.TerminalSize

/**
 * 终端进程实体
 * 表示一个正在运行的终端进程
 */
class TerminalProcess(
    val processId: ProcessId,
    private var configuration: ProcessConfiguration,
    private var status: ProcessStatus = ProcessStatus.RUNNING
) {
    private val outputBuffer = OutputBuffer()
    
    /**
     * 执行终端命令
     */
    fun execute(command: TerminalCommand) {
        require(status == ProcessStatus.RUNNING) { "Process is not running" }
        
        // 模拟命令执行
        val output = "Command executed: ${command.value}"
        outputBuffer.append(output)
    }
    
    /**
     * 调整终端尺寸
     */
    fun resize(newSize: TerminalSize) {
        configuration = configuration.copy(size = newSize)
        // 通知底层进程调整尺寸
    }
    
    /**
     * 终止进程
     */
    fun terminate() {
        status = ProcessStatus.TERMINATED
        // 清理资源
    }
    
    /**
     * 检查进程是否存活
     */
    val isAlive: Boolean
        get() = status == ProcessStatus.RUNNING
    
    /**
     * 获取进程输出
     */
    fun getOutput(): String = outputBuffer.getContent()
    
    companion object {
        /**
         * 创建新的终端进程
         */
        fun create(configuration: ProcessConfiguration): TerminalProcess {
            val processId = ProcessId.generate()
            return TerminalProcess(processId, configuration)
        }
    }
}

/**
 * 进程ID值对象
 */
@JvmInline
value class ProcessId(val value: Long) {
    companion object {
        private var nextId: Long = 1L
        
        /**
         * 生成新的进程ID
         */
        fun generate(): ProcessId = ProcessId(nextId++)
    }
}

/**
 * 进程状态枚举
 */
enum class ProcessStatus {
    RUNNING,    // 运行中
    TERMINATED, // 已终止
    ERROR       // 错误
}

/**
 * 进程配置数据类
 */
data class ProcessConfiguration(
    val size: TerminalSize,
    val shell: String,
    val environment: Map<String, String>,
    val workingDirectory: String
)

/**
 * 输出缓冲区
 */
class OutputBuffer {
    private val content = StringBuilder()
    
    fun append(output: String) {
        content.append(output).append("\n")
    }
    
    fun getContent(): String = content.toString()
    
    fun clear() {
        content.clear()
    }
}
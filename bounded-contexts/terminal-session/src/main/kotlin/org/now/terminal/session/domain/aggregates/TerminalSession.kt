package org.now.terminal.session.domain.aggregates

import org.now.terminal.session.domain.entities.TerminalProcess
import org.now.terminal.session.domain.valueobjects.TerminalCommand
import org.now.terminal.session.domain.valueobjects.TerminalSize
import org.now.terminal.shared.valueobjects.SessionId
import org.now.terminal.shared.valueobjects.UserId
import java.time.Instant

/**
 * 终端会话聚合根
 * 管理终端会话的完整生命周期和业务规则
 */
class TerminalSession(
    val sessionId: SessionId,
    val userId: UserId,
    private var configuration: PtyConfiguration,
    private var process: TerminalProcess? = null
) {
    private val outputBuffer = OutputBuffer()
    private val domainEvents = mutableListOf<DomainEvent>()
    
    /**
     * 创建终端进程
     */
    fun createProcess(): TerminalProcess {
        require(process == null) { "Process already exists" }
        
        val newProcess = TerminalProcess.create(configuration)
        process = newProcess
        
        registerEvent(SessionCreatedEvent(sessionId, userId, Instant.now()))
        return newProcess
    }
    
    /**
     * 处理终端输入
     */
    fun handleInput(command: TerminalCommand) {
        val currentProcess = process ?: throw IllegalStateException("No active process")
        currentProcess.execute(command)
        
        registerEvent(TerminalInputProcessedEvent(sessionId, command, Instant.now()))
    }
    
    /**
     * 调整终端尺寸
     */
    fun resize(newSize: TerminalSize) {
        configuration = configuration.copy(size = newSize)
        process?.resize(newSize)
        
        registerEvent(TerminalResizedEvent(sessionId, newSize, Instant.now()))
    }
    
    /**
     * 终止会话
     */
    fun terminate() {
        process?.terminate()
        process = null
        
        registerEvent(SessionTerminatedEvent(sessionId, TerminationReason.USER_REQUEST, Instant.now()))
    }
    
    /**
     * 获取会话状态
     */
    fun getStatus(): SessionStatus = when {
        process == null -> SessionStatus.CREATED
        process?.isAlive == true -> SessionStatus.ACTIVE
        else -> SessionStatus.TERMINATED
    }
    
    private fun registerEvent(event: DomainEvent) {
        domainEvents.add(event)
    }
    
    /**
     * 获取并清空领域事件
     */
    fun getDomainEvents(): List<DomainEvent> = domainEvents.toList().also { domainEvents.clear() }
}

/**
 * PTY配置数据类
 */
data class PtyConfiguration(
    val size: TerminalSize,
    val shell: String = "/bin/bash",
    val environment: Map<String, String> = emptyMap(),
    val workingDirectory: String = System.getProperty("user.home")
)

/**
 * 会话状态枚举
 */
enum class SessionStatus {
    CREATED,    // 已创建
    ACTIVE,     // 活跃
    TERMINATED  // 已终止
}

/**
 * 终止原因枚举
 */
enum class TerminationReason {
    USER_REQUEST,   // 用户请求
    TIMEOUT,        // 超时
    ERROR,          // 错误
    SYSTEM_SHUTDOWN // 系统关闭
}
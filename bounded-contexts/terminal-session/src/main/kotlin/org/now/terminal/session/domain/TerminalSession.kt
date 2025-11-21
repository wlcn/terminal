package org.now.terminal.session.domain

import org.now.terminal.shared.events.Event
import org.now.terminal.shared.events.EventHelper
import org.now.terminal.shared.valueobjects.EventId
import org.now.terminal.shared.valueobjects.SessionId
import org.now.terminal.shared.valueobjects.UserId
import org.now.terminal.session.domain.valueobjects.OutputBuffer
import org.now.terminal.session.domain.valueobjects.PtyConfiguration
import org.now.terminal.session.domain.valueobjects.TerminalCommand
import org.now.terminal.session.domain.valueobjects.TerminalSize
import java.time.Instant

/**
 * 终端会话聚合根
 * 负责管理终端会话的完整生命周期
 */
class TerminalSession(
    val sessionId: SessionId,
    val userId: UserId,
    private val ptyConfig: PtyConfiguration,
    private val eventPublisher: DomainEventPublisher
) {
    private var process: Process? = null
    private val outputBuffer = OutputBuffer()
    private var status: SessionStatus = SessionStatus.CREATED
    private val createdAt: Instant = Instant.now()
    private var terminatedAt: Instant? = null
    private var exitCode: Int? = null
    
    /**
     * 启动会话
     */
    fun start() {
        require(status == SessionStatus.CREATED) { "Session must be in CREATED state" }
        
        // 创建并启动进程
        process = createProcessAdapter()
        status = SessionStatus.RUNNING
        
        // 发布会话创建事件
        eventPublisher.publish(SessionCreatedEvent(
            eventId = EventId.generate(),
            sessionId = sessionId,
            userId = userId,
            configuration = ptyConfig,
            createdAt = createdAt
        ))
    }
    
    /**
     * 处理终端输入
     */
    fun handleInput(input: String) {
        require(status == SessionStatus.RUNNING) { "Session must be in RUNNING state" }
        
        val currentProcess = process ?: throw IllegalStateException("No active process")
        currentProcess.writeInput(input)
        
        // 发布输入处理事件
        eventPublisher.publish(TerminalInputProcessedEvent(
            eventId = EventId.generate(),
            sessionId = sessionId,
            command = TerminalCommand.fromString(input),
            processedAt = Instant.now()
        ))
    }
    
    /**
     * 调整终端尺寸
     */
    fun resize(newSize: TerminalSize) {
        require(status == SessionStatus.RUNNING) { "Session must be in RUNNING state" }
        
        process?.resize(newSize)
        
        // 发布尺寸调整事件
        eventPublisher.publish(TerminalResizedEvent(
            eventId = EventId.generate(),
            sessionId = sessionId,
            newSize = newSize,
            resizedAt = Instant.now()
        ))
    }
    
    /**
     * 终止会话
     */
    fun terminate(reason: TerminationReason) {
        if (status == SessionStatus.TERMINATED) return
        
        process?.terminate()
        status = SessionStatus.TERMINATED
        terminatedAt = Instant.now()
        exitCode = process?.getExitCode()
        
        // 发布会话终止事件
        eventPublisher.publish(SessionTerminatedEvent(
            eventId = EventId.generate(),
            sessionId = sessionId,
            reason = reason,
            exitCode = exitCode,
            terminatedAt = terminatedAt!!
        ))
    }
    
    /**
     * 读取输出
     */
    fun readOutput(): String {
        require(status == SessionStatus.RUNNING) { "Session must be in RUNNING state" }
        
        val output = process?.readOutput() ?: ""
        outputBuffer.append(output)
        
        if (output.isNotEmpty()) {
            // 发布输出事件
            eventPublisher.publish(TerminalOutputEvent(
                eventId = EventId.generate(),
                sessionId = sessionId,
                output = output,
                outputAt = Instant.now()
            ))
        }
        
        return output
    }
    
    /**
     * 检查会话是否存活
     */
    fun isAlive(): Boolean = status == SessionStatus.RUNNING && process?.isAlive() == true
    
    /**
     * 获取会话状态
     */
    fun getStatus(): SessionStatus = status
    
    /**
     * 获取输出缓冲区内容
     */
    fun getOutput(): String = outputBuffer.getContent()
    
    /**
     * 获取进程退出码
     */
    fun getExitCode(): Int? = exitCode
    
    /**
     * 获取配置
     */
    fun getConfiguration(): PtyConfiguration = ptyConfig
    
    /**
     * 创建进程适配器（工厂方法）
     */
    private fun createProcessAdapter(): Process {
        // 这里使用基础设施层的Pty4jProcessAdapter
        return Pty4jProcessAdapter(ptyConfig).also { it.start() }
    }
}

/**
 * 会话状态枚举
 */
enum class SessionStatus {
    CREATED,    // 已创建但未启动
    RUNNING,    // 正在运行
    TERMINATED  // 已终止
}
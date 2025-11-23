package org.now.terminal.session.domain.entities

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.Dispatchers
import org.now.terminal.shared.events.Event
import org.now.terminal.shared.events.EventHelper
import org.now.terminal.shared.valueobjects.SessionId
import org.now.terminal.shared.valueobjects.UserId
import org.now.terminal.session.domain.events.SessionCreatedEvent
import org.now.terminal.session.domain.events.SessionTerminatedEvent
import org.now.terminal.session.domain.events.TerminalInputProcessedEvent
import org.now.terminal.session.domain.events.TerminalOutputEvent
import org.now.terminal.session.domain.events.TerminalResizedEvent
import org.now.terminal.session.domain.services.Process
import org.now.terminal.session.domain.services.ProcessFactory
import org.now.terminal.session.domain.valueobjects.OutputBuffer
import org.now.terminal.session.domain.valueobjects.PtyConfiguration
import org.now.terminal.session.domain.valueobjects.TerminalSize
import org.now.terminal.session.domain.valueobjects.TerminationReason
import java.time.Instant

/**
 * 终端会话聚合根
 * 负责管理终端会话的完整生命周期
 */
class TerminalSession(
    val sessionId: SessionId,
    val userId: UserId,
    val ptyConfig: PtyConfiguration,
    private val processFactory: ProcessFactory
) {
    private var process: Process? = null
    private val outputBuffer = OutputBuffer()
    private var status: SessionStatus = SessionStatus.CREATED
    private val createdAt: Instant = Instant.now()
    private var terminatedAt: Instant? = null
    private var exitCode: Int? = null
    private val domainEvents = mutableListOf<Event>()
    
    /**
     * 启动会话
     */
    fun start() {
        require(status == SessionStatus.CREATED) { "Session must be in CREATED state" }
        
        // 创建并启动进程
        process = createProcess()
        status = SessionStatus.RUNNING
        
        // 启动异步输出监听器
        startOutputListener()
        
        // 添加会话创建领域事件
        domainEvents.add(SessionCreatedEvent(
            eventHelper = EventHelper(
                eventType = "SessionCreated",
                aggregateId = sessionId.value,
                aggregateType = "TerminalSession"
            ),
            sessionId = sessionId,
            userId = userId,
            configuration = ptyConfig,
            createdAt = createdAt
        ))
    }
    
    /**
     * 启动输出监听协程
     * 优化：使用Dispatchers.IO避免主线程阻塞，提升响应速度
     * 启动异步监听PTY进程的输出，当有输出时自动发布领域事件
     */
    fun startOutputListener() {
        require(status == SessionStatus.RUNNING) { "Session must be in RUNNING state" }
        
        val currentProcess = process ?: throw IllegalStateException("No active process")
        
        // 启动协程监听PTY进程的输出通道，使用IO调度器提升性能
        GlobalScope.launch(Dispatchers.IO) {
            currentProcess.getOutputChannel().consumeEach { output ->
                if (output.isNotEmpty()) {
                    // 添加到输出缓冲区
                    outputBuffer.append(output)
                    
                    // 立即发布输出领域事件，减少延迟
                    domainEvents.add(TerminalOutputEvent(
                        eventHelper = EventHelper(
                            eventType = "TerminalOutput",
                            aggregateId = sessionId.value,
                            aggregateType = "TerminalSession"
                        ),
                        sessionId = sessionId,
                        output = output,
                        outputAt = Instant.now()
                    ))
                }
            }
        }
    }
    
    /**
     * 处理终端输入
     */
    fun handleInput(input: String) {
        require(status == SessionStatus.RUNNING) { "Session must be in RUNNING state" }
        
        val currentProcess = process ?: throw IllegalStateException("No active process")
        currentProcess.writeInput(input)
        
        // 添加输入处理领域事件
        domainEvents.add(TerminalInputProcessedEvent(
            eventHelper = EventHelper(
                eventType = "TerminalInputProcessed",
                aggregateId = sessionId.value,
                aggregateType = "TerminalSession"
            ),
            sessionId = sessionId,
            input = input,
            processedAt = Instant.now()
        ))
    }
    
    /**
     * 调整终端尺寸
     */
    fun resize(newSize: TerminalSize) {
        require(status == SessionStatus.RUNNING) { "Session must be in RUNNING state" }
        
        process?.resize(newSize)
        
        // 添加尺寸调整领域事件
        domainEvents.add(TerminalResizedEvent(
            eventHelper = EventHelper(
                eventType = "TerminalResized",
                aggregateId = sessionId.value,
                aggregateType = "TerminalSession"
            ),
            sessionId = sessionId,
            columns = newSize.columns,
            rows = newSize.rows,
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
        
        // 添加会话终止领域事件
        domainEvents.add(SessionTerminatedEvent(
            eventHelper = EventHelper(
                eventType = "SessionTerminated",
                aggregateId = sessionId.value,
                aggregateType = "TerminalSession"
            ),
            sessionId = sessionId,
            reason = reason,
            exitCode = exitCode,
            terminatedAt = terminatedAt!!
        ))
    }
    
    /**
     * 读取输出
     * 注意：该方法仅用于同步读取输出，不发布领域事件
     * 领域事件由异步输出监听器负责发布
     */
    fun readOutput(): String {
        require(status == SessionStatus.RUNNING) { "Session must be in RUNNING state" }
        
        val output = process?.readOutput() ?: ""
        outputBuffer.append(output)
        
        // 注意：不再发布领域事件，避免与异步监听器产生双重事件发布
        // 领域事件由startOutputListener()中的异步监听器负责发布
        
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
     * 获取领域事件并清空事件列表
     */
    fun getDomainEvents(): List<Event> {
        val events = domainEvents.toList()
        domainEvents.clear()
        return events
    }
    
    /**
     * 创建进程实例（工厂方法）
     */
    private fun createProcess(): Process {
        // 使用ProcessFactory创建Process实例，实现依赖倒置
        val process = processFactory.createProcess(ptyConfig, sessionId)
        process.start()
        return process
    }
    
    /**
     * 获取会话创建时间
     */
    fun getCreatedAt(): Instant = createdAt
    
    /**
     * 获取会话终止时间
     */
    fun getTerminatedAt(): Instant? = terminatedAt
    
    /**
     * 获取会话持续时间（如果已终止）
     */
    fun getDuration(): java.time.Duration? {
        return terminatedAt?.let { java.time.Duration.between(createdAt, it) }
    }
    
    /**
     * 检查会话是否可被终止
     */
    fun canTerminate(): Boolean = status != SessionStatus.TERMINATED
    
    /**
     * 检查会话是否可接收输入
     */
    fun canReceiveInput(): Boolean = status == SessionStatus.RUNNING
    
    /**
     * 获取会话统计信息
     */
    fun getStatistics(): SessionStatistics {
        return SessionStatistics(
            sessionId = sessionId,
            userId = userId,
            status = status,
            createdAt = createdAt,
            terminatedAt = terminatedAt,
            exitCode = exitCode,
            outputSize = outputBuffer.size()
        )
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

/**
 * 会话统计信息数据类
 */
data class SessionStatistics(
    val sessionId: SessionId,
    val userId: UserId,
    val status: SessionStatus,
    val createdAt: Instant,
    val terminatedAt: Instant?,
    val exitCode: Int?,
    val outputSize: Int
)
package org.now.terminal.session.domain.aggregates

import org.now.terminal.session.domain.entities.TerminalProcess
import org.now.terminal.session.domain.valueobjects.TerminalCommand
import org.now.terminal.session.domain.valueobjects.TerminalSize
import org.now.terminal.shared.valueobjects.SessionId
import org.now.terminal.shared.valueobjects.UserId
import java.time.Instant

/**
 * 会话聚合根（增强版）
 * 使用事件溯源模式管理会话状态
 */
class SessionAggregate(
    val sessionId: SessionId,
    val userId: UserId
) {
    private var currentState: SessionState = SessionState.Created(sessionId, userId, Instant.now())
    private val pendingEvents = mutableListOf<SessionEvent>()
    
    /**
     * 创建终端进程
     */
    fun createProcess(configuration: PtyConfiguration) {
        require(currentState is SessionState.Created) { "Process already exists or session is terminated" }
        
        val process = TerminalProcess.create(configuration)
        val newState = SessionState.Active(
            sessionId = sessionId,
            userId = userId,
            process = process,
            configuration = configuration,
            startedAt = Instant.now()
        )
        
        applyEvent(SessionCreatedEvent(sessionId, userId, configuration, Instant.now()))
        currentState = newState
    }
    
    /**
     * 处理终端输入
     */
    fun handleInput(command: TerminalCommand) {
        val state = currentState as? SessionState.Active ?: throw IllegalStateException("Session is not active")
        
        state.process.execute(command)
        applyEvent(TerminalInputProcessedEvent(sessionId, command, Instant.now()))
    }
    
    /**
     * 调整终端尺寸
     */
    fun resize(newSize: TerminalSize) {
        val state = currentState as? SessionState.Active ?: throw IllegalStateException("Session is not active")
        
        state.process.resize(newSize)
        applyEvent(TerminalResizedEvent(sessionId, newSize, Instant.now()))
    }
    
    /**
     * 终止会话
     */
    fun terminate(reason: TerminationReason) {
        when (val state = currentState) {
            is SessionState.Active -> {
                state.process.terminate()
                currentState = SessionState.Terminated(
                    sessionId = sessionId,
                    userId = userId,
                    terminatedAt = Instant.now(),
                    reason = reason
                )
                applyEvent(SessionTerminatedEvent(sessionId, reason, Instant.now()))
            }
            is SessionState.Created -> {
                currentState = SessionState.Terminated(
                    sessionId = sessionId,
                    userId = userId,
                    terminatedAt = Instant.now(),
                    reason = reason
                )
                applyEvent(SessionTerminatedEvent(sessionId, reason, Instant.now()))
            }
            is SessionState.Terminated -> {
                // 已经终止，无需操作
            }
        }
    }
    
    /**
     * 获取当前状态
     */
    fun getCurrentState(): SessionState = currentState
    
    /**
     * 获取待发布事件
     */
    fun getPendingEvents(): List<SessionEvent> = pendingEvents.toList().also { pendingEvents.clear() }
    
    private fun applyEvent(event: SessionEvent) {
        pendingEvents.add(event)
    }
}

/**
 * 会话状态密封类
 */
sealed class SessionState {
    data class Created(
        val sessionId: SessionId,
        val userId: UserId,
        val createdAt: Instant
    ) : SessionState()
    
    data class Active(
        val sessionId: SessionId,
        val userId: UserId,
        val process: TerminalProcess,
        val configuration: PtyConfiguration,
        val startedAt: Instant
    ) : SessionState()
    
    data class Terminated(
        val sessionId: SessionId,
        val userId: UserId,
        val terminatedAt: Instant,
        val reason: TerminationReason
    ) : SessionState()
}

/**
 * 会话事件密封类
 */
sealed class SessionEvent(
    val eventId: String,
    val occurredAt: Instant
) {
    data class SessionCreated(
        val sessionId: SessionId,
        val userId: UserId,
        val configuration: PtyConfiguration,
        occurredAt: Instant
    ) : SessionEvent(java.util.UUID.randomUUID().toString(), occurredAt)
    
    data class TerminalInputProcessed(
        val sessionId: SessionId,
        val command: TerminalCommand,
        occurredAt: Instant
    ) : SessionEvent(java.util.UUID.randomUUID().toString(), occurredAt)
    
    data class TerminalResized(
        val sessionId: SessionId,
        val newSize: TerminalSize,
        occurredAt: Instant
    ) : SessionEvent(java.util.UUID.randomUUID().toString(), occurredAt)
    
    data class SessionTerminated(
        val sessionId: SessionId,
        val reason: TerminationReason,
        occurredAt: Instant
    ) : SessionEvent(java.util.UUID.randomUUID().toString(), occurredAt)
}
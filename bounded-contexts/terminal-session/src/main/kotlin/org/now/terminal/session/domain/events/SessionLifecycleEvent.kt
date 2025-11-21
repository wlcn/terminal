package org.now.terminal.session.domain.events

import org.now.terminal.shared.valueobjects.EventId
import org.now.terminal.shared.valueobjects.SessionId
import org.now.terminal.shared.valueobjects.UserId
import java.time.Instant

/**
 * 会话生命周期事件
 * 用于终端会话上下文内部的会话状态变更事件
 */
sealed class SessionLifecycleEvent(
    open val eventId: EventId = EventId.generate(),
    open val occurredAt: Instant = Instant.now(),
    open val sessionId: SessionId,
    open val userId: UserId
) {
    /**
     * 会话创建事件
     */
    data class SessionCreated(
        override val sessionId: SessionId,
        override val userId: UserId,
        override val eventId: EventId = EventId.generate(),
        override val occurredAt: Instant = Instant.now(),
        val terminalType: String = "xterm",
        val initialSize: String = "24x80",
        val environment: Map<String, String> = emptyMap()
    ) : SessionLifecycleEvent(sessionId = sessionId, userId = userId)

    /**
     * 会话终止事件
     */
    data class SessionTerminated(
        override val sessionId: SessionId,
        override val userId: UserId,
        override val eventId: EventId = EventId.generate(),
        override val occurredAt: Instant = Instant.now(),
        val reason: TerminationReason,
        val exitCode: Int? = null
    ) : SessionLifecycleEvent(sessionId = sessionId, userId = userId)

    /**
     * 会话活动事件
     */
    data class SessionActive(
        override val sessionId: SessionId,
        override val userId: UserId,
        override val eventId: EventId = EventId.generate(),
        override val occurredAt: Instant = Instant.now(),
        val lastActivityAt: Instant = Instant.now(),
        val commandCount: Int = 0
    ) : SessionLifecycleEvent(sessionId = sessionId, userId = userId)

    /**
     * 会话空闲事件
     */
    data class SessionIdle(
        override val sessionId: SessionId,
        override val userId: UserId,
        override val eventId: EventId = EventId.generate(),
        override val occurredAt: Instant = Instant.now(),
        val idleDurationSeconds: Long
    ) : SessionLifecycleEvent(sessionId = sessionId, userId = userId)

    /**
     * 终止原因枚举
     */
    enum class TerminationReason {
        USER_REQUEST,
        TIMEOUT,
        SYSTEM_SHUTDOWN,
        ERROR,
        RESOURCE_LIMIT
    }

    /**
     * 获取事件类型
     */
    fun getEventType(): String = this::class.simpleName ?: "Unknown"

    /**
     * 检查是否为创建事件
     */
    fun isCreationEvent(): Boolean = this is SessionCreated

    /**
     * 检查是否为终止事件
     */
    fun isTerminationEvent(): Boolean = this is SessionTerminated
}
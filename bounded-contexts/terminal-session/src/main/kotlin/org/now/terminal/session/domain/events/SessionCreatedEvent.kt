package org.now.terminal.session.domain.events

import org.now.terminal.session.domain.aggregates.PtyConfiguration
import org.now.terminal.session.domain.valueobjects.TerminalSize
import org.now.terminal.shared.events.Event
import org.now.terminal.shared.valueobjects.EventId
import org.now.terminal.shared.valueobjects.SessionId
import org.now.terminal.shared.valueobjects.UserId
import java.time.Instant

/**
 * 会话创建领域事件
 * 当新终端会话创建时触发
 */
data class SessionCreatedEvent(
    val sessionId: SessionId,
    val userId: UserId,
    val configuration: PtyConfiguration,
    val eventHelper: DomainEventHelper = DomainEventHelper()
) : Event {
    override val eventHelper: EventHelper get() = EventHelper(
        eventId = eventHelper.eventId,
        occurredAt = eventHelper.occurredAt,
        eventType = "SessionCreatedEvent",
        aggregateId = sessionId.value,
        aggregateType = "Session"
    )
    
    // 保持向后兼容的便捷访问
    val eventId: EventId get() = eventHelper.eventId
    val occurredAt: Instant get() = eventHelper.occurredAt
}

/**
 * 终端输入处理事件
 * 当终端接收到用户输入时触发
 */
data class TerminalInputProcessedEvent(
    val sessionId: SessionId,
    val command: TerminalCommand,
    val eventHelper: DomainEventHelper = DomainEventHelper()
) : Event {
    override val eventHelper: EventHelper get() = EventHelper(
        eventId = eventHelper.eventId,
        occurredAt = eventHelper.occurredAt,
        eventType = "TerminalInputProcessedEvent",
        aggregateId = sessionId.value,
        aggregateType = "Session"
    )
    
    // 保持向后兼容的便捷访问
    val eventId: EventId get() = eventHelper.eventId
    val occurredAt: Instant get() = eventHelper.occurredAt
}

/**
 * 终端调整尺寸事件
 * 当终端尺寸发生变化时触发
 */
data class TerminalResizedEvent(
    val sessionId: SessionId,
    val newSize: TerminalSize,
    val eventHelper: DomainEventHelper = DomainEventHelper()
) : Event {
    override val eventHelper: EventHelper get() = EventHelper(
        eventId = eventHelper.eventId,
        occurredAt = eventHelper.occurredAt,
        eventType = "TerminalResizedEvent",
        aggregateId = sessionId.value,
        aggregateType = "Session"
    )
    
    // 保持向后兼容的便捷访问
    val eventId: EventId get() = eventHelper.eventId
    val occurredAt: Instant get() = eventHelper.occurredAt
}

/**
 * 会话终止事件
 * 当终端会话终止时触发
 */
data class SessionTerminatedEvent(
    val sessionId: SessionId,
    val reason: TerminationReason,
    val eventHelper: DomainEventHelper = DomainEventHelper()
) : Event {
    override val eventHelper: EventHelper get() = EventHelper(
        eventId = eventHelper.eventId,
        occurredAt = eventHelper.occurredAt,
        eventType = "SessionTerminatedEvent",
        aggregateId = sessionId.value,
        aggregateType = "Session"
    )
    
    // 保持向后兼容的便捷访问
    val eventId: EventId get() = eventHelper.eventId
    val occurredAt: Instant get() = eventHelper.occurredAt
}

/**
 * 领域事件助手类（组合方式）
 */
class DomainEventHelper(
    val eventId: org.now.terminal.shared.valueobjects.EventId = org.now.terminal.shared.valueobjects.EventId.generate(),
    val occurredAt: Instant = Instant.now()
)

/**
 * 终端命令值对象（领域事件中使用）
 */
@JvmInline
value class TerminalCommand(val value: String)



/**
 * 终止原因枚举
 */
enum class TerminationReason {
    USER_REQUEST,   // 用户请求
    TIMEOUT,        // 超时
    ERROR,          // 错误
    SYSTEM_SHUTDOWN // 系统关闭
}
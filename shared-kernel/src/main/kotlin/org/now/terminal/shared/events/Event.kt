package org.now.terminal.shared.events

import org.now.terminal.shared.valueobjects.EventId
import java.time.Instant

/**
 * 事件助手类 - 提供事件通用功能（组合方式）
 * 替代继承，使用组合实现事件通用功能
 */
@kotlinx.serialization.Serializable
data class EventHelper(
    @kotlinx.serialization.Contextual
    val eventId: EventId = EventId.generate(),
    @kotlinx.serialization.Contextual
    val occurredAt: Instant = Instant.now(),
    val eventType: String,
    val aggregateId: String? = null,
    val aggregateType: String? = null,
    val version: Int = 1
)

/**
 * 事件接口 - 所有领域事件的契约
 * 采用组合而非继承的设计原则
 */
interface Event {
    val eventHelper: EventHelper
    
    // 提供便捷的属性访问
    val eventId: EventId get() = eventHelper.eventId
    val occurredAt: Instant get() = eventHelper.occurredAt
    val eventType: String get() = eventHelper.eventType
    val aggregateId: String? get() = eventHelper.aggregateId
    val aggregateType: String? get() = eventHelper.aggregateType
    val version: Int get() = eventHelper.version
}

/**
 * 事件处理器接口
 */
interface EventHandler<T : Event> {
    suspend fun handle(event: T)
    fun canHandle(eventType: String): Boolean
}

/**
 * 事件订阅者接口
 */
interface EventSubscriber {
    suspend fun <T : Event> subscribe(eventType: Class<T>, handler: EventHandler<T>)
    suspend fun <T : Event> unsubscribe(eventType: Class<T>, handler: EventHandler<T>)
}
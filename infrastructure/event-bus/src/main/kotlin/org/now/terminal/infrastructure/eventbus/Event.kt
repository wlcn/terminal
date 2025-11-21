package org.now.terminal.infrastructure.eventbus

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * 事件接口 - 所有领域事件的基类
 */
@Serializable
sealed interface Event {
    val eventId: String
    val occurredAt: Instant
    val eventType: String
    val aggregateId: String?
    val aggregateType: String?
    val version: Int
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
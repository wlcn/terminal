package org.now.terminal.infrastructure.eventbus

import kotlinx.serialization.Serializable

/**
 * 事件总线配置属性
 */
@Serializable
data class EventBusProperties(
    val type: EventBusType = EventBusType.IN_MEMORY,
    val bufferSize: Int = 1000,
    val maxRetries: Int = 3,
    val retryDelayMs: Long = 1000,
    val enableMetrics: Boolean = true,
    val enableDeadLetterQueue: Boolean = false,
    val deadLetterQueueCapacity: Int = 1000,
    val enableRetry: Boolean = true,
    val retryBackoffMultiplier: Double = 2.0,
    val maxRetryDelayMs: Long = 60000
)

/**
 * 事件总线类型
 */
@Serializable
enum class EventBusType {
    IN_MEMORY,
    KAFKA,
    RABBITMQ
}

/**
 * 事件总线配置
 */
class EventBusConfig(
    val properties: EventBusProperties = EventBusProperties()
) {
    fun createEventBus(): EventBus {
        return when (properties.type) {
            EventBusType.IN_MEMORY -> EventBusFactory.createInMemoryEventBus(
                config = properties
            )
            EventBusType.KAFKA -> throw UnsupportedOperationException("Kafka event bus not yet implemented")
            EventBusType.RABBITMQ -> throw UnsupportedOperationException("RabbitMQ event bus not yet implemented")
        }
    }
}
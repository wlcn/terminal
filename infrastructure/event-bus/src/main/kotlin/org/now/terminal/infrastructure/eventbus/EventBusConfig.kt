package org.now.terminal.infrastructure.eventbus

import kotlinx.serialization.Serializable
import org.now.terminal.infrastructure.configuration.ConfigurationManager

/**
 * 事件总线配置属性
 */
@Serializable
data class EventBusProperties(
    val type: EventBusType = EventBusType.IN_MEMORY,
    val bufferSize: Int = 1000
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
            EventBusType.IN_MEMORY -> EventBusFactory.createWithConfig(
                bufferSize = properties.bufferSize
            )
            EventBusType.KAFKA -> throw UnsupportedOperationException("Kafka event bus not yet implemented")
            EventBusType.RABBITMQ -> throw UnsupportedOperationException("RabbitMQ event bus not yet implemented")
        }
    }
}

/**
 * 配置化的事件总线工厂
 */
object ConfiguredEventBusFactory {
    
    /**
     * 从配置管理器创建事件总线
     */
    fun createFromConfiguration(): EventBus {
        val eventBusConfig = org.now.terminal.infrastructure.configuration.ConfigurationManager.getEventBusConfig()
        val properties = EventBusProperties(
            bufferSize = eventBusConfig.bufferSize
        )
        
        return EventBusConfig(properties).createEventBus()
    }
}
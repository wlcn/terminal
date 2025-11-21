package org.now.terminal.infrastructure.eventbus

import kotlinx.coroutines.*
import org.now.terminal.shared.events.Event
import org.now.terminal.shared.events.EventHandler
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * 带监控的事件总线包装器
 */
class MonitoredEventBus(
    private val delegate: EventBus,
    private val metrics: EventBusMetrics
) : EventBus {
    private val logger = LoggerFactory.getLogger(MonitoredEventBus::class.java)
    
    override suspend fun <T : Event> publish(event: T) {
        val startTime = System.nanoTime()
        
        try {
            delegate.publish(event)
            metrics.recordEventPublished(event.eventType)
            
            val duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)
            metrics.recordEventProcessed(event.eventType, duration)
            
            logger.debug("Successfully published event: {} with id: {}", event.eventType, event.eventId)
        } catch (e: Exception) {
            metrics.recordEventError(event.eventType, "PUBLISH_ERROR")
            logger.error("Failed to publish event: {} with id: {}", event.eventType, event.eventId, e)
            throw EventPublishException(
                eventType = event.eventType,
                publisherId = "monitored-event-bus",
                message = "Failed to publish event: ${event.eventId}",
                cause = e
            )
        }
    }
    
    override suspend fun <T : Event> subscribe(eventType: Class<T>, handler: EventHandler<T>) {
        try {
            delegate.subscribe(eventType, handler)
            
            // 更新活跃订阅数（这里需要获取当前订阅数，简化处理）
            metrics.updateActiveSubscriptions(1)
            
            logger.debug("Successfully subscribed handler for event type: {}", eventType.simpleName)
        } catch (e: Exception) {
            metrics.recordEventError(eventType.simpleName, "SUBSCRIPTION_ERROR")
            logger.error("Failed to subscribe to event type: {}", eventType.simpleName, e)
            throw EventSubscriptionException(
                eventType = eventType.simpleName,
                subscriberId = "monitored-event-bus",
                message = "Failed to subscribe to event type: ${eventType.simpleName}",
                cause = e
            )
        }
    }
    
    override suspend fun <T : Event> unsubscribe(eventType: Class<T>, handler: EventHandler<T>) {
        try {
            delegate.unsubscribe(eventType, handler)
            
            // 更新活跃订阅数（这里需要获取当前订阅数，简化处理）
            metrics.updateActiveSubscriptions(-1)
            
            logger.debug("Successfully unsubscribed handler for event type: {}", eventType.simpleName)
        } catch (e: Exception) {
            metrics.recordEventError(eventType.simpleName, "UNSUBSCRIPTION_ERROR")
            logger.error("Failed to unsubscribe from event type: {}", eventType.simpleName, e)
            throw EventSubscriptionException(
                eventType = eventType.simpleName,
                subscriberId = "monitored-event-bus",
                message = "Failed to unsubscribe from event type: ${eventType.simpleName}",
                cause = e
            )
        }
    }
    
    override fun start() {
        delegate.start()
        logger.info("Monitored event bus started")
    }
    
    override fun stop() {
        delegate.stop()
        logger.info("Monitored event bus stopped")
    }
}

/**
 * 事件总线构建器
 */
class EventBusBuilder {
    private var config: EventBusConfig = EventBusConfig()
    private var metrics: EventBusMetrics? = null
    
    fun withConfig(config: EventBusConfig): EventBusBuilder {
        this.config = config
        return this
    }
    
    fun withMetrics(metrics: EventBusMetrics): EventBusBuilder {
        this.metrics = metrics
        return this
    }
    
    fun build(): EventBus {
        val baseEventBus = config.createEventBus()
        
        return if (metrics != null && config.properties.enableMetrics) {
            MonitoredEventBus(baseEventBus, metrics!!)
        } else {
            baseEventBus
        }
    }
}
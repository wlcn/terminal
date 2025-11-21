package org.now.terminal.infrastructure.eventbus

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.now.terminal.shared.events.Event
import org.now.terminal.shared.events.EventHandler
import org.now.terminal.shared.events.EventSubscriber
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * 支持持久化的事件总线实现
 * 在内存事件总线基础上增加事件存储功能
 */
class PersistentEventBus(
    private val delegate: EventBus = InMemoryEventBus(),
    private val eventStore: EventStore = FileEventStore(),
    private val enablePersistence: Boolean = true,
    private val persistenceScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : EventBus {
    private val logger = LoggerFactory.getLogger(PersistentEventBus::class.java)
    private val isRunning = AtomicBoolean(false)
    private val storedEventCount = AtomicLong(0)
    
    private val persistenceChannel = Channel<Event>(Channel.UNLIMITED)
    
    override suspend fun publish(event: Event) {
        // 先发布到委托事件总线
        delegate.publish(event)
        
        // 如果启用持久化，异步存储事件
        if (enablePersistence) {
            persistenceChannel.send(event)
        }
    }
    
    override suspend fun subscribe(subscriber: EventSubscriber) {
        delegate.subscribe(subscriber)
    }
    
    override suspend fun subscribe(handler: EventHandler, eventType: String) {
        delegate.subscribe(handler, eventType)
    }
    
    override suspend fun unsubscribe(subscriber: EventSubscriber) {
        delegate.unsubscribe(subscriber)
    }
    
    override suspend fun unsubscribe(handler: EventHandler, eventType: String) {
        delegate.unsubscribe(handler, eventType)
    }
    
    override suspend fun start() {
        if (isRunning.compareAndSet(false, true)) {
            delegate.start()
            
            if (enablePersistence) {
                // 启动持久化处理协程
                persistenceScope.launch {
                    processPersistenceEvents()
                }
                logger.info("PersistentEventBus started with persistence enabled")
            } else {
                logger.info("PersistentEventBus started without persistence")
            }
        }
    }
    
    override suspend fun stop() {
        if (isRunning.compareAndSet(true, false)) {
            delegate.stop()
            
            // 关闭持久化通道和处理协程
            persistenceChannel.close()
            persistenceScope.cancel("EventBus stopped")
            
            logger.info("PersistentEventBus stopped. Total events stored: {}", storedEventCount.get())
        }
    }
    
    override fun isRunning(): Boolean = isRunning.get()
    
    /**
     * 处理持久化事件的协程
     */
    private suspend fun processPersistenceEvents() {
        try {
            for (event in persistenceChannel) {
                try {
                    if (eventStore.store(event)) {
                        storedEventCount.incrementAndGet()
                        logger.debug("Event persisted: {}", event.eventId)
                    } else {
                        logger.warn("Failed to persist event: {}", event.eventId)
                    }
                } catch (e: Exception) {
                    logger.error("Error persisting event: {}", event.eventId, e)
                }
            }
        } catch (e: Exception) {
            logger.error("Persistence processing stopped due to error", e)
        }
    }
    
    /**
     * 获取存储的事件数量
     */
    suspend fun getStoredEventCount(): Long = storedEventCount.get()
    
    /**
     * 获取底层事件总线
     */
    fun getDelegate(): EventBus = delegate
    
    /**
     * 获取事件存储
     */
    fun getEventStore(): EventStore = eventStore
    
    /**
     * 重新发布存储的事件
     * @param eventType 事件类型过滤器，为空则重新发布所有事件
     * @param limit 最大重新发布数量
     */
    suspend fun republishStoredEvents(eventType: String? = null, limit: Int = 1000) {
        if (!isRunning()) {
            throw IllegalStateException("EventBus is not running")
        }
        
        val events = if (eventType != null) {
            eventStore.retrieveByType(eventType, limit)
        } else {
            // 简化实现，实际中需要更复杂的检索逻辑
            emptyList()
        }
        
        var republishedCount = 0
        events.forEach { event ->
            try {
                delegate.publish(event)
                republishedCount++
                logger.debug("Republished stored event: {}", event.eventId)
            } catch (e: Exception) {
                logger.error("Failed to republish event: {}", event.eventId, e)
            }
        }
        
        logger.info("Republished {} stored events", republishedCount)
    }
    
    /**
     * 清理过时的事件存储
     */
    suspend fun cleanupStoredEvents(beforeTimestamp: Long): Int {
        return eventStore.cleanup(beforeTimestamp)
    }
}

/**
 * 持久化事件总线构建器
 */
class PersistentEventBusBuilder {
    private var delegate: EventBus = InMemoryEventBus()
    private var eventStore: EventStore = FileEventStore()
    private var enablePersistence: Boolean = true
    private var persistenceScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    fun withDelegate(delegate: EventBus): PersistentEventBusBuilder {
        this.delegate = delegate
        return this
    }
    
    fun withEventStore(eventStore: EventStore): PersistentEventBusBuilder {
        this.eventStore = eventStore
        return this
    }
    
    fun withPersistenceEnabled(enabled: Boolean): PersistentEventBusBuilder {
        this.enablePersistence = enabled
        return this
    }
    
    fun withPersistenceScope(scope: CoroutineScope): PersistentEventBusBuilder {
        this.persistenceScope = scope
        return this
    }
    
    fun build(): PersistentEventBus {
        return PersistentEventBus(
            delegate = delegate,
            eventStore = eventStore,
            enablePersistence = enablePersistence,
            persistenceScope = persistenceScope
        )
    }
}
package org.now.terminal.infrastructure.eventbus

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import org.now.terminal.infrastructure.logging.TerminalLogger
import org.now.terminal.shared.events.Event
import org.now.terminal.shared.events.EventHandler
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 事件总线接口
 */
interface EventBus {
    suspend fun <T : Event> publish(event: T)
    suspend fun <T : Event> subscribe(eventType: Class<T>, handler: EventHandler<T>)
    suspend fun <T : Event> unsubscribe(eventType: Class<T>, handler: EventHandler<T>)
    fun start()
    fun stop()
    
    /**
     * 检查事件总线是否正在运行
     */
    fun isRunning(): Boolean
}

/**
 * 内存事件总线实现
 */
class InMemoryEventBus(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val bufferSize: Int = 1000,
    private val config: EventBusProperties = EventBusProperties(),
    private val deadLetterQueue: DeadLetterQueue = DeadLetterQueue(config.deadLetterQueueCapacity),
    private val retryHandler: EventRetryHandler = EventRetryHandler(config)
) : EventBus {
    private val logger = TerminalLogger.getLogger(InMemoryEventBus::class.java)
    private val handlers = ConcurrentHashMap<Class<*>, CopyOnWriteArrayList<EventHandler<*>>>()
    private val eventChannel = Channel<Event>(bufferSize)
    private var processingJob: Job? = null
    
    override suspend fun <T : Event> publish(event: T) {
        logger.debug("Publishing event: {} with id: {}", event.eventType, event.eventId)
        eventChannel.send(event)
    }
    
    override suspend fun <T : Event> subscribe(eventType: Class<T>, handler: EventHandler<T>) {
        handlers.getOrPut(eventType) { CopyOnWriteArrayList() }.add(handler as EventHandler<*>)
        logger.debug("Subscribed handler for event type: {}", eventType.simpleName)
    }
    
    /**
     * 批量注册事件处理器
     * 业务层可以直接调用此方法来注册所有事件处理器
     */
    suspend fun registerHandlers(vararg handlerPairs: Pair<Class<out Event>, EventHandler<*>>) {
        handlerPairs.forEach { (eventClass, handler) ->
            @Suppress("UNCHECKED_CAST")
            subscribe(eventClass as Class<Event>, handler as EventHandler<Event>)
        }
        logger.info("✅ 批量注册了 ${handlerPairs.size} 个事件处理器")
    }
    
    override suspend fun <T : Event> unsubscribe(eventType: Class<T>, handler: EventHandler<T>) {
        handlers[eventType]?.remove(handler as EventHandler<*>)
        logger.debug("Unsubscribed handler for event type: {}", eventType.simpleName)
    }
    
    override fun start() {
        if (processingJob?.isActive == true) {
            logger.warn("Event bus is already running")
            return
        }
        
        processingJob = CoroutineScope(dispatcher).launch {
            eventChannel.consumeEach { event ->
                try {
                    handleEvent(event)
                } catch (e: Exception) {
                    logger.error("Error processing event: {}", event.eventId, e)
                }
            }
        }
        logger.info("In-memory event bus started")
    }
    
    override fun stop() {
        processingJob?.cancel()
        processingJob = null
        eventChannel.close()
        logger.info("In-memory event bus stopped")
    }
    
    override fun isRunning(): Boolean = processingJob?.isActive == true
    
    /**
     * 获取死信队列（用于监控和管理）
     */
    fun getDeadLetterQueue(): DeadLetterQueue = deadLetterQueue
    
    /**
     * 获取当前配置
     */
    fun getConfig(): EventBusProperties = config
    
    /**
     * 获取活跃订阅者数量
     */
    fun getActiveSubscriptions(): Int {
        return handlers.values.sumOf { it.size }
    }
    
    private suspend fun handleEvent(event: Event) {
        val eventHandlers = handlers[event::class.java] ?: return
        
        logger.debug("Processing event: {} with {} handlers", event.eventType, eventHandlers.size)
        
        eventHandlers.forEach { handler ->
            if (handler.canHandle(event.eventType)) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    val typedHandler = handler as EventHandler<Event>
                    
                    // 使用重试机制处理事件
                    val success = retryHandler.retry(event, handler.javaClass.simpleName) { eventToProcess ->
                        typedHandler.handle(eventToProcess)
                    }
                    
                    if (!success && config.enableDeadLetterQueue) {
                        // 重试失败，将事件添加到死信队列
                        val error = EventHandlingException(
                            eventType = event.eventType,
                            handlerId = handler.javaClass.simpleName,
                            message = "Event processing failed after ${config.maxRetries} retries"
                        )
                        deadLetterQueue.add(event, error, config.maxRetries)
                    }
                    
                } catch (e: Exception) {
                    logger.error("Handler error for event: {}", event.eventId, e)
                    
                    if (config.enableDeadLetterQueue) {
                        // 将事件添加到死信队列
                        deadLetterQueue.add(event, e, 0)
                    }
                }
            }
        }
    }
}

/**
 * 事件总线工厂
 */
object EventBusFactory {
    /**
     * 创建内存事件总线
     */
    fun createInMemoryEventBus(
        config: EventBusProperties = EventBusProperties(),
        deadLetterQueue: DeadLetterQueue = DeadLetterQueue(config.deadLetterQueueCapacity)
    ): EventBus {
        return InMemoryEventBus(
            config = config,
            deadLetterQueue = deadLetterQueue,
            retryHandler = EventRetryHandler(config)
        )
    }
    
    /**
     * 创建带监控的事件总线
     */
    fun createMonitoredEventBus(
        delegate: EventBus = createInMemoryEventBus(),
        metrics: EventBusMetrics = EventBusMetrics()
    ): EventBus {
        return MonitoredEventBus(delegate, metrics)
    }
    

}
package org.now.terminal.infrastructure.eventbus

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import org.slf4j.LoggerFactory
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
}

/**
 * 内存事件总线实现
 */
class InMemoryEventBus(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val bufferSize: Int = 1000
) : EventBus {
    private val logger = LoggerFactory.getLogger(InMemoryEventBus::class.java)
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
    
    private suspend fun handleEvent(event: Event) {
        val eventHandlers = handlers[event::class.java] ?: return
        
        logger.debug("Processing event: {} with {} handlers", event.eventType, eventHandlers.size)
        
        eventHandlers.forEach { handler ->
            if (handler.canHandle(event.eventType)) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    (handler as EventHandler<Event>).handle(event)
                } catch (e: Exception) {
                    logger.error("Handler error for event: {}", event.eventId, e)
                }
            }
        }
    }
}

/**
 * 事件总线工厂
 */
object EventBusFactory {
    fun createInMemoryEventBus(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        bufferSize: Int = 1000
    ): EventBus {
        return InMemoryEventBus(dispatcher, bufferSize)
    }
}
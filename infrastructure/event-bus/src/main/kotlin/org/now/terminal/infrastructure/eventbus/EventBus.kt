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
 * 简化的事件总线接口
 * 提供动态注册事件处理器的能力，移除Ktor DI依赖
 */
interface EventBus {
    suspend fun <T : Event> publish(event: T)
    suspend fun <T : Event> subscribe(eventType: Class<T>, handler: EventHandler<T>)
    suspend fun <T : Event> unsubscribe(eventType: Class<T>, handler: EventHandler<T>)
    fun start()
    fun stop()
    fun isRunning(): Boolean
    fun getRegisteredHandlerCount(): Int
}

/**
 * 轻量级内存事件总线实现
 * 专注于动态注册能力，移除复杂的DI和批量注册功能
 */
class SimpleEventBus(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val bufferSize: Int = 1000
) : EventBus {
    private val logger = TerminalLogger.getLogger(SimpleEventBus::class.java)
    private val handlers = ConcurrentHashMap<Class<*>, CopyOnWriteArrayList<EventHandler<*>>>()
    private val eventChannels = ConcurrentHashMap<Class<*>, Channel<Event>>()
    private val processingJobs = ConcurrentHashMap<Class<*>, Job>()
    private var isRunningFlag = false
    
    override suspend fun <T : Event> publish(event: T) {
        logger.debug("发布事件: {} (ID: {})", event.eventType, event.eventId)
        
        val eventChannel = eventChannels[event::class.java]
        if (eventChannel != null && !eventChannel.isClosedForSend) {
            eventChannel.send(event)
        } else {
            logger.warn("事件类型 {} 没有活跃的处理器，事件将被丢弃", event::class.java.simpleName)
        }
    }
    
    override suspend fun <T : Event> subscribe(eventType: Class<T>, handler: EventHandler<T>) {
        handlers.getOrPut(eventType) { CopyOnWriteArrayList() }.add(handler as EventHandler<*>)
        logger.debug("注册事件处理器: {} -> {}", eventType.simpleName, handler.javaClass.simpleName)
        
        // 只有在事件总线运行时才启动监听协程
        if (isRunning()) {
            startProcessingForEventType(eventType)
        }
    }
    
    override suspend fun <T : Event> unsubscribe(eventType: Class<T>, handler: EventHandler<T>) {
        handlers[eventType]?.remove(handler as EventHandler<*>)
        logger.debug("取消注册事件处理器: {} -> {}", eventType.simpleName, handler.javaClass.simpleName)
        
        // 如果没有更多处理器，停止监听协程
        if (handlers[eventType]?.isEmpty() == true) {
            stopProcessingForEventType(eventType)
        }
    }
    
    override fun start() {
        if (isRunningFlag) {
            logger.warn("事件总线已在运行")
            return
        }
        
        isRunningFlag = true
        
        // 为所有已注册的事件类型启动监听协程
        handlers.keys.forEach { eventType ->
            @Suppress("UNCHECKED_CAST")
            startProcessingForEventType(eventType as Class<Event>)
        }
        
        logger.info("事件总线已启动，当前处理器数量: {}", getRegisteredHandlerCount())
    }
    
    override fun stop() {
        if (!isRunningFlag) {
            logger.warn("事件总线未在运行")
            return
        }
        
        isRunningFlag = false
        
        // 停止所有监听协程
        processingJobs.values.forEach { job ->
            job.cancel()
        }
        processingJobs.clear()
        
        // 关闭所有事件通道
        eventChannels.values.forEach { channel ->
            channel.close()
        }
        eventChannels.clear()
        
        logger.info("事件总线已停止")
    }
    
    override fun isRunning(): Boolean = isRunningFlag
    
    override fun getRegisteredHandlerCount(): Int {
        return handlers.values.sumOf { it.size }
    }
    
    /**
     * 获取活跃订阅数量
     */
    fun getActiveSubscriptions(): Int = getRegisteredHandlerCount()
    
    /**
     * 获取事件总线状态
     */
    fun getStatus(): EventBusStatus {
        return EventBusStatus(
            isActive = isRunning(),
            activeSubscriptions = getActiveSubscriptions(),
            queueSize = 0 // Channel没有size属性，暂时返回0
        )
    }
    
    /**
     * 为特定事件类型启动监听协程
     */
    private fun <T : Event> startProcessingForEventType(eventType: Class<T>) {
        // 如果已经有活跃的监听协程，直接返回
        val existingJob = processingJobs[eventType]
        if (existingJob != null && existingJob.isActive) {
            return // 监听协程已经在运行
        }
        
        // 如果协程存在但不活跃，清理掉
        if (existingJob != null) {
            processingJobs.remove(eventType)
            eventChannels.remove(eventType)?.close()
        }
        
        // 只有在事件总线已启动时才创建新的监听协程
        if (isRunning()) {
            val eventChannel = Channel<Event>(bufferSize)
            eventChannels[eventType] = eventChannel
            
            val job = CoroutineScope(dispatcher).launch {
                eventChannel.consumeEach { event ->
                    try {
                        handleEventForType(eventType, event)
                    } catch (e: Exception) {
                        logger.error("处理事件失败: {} (ID: {})", event.eventType, event.eventId, e)
                    }
                }
            }
            
            processingJobs[eventType] = job
            logger.debug("为事件类型 {} 启动监听协程", eventType.simpleName)
        }
    }
    
    /**
     * 停止特定事件类型的监听协程
     */
    private fun <T : Event> stopProcessingForEventType(eventType: Class<T>) {
        val job = processingJobs.remove(eventType)
        job?.cancel()
        
        val channel = eventChannels.remove(eventType)
        channel?.close()
        
        logger.debug("为事件类型 {} 停止监听协程", eventType.simpleName)
    }
    
    /**
     * 处理特定事件类型的事件
     */
    private suspend fun <T : Event> handleEventForType(eventType: Class<T>, event: Event) {
        val eventHandlers = handlers[eventType] ?: return
        
        logger.debug("处理事件: {}，处理器数量: {}", event.eventType, eventHandlers.size)
        
        // 直接处理所有已注册的处理器
        eventHandlers.forEach { handler ->
            try {
                @Suppress("UNCHECKED_CAST")
                (handler as EventHandler<Event>).handle(event)
                logger.debug("事件处理成功: {} -> {}", event.eventType, handler.javaClass.simpleName)
            } catch (e: Exception) {
                logger.error("事件处理器失败: {} -> {}", event.eventType, handler.javaClass.simpleName, e)
            }
        }
    }
}

/**
 * 事件总线状态
 */
data class EventBusStatus(
    val isActive: Boolean,
    val activeSubscriptions: Int,
    val queueSize: Int
)

/**
 * 简化的事件总线工厂
 */
object EventBusFactory {
    /**
     * 创建默认事件总线
     */
    fun createDefault(): EventBus = SimpleEventBus()
    
    /**
     * 创建带自定义配置的事件总线
     */
    fun createWithConfig(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        bufferSize: Int = 1000
    ): EventBus = SimpleEventBus(dispatcher, bufferSize)
}
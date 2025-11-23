package org.now.terminal.infrastructure.eventbus

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import org.now.terminal.infrastructure.logging.TerminalLogger
import org.now.terminal.shared.events.Event
import org.now.terminal.shared.events.EventHandler
import org.now.terminal.shared.valueobjects.SessionId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 简化的事件总线接口
 * 提供动态注册事件处理器的能力，支持基于session的事件路由
 */
interface EventBus {
    suspend fun <T : Event> publish(event: T)
    suspend fun <T : Event> subscribe(eventType: Class<T>, handler: EventHandler<T>)
    suspend fun <T : Event> subscribe(eventType: Class<T>, sessionId: String, handler: EventHandler<T>)
    suspend fun <T : Event> unsubscribe(eventType: Class<T>, handler: EventHandler<T>)
    suspend fun <T : Event> unsubscribe(eventType: Class<T>, sessionId: String)
    suspend fun unsubscribeAllForSession(sessionId: String)
    fun start()
    fun stop()
    fun isRunning(): Boolean
    fun getRegisteredHandlerCount(): Int
    fun getSessionHandlerCount(sessionId: String): Int
}

/**
 * 轻量级内存事件总线实现
 * 支持基于session的事件路由，确保会话隔离
 */
class SimpleEventBus(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val bufferSize: Int = 1000
) : EventBus {
    private val logger = TerminalLogger.getLogger(SimpleEventBus::class.java)
    
    // 全局处理器映射（无session限制）
    private val globalHandlers = ConcurrentHashMap<Class<*>, CopyOnWriteArrayList<EventHandler<*>>>()
    
    // 基于session的处理器映射
    private val sessionHandlers = ConcurrentHashMap<String, ConcurrentHashMap<Class<*>, CopyOnWriteArrayList<EventHandler<*>>>>()
    
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
        val handlerList = globalHandlers.getOrPut(eventType) { CopyOnWriteArrayList() }
        
        // 检查处理器是否已经注册，避免重复注册
        if (handlerList.contains(handler)) {
            logger.debug("全局事件处理器已存在，跳过重复注册: {} -> {}", eventType.simpleName, handler.javaClass.simpleName)
            return
        }
        
        handlerList.add(handler as EventHandler<*>)
        logger.debug("注册全局事件处理器: {} -> {}", eventType.simpleName, handler.javaClass.simpleName)
        
        // 只有在事件总线运行时才启动监听协程
        if (isRunning()) {
            startProcessingForEventType(eventType)
        }
    }
    
    override suspend fun <T : Event> subscribe(eventType: Class<T>, sessionId: String, handler: EventHandler<T>) {
        // 获取或创建session的处理器映射
        val sessionHandlerMap = sessionHandlers.getOrPut(sessionId) { ConcurrentHashMap() }
        val handlerList = sessionHandlerMap.getOrPut(eventType) { CopyOnWriteArrayList() }
        
        // 检查处理器是否已经注册，避免重复注册
        if (handlerList.contains(handler)) {
            logger.debug("会话事件处理器已存在，跳过重复注册: {} -> {} (session: {})", 
                eventType.simpleName, handler.javaClass.simpleName, sessionId)
            return
        }
        
        handlerList.add(handler as EventHandler<*>)
        logger.debug("注册会话事件处理器: {} -> {} (session: {})", 
            eventType.simpleName, handler.javaClass.simpleName, sessionId)
        
        // 只有在事件总线运行时才启动监听协程
        if (isRunning()) {
            startProcessingForEventType(eventType)
        }
    }
    
    override suspend fun <T : Event> unsubscribe(eventType: Class<T>, handler: EventHandler<T>) {
        globalHandlers[eventType]?.remove(handler as EventHandler<*>)
        logger.debug("取消注册全局事件处理器: {} -> {}", eventType.simpleName, handler.javaClass.simpleName)
        
        // 如果没有更多处理器，停止监听协程
        if (globalHandlers[eventType]?.isEmpty() == true) {
            stopProcessingForEventType(eventType)
        }
    }
    
    override suspend fun <T : Event> unsubscribe(eventType: Class<T>, sessionId: String) {
        val sessionHandlerMap = sessionHandlers[sessionId]
        val handlerList = sessionHandlerMap?.get(eventType)
        
        if (handlerList != null) {
            // 移除该session下该事件类型的所有处理器
            handlerList.clear()
            sessionHandlerMap.remove(eventType)
            
            logger.debug("取消注册会话事件处理器: {} (session: {})", eventType.simpleName, sessionId)
            
            // 如果session没有更多处理器，清理session映射
            if (sessionHandlerMap.isEmpty()) {
                sessionHandlers.remove(sessionId)
            }
            
            // 检查是否还有全局或其它session的处理器
            val hasOtherHandlers = globalHandlers[eventType]?.isNotEmpty() == true || 
                sessionHandlers.values.any { it[eventType]?.isNotEmpty() == true }
            
            if (!hasOtherHandlers) {
                stopProcessingForEventType(eventType)
            }
        }
    }
    
    override suspend fun unsubscribeAllForSession(sessionId: String) {
        val sessionHandlerMap = sessionHandlers.remove(sessionId)
        
        if (sessionHandlerMap != null) {
            // 检查哪些事件类型需要停止监听协程
            val affectedEventTypes = sessionHandlerMap.keys.filter { eventType ->
                val hasOtherHandlers = globalHandlers[eventType]?.isNotEmpty() == true || 
                    sessionHandlers.values.any { it[eventType]?.isNotEmpty() == true }
                !hasOtherHandlers
            }
            
            // 停止不需要的事件类型监听协程
            affectedEventTypes.forEach { eventType ->
                stopProcessingForEventType(eventType as Class<Event>)
            }
            
            logger.debug("取消注册会话所有事件处理器 (session: {})", sessionId)
        }
    }
    
    override fun start() {
        if (isRunningFlag) {
            logger.warn("事件总线已在运行")
            return
        }
        
        isRunningFlag = true
        
        // 收集所有已注册的事件类型（包括全局和session级别）
        val allEventTypes = mutableSetOf<Class<*>>()
        allEventTypes.addAll(globalHandlers.keys)
        sessionHandlers.values.forEach { sessionMap ->
            allEventTypes.addAll(sessionMap.keys)
        }
        
        // 为所有已注册的事件类型启动监听协程
        allEventTypes.forEach { eventType ->
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
        val globalCount = globalHandlers.values.sumOf { it.size }
        val sessionCount = sessionHandlers.values.sumOf { sessionMap ->
            sessionMap.values.sumOf { it.size }
        }
        return globalCount + sessionCount
    }
    
    override fun getSessionHandlerCount(sessionId: String): Int {
        val sessionHandlerMap = sessionHandlers[sessionId]
        return sessionHandlerMap?.values?.sumOf { it.size } ?: 0
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
     * 从事件中提取sessionId
     * 直接使用Event接口的sessionId属性，无需反射
     */
    private fun extractSessionIdFromEvent(event: Event): String? {
        return event.sessionId?.value
    }
    
    /**
     * 处理特定事件类型的事件，支持基于session的路由
     */
    private suspend fun <T : Event> handleEventForType(eventType: Class<T>, event: Event) {
        // 获取全局处理器
        val globalHandlers = globalHandlers[eventType] ?: emptyList()
        
        // 获取基于session的处理器
        val sessionHandlers = mutableListOf<EventHandler<*>>()
        
        // 如果事件包含sessionId，只处理对应session的处理器
        val sessionId = extractSessionIdFromEvent(event)
        if (sessionId != null) {
            val sessionHandlerMap = this.sessionHandlers[sessionId]
            val handlersForSession = sessionHandlerMap?.get(eventType) ?: emptyList()
            sessionHandlers.addAll(handlersForSession)
        } else {
            // 如果没有sessionId，处理所有session的处理器
            this.sessionHandlers.values.forEach { sessionMap ->
                sessionMap[eventType]?.let { handlers ->
                    sessionHandlers.addAll(handlers)
                }
            }
        }
        
        val allHandlers = globalHandlers + sessionHandlers
        
        if (allHandlers.isEmpty()) {
            logger.debug("没有找到事件处理器: {}", event.eventType)
            return
        }
        
        logger.debug("处理事件: {}，全局处理器: {}，会话处理器: {} (session: {})", 
            event.eventType, globalHandlers.size, sessionHandlers.size, sessionId ?: "none")
        
        // 处理所有匹配的处理器
        allHandlers.forEach { handler ->
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
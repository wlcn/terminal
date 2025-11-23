package org.now.terminal.infrastructure.eventbus

import org.now.terminal.infrastructure.logging.TerminalLogger
import org.now.terminal.shared.events.Event
import org.now.terminal.shared.events.EventHandler
import java.util.concurrent.ConcurrentHashMap

/**
 * 动态事件处理器注册服务
 * 提供便捷的API来动态注册和取消注册事件处理器
 */
class DynamicEventHandlerRegistry(private val eventBus: EventBus) {
    private val logger = TerminalLogger.getLogger(DynamicEventHandlerRegistry::class.java)
    
    // 跟踪已注册的处理器，便于后续取消注册
    private val registeredHandlers = ConcurrentHashMap<String, MutableSet<EventHandler<*>>>()
    
    /**
     * 注册事件处理器
     * @param eventType 事件类型
     * @param handler 事件处理器
     * @param handlerId 处理器标识（可选，用于后续取消注册）
     */
    suspend fun <T : Event> registerHandler(
        eventType: Class<T>,
        handler: EventHandler<T>,
        handlerId: String? = null
    ) {
        eventBus.subscribe(eventType, handler)
        
        val id = handlerId ?: handler.javaClass.simpleName
        registeredHandlers.getOrPut(id) { mutableSetOf() }.add(handler)
        
        logger.debug("动态注册事件处理器: {} -> {}", eventType.simpleName, id)
    }
    
    /**
     * 批量注册事件处理器
     * @param handlers 处理器映射：事件类型 -> 处理器列表
     */
    suspend fun registerHandlers(handlers: Map<Class<out Event>, List<EventHandler<*>>>) {
        handlers.forEach { (eventType, handlerList) ->
            handlerList.forEach { handler ->
                // 使用安全的类型转换方法
                registerHandlerWithTypeCheck(eventType, handler)
            }
        }
        logger.info("批量注册了 ${handlers.values.sumOf { it.size }} 个事件处理器")
    }
    
    /**
     * 安全的类型检查注册方法
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun <T : Event> registerHandlerWithTypeCheck(eventType: Class<T>, handler: EventHandler<*>) {
        val typedHandler = handler as? EventHandler<T>
        if (typedHandler != null) {
            registerHandler(eventType, typedHandler)
        } else {
            logger.warn("事件处理器类型不匹配: {} -> {}", eventType.simpleName, handler.javaClass.simpleName)
        }
    }
    
    /**
     * 取消注册指定标识的所有处理器
     * @param handlerId 处理器标识
     */
    suspend fun unregisterHandlers(handlerId: String) {
        val handlers = registeredHandlers[handlerId] ?: return
        
        handlers.forEach { handler ->
            // 查找处理器对应的事件类型
            val eventType = findEventTypeForHandler(handler)
            if (eventType != null) {
                // 使用安全的取消注册方法
                unsubscribeWithTypeCheck(eventType, handler)
            }
        }
        
        registeredHandlers.remove(handlerId)
        logger.debug("取消注册处理器组: {} ({}个处理器)", handlerId, handlers.size)
    }
    
    /**
     * 安全的类型检查取消注册方法
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun <T : Event> unsubscribeWithTypeCheck(eventType: Class<T>, handler: EventHandler<*>) {
        val typedHandler = handler as? EventHandler<T>
        if (typedHandler != null) {
            eventBus.unsubscribe(eventType, typedHandler)
        } else {
            logger.warn("取消注册时事件处理器类型不匹配: {} -> {}", eventType.simpleName, handler.javaClass.simpleName)
        }
    }
    
    /**
     * 取消注册单个处理器
     * @param eventType 事件类型
     * @param handler 事件处理器
     */
    suspend fun <T : Event> unregisterHandler(eventType: Class<T>, handler: EventHandler<T>) {
        eventBus.unsubscribe(eventType, handler)
        
        // 从注册表中移除
        registeredHandlers.forEach { (id, handlers) ->
            if (handlers.remove(handler)) {
                if (handlers.isEmpty()) {
                    registeredHandlers.remove(id)
                }
            }
        }
        
        logger.debug("取消注册事件处理器: {} -> {}", eventType.simpleName, handler.javaClass.simpleName)
    }
    
    /**
     * 获取所有已注册的处理器标识
     */
    fun getRegisteredHandlerIds(): Set<String> = registeredHandlers.keys.toSet()
    
    /**
     * 获取指定标识的处理器数量
     */
    fun getHandlerCount(handlerId: String): Int = registeredHandlers[handlerId]?.size ?: 0
    
    /**
     * 获取总注册处理器数量
     */
    fun getTotalHandlerCount(): Int = registeredHandlers.values.sumOf { it.size }
    
    /**
     * 清空所有注册的处理器
     */
    suspend fun clearAllHandlers() {
        registeredHandlers.forEach { (id, handlers) ->
            handlers.forEach { handler ->
                val eventType = findEventTypeForHandler(handler)
                if (eventType != null) {
                    // 使用安全的取消注册方法
                    unsubscribeWithTypeCheck(eventType, handler)
                }
            }
        }
        
        registeredHandlers.clear()
        logger.info("清空所有事件处理器，共 ${getTotalHandlerCount()} 个")
    }
    
    /**
     * 检查指定标识的处理器是否存在
     */
    fun hasHandlers(handlerId: String): Boolean = registeredHandlers[handlerId]?.isNotEmpty() ?: false
    
    private fun findEventTypeForHandler(handler: EventHandler<*>): Class<out Event>? {
        // 这是一个简化的实现，实际项目中可能需要更复杂的逻辑
        // 或者让调用方明确指定事件类型
        return Event::class.java
    }
}

/**
 * 动态注册服务的工厂类
 */
object DynamicEventHandlerRegistryFactory {
    /**
     * 创建动态事件处理器注册服务
     */
    fun create(eventBus: EventBus): DynamicEventHandlerRegistry {
        return DynamicEventHandlerRegistry(eventBus)
    }
    
    /**
     * 创建并启动事件总线，返回注册服务
     */
    fun createAndStart(eventBus: EventBus): DynamicEventHandlerRegistry {
        eventBus.start()
        return create(eventBus)
    }
}
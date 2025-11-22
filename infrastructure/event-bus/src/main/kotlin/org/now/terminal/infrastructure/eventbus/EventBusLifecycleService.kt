package org.now.terminal.infrastructure.eventbus

import org.now.terminal.infrastructure.logging.TerminalLogger
import org.now.terminal.shared.events.Event
import org.now.terminal.shared.events.EventHandler

/**
 * 事件总线生命周期服务
 * 负责事件总线的启动、停止和事件处理器的自动注册
 * 
 * 通过构造参数注入的事件处理器集合，在启动时自动注册到事件总线
 */
class EventBusLifecycleService(
    private val eventBus: EventBus,
    private val eventHandlers: List<EventHandler<*>>
) {
    private val logger = TerminalLogger.getLogger(EventBusLifecycleService::class.java)

    /**
     * 启动事件总线并自动注册所有事件处理器
     */
    fun start() {
        eventBus.start()
        logger.info("事件总线已启动")
        
        // 自动注册所有事件处理器
        registerAllEventHandlers()
    }

    /**
     * 停止事件总线
     */
    fun stop() {
        eventBus.stop()
        logger.info("事件总线已停止")
    }

    /**
     * 自动注册所有事件处理器
     * 由于事件处理器已经通过DI框架收集并注入，这里直接注册即可
     */
    private fun registerAllEventHandlers() {
        if (eventHandlers.isEmpty()) {
            logger.warn("未发现任何事件处理器")
            return
        }
        
        logger.info("开始注册 ${eventHandlers.size} 个事件处理器...")
        
        // 每个事件处理器都应该知道它能处理什么事件
        // 这里直接注册所有处理器，由事件总线在运行时根据事件类型进行分发
        eventHandlers.forEach { handler ->
            try {
                // 事件处理器已经通过泛型参数声明了它能处理的事件类型
                // 这里直接注册，事件总线会在运行时根据事件类型进行匹配
                @Suppress("UNCHECKED_CAST")
                val typedHandler = handler as EventHandler<Event>
                
                // 注册到事件总线，事件总线会处理类型匹配
                eventBus.subscribe(Event::class.java, typedHandler)
                logger.debug("成功注册事件处理器: ${handler::class.simpleName}")
            } catch (e: Exception) {
                logger.error("注册事件处理器 ${handler::class.simpleName} 时发生错误", e)
            }
        }
        
        logger.info("✅ 成功注册了 ${eventHandlers.size} 个事件处理器")
    }
}
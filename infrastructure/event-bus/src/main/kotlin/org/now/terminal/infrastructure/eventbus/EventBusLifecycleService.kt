package org.now.terminal.infrastructure.eventbus

import org.koin.core.Koin
import org.koin.core.context.GlobalContext
import org.now.terminal.infrastructure.logging.TerminalLogger
import org.now.terminal.shared.events.Event
import org.now.terminal.shared.events.EventHandler

/**
 * 事件总线生命周期服务
 * 负责事件总线的启动、停止和事件处理器注册
 */
class EventBusLifecycleService {
    
    private val logger = TerminalLogger.getLogger(EventBusLifecycleService::class.java)
    private val eventBus = EventBusFactory.createMonitoredEventBus()
    private val discoveryService = EventHandlerDiscoveryService(getKoin())
    
    /**
     * 启动事件总线
     */
    fun start() {
        try {
            eventBus.start()
            logger.info("✅ Event bus started successfully")
        } catch (e: Exception) {
            logger.error("❌ Failed to start event bus: {}", e.message)
        }
    }
    
    /**
     * 停止事件总线
     */
    fun stop() {
        try {
            eventBus.stop()
            logger.info("✅ Event bus stopped successfully")
        } catch (e: Exception) {
            logger.error("❌ Failed to stop event bus: {}", e.message)
        }
    }
    
    /**
     * 注册事件处理器
     */
    suspend fun registerEventHandlers() {
        try {
            // 发现所有事件处理器
            val handlers = discoveryService.discoverEventHandlers()
            
            if (handlers.isEmpty()) {
                logger.warn("⚠️ 未发现任何事件处理器")
                return
            }
            
            // 注册每个事件处理器
            var registeredCount = 0
            handlers.forEach { handler ->
                val eventClass = discoveryService.getEventClassForHandler(handler)
                if (eventClass != null) {
                    registerEventHandler(eventClass, handler)
                    registeredCount++
                } else {
                    logger.warn("⚠️ 无法确定处理器 ${handler.javaClass.simpleName} 处理的事件类型")
                }
            }
            
            logger.info("✅ 成功注册 $registeredCount 个事件处理器")
            
        } catch (e: Exception) {
            logger.error("❌ 注册事件处理器时发生错误: {}", e.message)
        }
    }
    
    /**
     * 注册单个事件处理器
     */
    private suspend fun <T : Event> registerEventHandler(eventClass: Class<T>, handler: EventHandler<*>) {
        try {
            @Suppress("UNCHECKED_CAST")
            val typedHandler = handler as EventHandler<T>
            
            eventBus.subscribe(eventClass, typedHandler)
            logger.debug("✅ 注册事件处理器: ${handler.javaClass.simpleName} -> ${eventClass.simpleName}")
            
        } catch (e: Exception) {
            logger.error("❌ 注册事件处理器失败: ${handler.javaClass.simpleName} -> ${eventClass.simpleName}, 错误: ${e.message}")
        }
    }
    
    /**
     * 获取Koin实例
     */
    private fun getKoin(): Koin {
        return GlobalContext.get()
    }
}
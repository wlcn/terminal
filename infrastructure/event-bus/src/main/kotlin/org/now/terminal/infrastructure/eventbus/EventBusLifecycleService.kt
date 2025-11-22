package org.now.terminal.infrastructure.eventbus

import org.now.terminal.infrastructure.logging.TerminalLogger

/**
 * 事件总线生命周期服务
 * 负责事件总线的启动、停止和事件处理器注册
 */
class EventBusLifecycleService {
    
    private val logger = TerminalLogger.getLogger(EventBusLifecycleService::class.java)
    private val eventBus = EventBusFactory.createMonitoredEventBus()
    private val discoveryService = EventHandlerDiscoveryService(eventBus)
    
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
     * 初始化事件处理器注册服务
     * 业务层应该直接调用 EventBus.registerHandlers() 方法进行手动注册
     */
    suspend fun initializeEventHandlers() {
        try {
            discoveryService.initialize()
            logger.info("✅ 事件处理器注册服务已初始化")
            logger.info("💡 建议业务层直接调用 EventBus.registerHandlers() 方法进行手动注册")
            logger.info("💡 这样可以避免依赖注入框架的局限性，提供更好的控制性和可维护性")
        } catch (e: Exception) {
            logger.error("❌ 初始化事件处理器注册服务时发生错误: {}", e.message)
        }
    }
}
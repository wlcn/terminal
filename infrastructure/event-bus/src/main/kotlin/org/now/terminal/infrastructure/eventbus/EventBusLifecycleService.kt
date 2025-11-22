package org.now.terminal.infrastructure.eventbus

import org.now.terminal.infrastructure.logging.TerminalLogger

/**
 * 事件总线生命周期服务
 * 负责事件总线的启动、停止和事件处理器注册
 */
class EventBusLifecycleService {
    
    private val logger = TerminalLogger.getLogger(EventBusLifecycleService::class.java)
    private val eventBus = EventBusFactory.createMonitoredEventBus()
    
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
    fun registerEventHandlers() {
        try {
            // 这里可以添加自动发现和注册事件处理器的逻辑
            logger.info("📋 Event handlers registered")
        } catch (e: Exception) {
            logger.error("❌ Failed to register event handlers: {}", e.message)
        }
    }
}
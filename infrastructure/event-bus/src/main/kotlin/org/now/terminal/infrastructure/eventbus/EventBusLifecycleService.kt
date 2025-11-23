package org.now.terminal.infrastructure.eventbus

import org.now.terminal.infrastructure.configuration.ConfigurationManager
import org.now.terminal.infrastructure.logging.TerminalLogger

/**
 * EventBus生命周期管理服务
 * 负责EventBus的启动、停止和事件处理器注册
 */
class EventBusLifecycleService(
    private val eventBus: EventBus,
    private val configurationManager: ConfigurationManager
) {
    
    private val logger = TerminalLogger.getLogger(EventBusLifecycleService::class.java)
    
    /**
     * 初始化EventBus系统
     */
    fun initialize() {
        try {
            // 获取EventBus配置
            val eventBusConfig = configurationManager.getEventBusConfig()
            
            // 启动EventBus
            eventBus.start()
            
            logger.info("✅ EventBus system initialized successfully")
            logger.info("📊 Buffer size: ${eventBusConfig.bufferSize}")
            
        } catch (e: Exception) {
            logger.error("❌ Failed to initialize EventBus system: {}", e.message)
            throw e
        }
    }
    
    /**
     * 停止EventBus系统
     */
    fun stop() {
        try {
            eventBus.stop()
            logger.info("✅ EventBus system stopped successfully")
        } catch (e: Exception) {
            logger.error("❌ Failed to stop EventBus system: {}", e.message)
        }
    }
    
    /**
     * 检查EventBus是否正在运行
     */
    fun isRunning(): Boolean = eventBus.isRunning()
    
    /**
     * 获取EventBus状态信息
     */
    fun getStatus(): EventBusStatus {
        return EventBusStatus(
            isActive = eventBus.isRunning(),
            activeSubscriptions = eventBus.getRegisteredHandlerCount(),
            queueSize = 0 // SimpleEventBus没有队列大小统计
        )
    }
}
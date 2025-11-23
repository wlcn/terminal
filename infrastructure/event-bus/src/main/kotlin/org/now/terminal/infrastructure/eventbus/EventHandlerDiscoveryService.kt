package org.now.terminal.infrastructure.eventbus

import org.now.terminal.infrastructure.logging.TerminalLogger

class EventHandlerDiscoveryService(private val eventBus: EventBus) {
    
    private val logger = TerminalLogger.getLogger(EventHandlerDiscoveryService::class.java)
    
    /**
     * 初始化事件处理器注册
     * 业务层应该直接调用 EventBus 的 registerHandlers 方法进行手动注册
     */
    suspend fun initialize() {
        logger.info("🔧 事件处理器发现服务已初始化")
        logger.info("💡 建议业务层直接调用 EventBus.registerHandlers() 方法进行手动注册")
        logger.info("💡 这样可以避免依赖注入框架的局限性，提供更好的控制性和可维护性")
    }
}
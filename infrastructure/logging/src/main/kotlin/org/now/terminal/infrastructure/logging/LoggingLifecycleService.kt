package org.now.terminal.infrastructure.logging

/**
 * 日志系统生命周期服务
 * 负责日志系统的初始化和配置
 */
class LoggingLifecycleService {
    
    private val logger = TerminalLogger.getLogger(LoggingLifecycleService::class.java)
    
    /**
     * 初始化日志系统
     */
    fun initialize() {
        try {
            TerminalLogger.initialize()
            logger.info("✅ Logging system initialized successfully")
        } catch (e: Exception) {
            logger.error("❌ Failed to initialize logging system: {}", e.message)
        }
    }
}
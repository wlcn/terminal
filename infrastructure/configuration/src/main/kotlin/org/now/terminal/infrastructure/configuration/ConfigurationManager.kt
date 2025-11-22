package org.now.terminal.infrastructure.configuration

import org.now.terminal.infrastructure.logging.TerminalLogger

/**
 * 配置管理器 - 提供全局配置访问和生命周期管理
 */
object ConfigurationManager {
    private val logger = TerminalLogger.getLogger(ConfigurationManager::class.java)
    
    private var currentConfig: AppConfig? = null
    private var isInitialized = false
    
    /**
     * 初始化配置管理器
     * @param configPath 配置文件路径（可选）
     * @param environment 环境名称（可选）
     */
    fun initialize(configPath: String? = null, environment: String? = null) {
        if (isInitialized) {
            logger.warn("ConfigurationManager is already initialized")
            return
        }
        
        try {
            // 加载配置
            val config = ConfigLoader.loadAppConfig(configPath, environment)
            
            // 验证配置
            ConfigLoader.validateConfig(config)
            
            // 设置当前配置
            currentConfig = config
            isInitialized = true
            
            logger.info("ConfigurationManager initialized successfully for environment: {}", 
                config.environment)
            logger.debug("Application: {} v{}", config.name, config.version)
            
        } catch (e: Exception) {
            logger.error("Failed to initialize ConfigurationManager", e)
            throw ConfigurationException("Failed to initialize ConfigurationManager", e)
        }
    }
    
    /**
     * 获取当前配置
     * @throws IllegalStateException 如果配置管理器未初始化
     */
    fun getConfig(): AppConfig {
        if (!isInitialized || currentConfig == null) {
            throw IllegalStateException("ConfigurationManager is not initialized. Call initialize() first.")
        }
        return currentConfig!!
    }
    
    /**
     * 检查配置管理器是否已初始化
     */
    fun isInitialized(): Boolean = isInitialized
    
    /**
     * 重新加载配置
     * @param configPath 新的配置文件路径（可选）
     * @param environment 新的环境名称（可选）
     */
    fun reload(configPath: String? = null, environment: String? = null) {
        try {
            val oldConfig = currentConfig
            val newConfig = ConfigLoader.loadAppConfig(configPath, environment)
            
            ConfigLoader.validateConfig(newConfig)
            
            currentConfig = newConfig
            
            logger.info("Configuration reloaded successfully")
            logger.debug("Environment changed from {} to {}", 
                oldConfig?.environment ?: "unknown", newConfig.environment)
                
        } catch (e: Exception) {
            logger.error("Failed to reload configuration", e)
            throw ConfigurationException("Failed to reload configuration", e)
        }
    }
    
    /**
     * 重置配置管理器（主要用于测试）
     */
    fun reset() {
        currentConfig = null
        isInitialized = false
        logger.info("ConfigurationManager reset")
    }
    
    /**
     * 便捷方法：获取服务器配置
     */
    fun getServerConfig(): ServerConfig = getConfig().server
    
    /**
     * 便捷方法：获取数据库配置
     */
    fun getDatabaseConfig(): DatabaseConfig = getConfig().database
    
    /**
     * 便捷方法：获取事件总线配置
     */
    fun getEventBusConfig(): EventBusConfig = getConfig().eventBus
    
    /**
     * 便捷方法：获取日志配置
     */
    fun getLoggingConfig(): LoggingConfig = getConfig().logging
    
    /**
     * 便捷方法：获取监控配置
     */
    fun getMonitoringConfig(): MonitoringConfig = getConfig().monitoring
    
    /**
     * 便捷方法：获取应用程序名称
     */
    fun getAppName(): String = getConfig().name
    
    /**
     * 便捷方法：获取应用程序版本
     */
    fun getAppVersion(): String = getConfig().version
    
    /**
     * 便捷方法：获取当前环境
     */
    fun getEnvironment(): String = getConfig().environment
    
    /**
     * 检查当前是否为开发环境
     */
    fun isDevelopment(): Boolean = getEnvironment().equals("dev", ignoreCase = true)
    
    /**
     * 检查当前是否为测试环境
     */
    fun isTest(): Boolean = getEnvironment().equals("test", ignoreCase = true)
    
    /**
     * 检查当前是否为生产环境
     */
    fun isProduction(): Boolean = getEnvironment().equals("prod", ignoreCase = true)
    
    /**
     * 获取服务器端口
     */
    fun getServerPort(): Int = getServerConfig().port
    
    /**
     * 获取服务器主机名
     */
    fun getServerHost(): String = getServerConfig().host
    
    /**
     * 获取数据库连接URL
     */
    fun getDatabaseUrl(): String = getDatabaseConfig().url
    
    /**
     * 获取数据库用户名
     */
    fun getDatabaseUsername(): String = getDatabaseConfig().username
    
    /**
     * 获取数据库密码
     */
    fun getDatabasePassword(): String = getDatabaseConfig().password
    
    /**
     * 获取日志级别
     */
    fun getLogLevel(): String = getLoggingConfig().level
    
    /**
     * 检查文件日志是否启用
     */
    fun isFileLoggingEnabled(): Boolean = getLoggingConfig().file.enabled
    
    /**
     * 获取日志文件路径
     */
    fun getLogFilePath(): String = getLoggingConfig().file.path
    
    /**
     * 获取事件总线缓冲区大小
     */
    fun getEventBusBufferSize(): Int = getEventBusConfig().bufferSize
    
    /**
     * 检查事件总线重试机制是否启用
     */
    fun isEventBusRetryEnabled(): Boolean = getEventBusConfig().maxRetries > 0
    
    /**
     * 检查监控是否启用
     */
    fun isMonitoringEnabled(): Boolean = getMonitoringConfig().enabled
    
    /**
     * 检查指标监控是否启用
     */
    fun isMetricsEnabled(): Boolean = getMonitoringConfig().metrics.enabled
    
    /**
     * 检查健康检查是否启用
     */
    fun isHealthCheckEnabled(): Boolean = getMonitoringConfig().health.enabled
    
    /**
     * 获取指标导出间隔（毫秒）
     */
    fun getMetricsExportInterval(): Long = getMonitoringConfig().metrics.exportInterval
    
    /**
     * 获取健康检查间隔（毫秒）
     */
    fun getHealthCheckInterval(): Long = getMonitoringConfig().health.checkInterval
}
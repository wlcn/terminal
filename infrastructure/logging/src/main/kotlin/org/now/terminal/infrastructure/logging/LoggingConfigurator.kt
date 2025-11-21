package org.now.terminal.infrastructure.logging

import org.now.terminal.infrastructure.configuration.ConfigurationManager
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.FileAppender
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy
import ch.qos.logback.core.util.FileSize
import java.nio.file.Paths

/**
 * 日志配置器 - 负责根据配置动态配置日志系统
 */
object LoggingConfigurator {
    
    private val logger = LoggerFactory.getLogger(LoggingConfigurator::class.java)
    
    /**
     * 配置日志系统
     */
    fun configure() {
        try {
            // 确保配置管理器已初始化
            if (!ConfigurationManager.isInitialized()) {
                ConfigurationManager.initialize()
            }
            
            val loggingConfig = ConfigurationManager.getLoggingConfig()
            configureLogback(loggingConfig)
            
            logger.info("Logging system configured successfully. Level: {}", loggingConfig.level)
            
        } catch (e: Exception) {
            logger.error("Failed to configure logging system", e)
            // 使用默认配置继续运行
        }
    }
    
    /**
     * 配置Logback日志系统
     */
    private fun configureLogback(loggingConfig: org.now.terminal.infrastructure.configuration.LoggingConfig) {
        val context = LoggerFactory.getILoggerFactory() as? LoggerContext
        if (context == null) {
            logger.warn("Logback context not available, using default logging configuration")
            return
        }
        
        // 重置日志上下文
        context.reset()
        
        // 配置根日志级别
        val rootLogger = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger
        rootLogger.level = Level.toLevel(loggingConfig.level, Level.INFO)
        
        // 创建控制台输出器
        val consoleAppender = createConsoleAppender(context, loggingConfig)
        rootLogger.addAppender(consoleAppender)
        
        // 如果启用了文件日志，创建文件输出器
        if (loggingConfig.file.enabled) {
            val fileAppender = createFileAppender(context, loggingConfig)
            rootLogger.addAppender(fileAppender)
        }
        
        // 启动日志上下文
        context.start()
    }
    
    /**
     * 创建控制台输出器
     */
    private fun createConsoleAppender(
        context: LoggerContext,
        loggingConfig: org.now.terminal.infrastructure.configuration.LoggingConfig
    ): ConsoleAppender<ch.qos.logback.classic.spi.ILoggingEvent> {
        val encoder = PatternLayoutEncoder().apply {
            context = context
            pattern = loggingConfig.format
            start()
        }
        
        return ConsoleAppender<ch.qos.logback.classic.spi.ILoggingEvent>().apply {
            context = context
            name = "CONSOLE"
            encoder = encoder
            start()
        }
    }
    
    /**
     * 创建文件输出器
     */
    private fun createFileAppender(
        context: LoggerContext,
        loggingConfig: org.now.terminal.infrastructure.configuration.LoggingConfig
    ): FileAppender<ch.qos.logback.classic.spi.ILoggingEvent> {
        val encoder = PatternLayoutEncoder().apply {
            context = context
            pattern = loggingConfig.format
            start()
        }
        
        // 确保日志目录存在
        val logPath = Paths.get(loggingConfig.file.path)
        logPath.parent?.toFile()?.mkdirs()
        
        return RollingFileAppender<ch.qos.logback.classic.spi.ILoggingEvent>().apply {
            context = context
            name = "FILE"
            file = loggingConfig.file.path
            encoder = encoder
            
            val rollingPolicy = SizeAndTimeBasedRollingPolicy<ch.qos.logback.classic.spi.ILoggingEvent>().apply {
                context = context
                fileNamePattern = "${loggingConfig.file.path}.%d{yyyy-MM-dd}.%i.gz"
                maxFileSize = FileSize.valueOf(loggingConfig.file.maxFileSize)
                maxHistory = loggingConfig.file.maxHistory
                totalSizeCap = FileSize.valueOf("1GB")
                setParent(this@apply)
                start()
            }
            
            rollingPolicy = rollingPolicy
            start()
        }
    }
    
    /**
     * 重新配置日志系统（用于热重载）
     */
    fun reconfigure() {
        logger.info("Reconfiguring logging system...")
        configure()
    }
}
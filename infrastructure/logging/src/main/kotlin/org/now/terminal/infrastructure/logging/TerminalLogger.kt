package org.now.terminal.infrastructure.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * 终端应用全局日志工具类
 * 提供统一的日志输出接口，确保所有模块使用相同的日志配置
 */
object TerminalLogger {
    
    /**
     * 获取指定类的日志记录器
     */
    fun getLogger(clazz: Class<*>): Logger {
        return LoggerFactory.getLogger(clazz)
    }
    
    /**
     * 获取指定名称的日志记录器
     */
    fun getLogger(name: String): Logger {
        return LoggerFactory.getLogger(name)
    }
    
    /**
     * 初始化日志系统
     * 应该在应用启动时调用
     */
    fun initialize() {
        LoggingConfigurator.configure()
    }
    
    /**
     * 重新配置日志系统（用于热重载）
     */
    fun reconfigure() {
        LoggingConfigurator.reconfigure()
    }
}

/**
 * 日志记录器扩展函数，提供更便捷的日志使用方式
 */
inline fun <reified T> T.logger(): Logger {
    return TerminalLogger.getLogger(T::class.java)
}

/**
 * 顶层日志函数，提供全局日志访问
 */
val logger: Logger
    get() = TerminalLogger.getLogger("GlobalLogger")
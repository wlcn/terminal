package org.now.terminal.websocket

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.koin.ktor.plugin.Koin
import org.koin.ktor.plugin.koin
import org.now.terminal.infrastructure.configuration.ConfigurationManager
import org.now.terminal.infrastructure.eventbus.EventBus
import org.now.terminal.infrastructure.logging.TerminalLogger
import org.now.terminal.websocket.di.webSocketModule

/**
 * WebSocket Gateway应用入口点
 * 启动Ktor服务器并配置WebSocket功能
 */
object WebSocketApplication {
    
    /**
     * 启动WebSocket服务器
     * @param port 服务器端口，默认从配置管理器获取
     */
    fun start(port: Int? = null) {
        // 初始化配置管理器
        ConfigurationManager.initialize()
        
        // 初始化日志系统
        TerminalLogger.initialize()
        
        val actualPort = port ?: ConfigurationManager.getServerPort()
        embeddedServer(Netty, port = actualPort) {
            configureApplication()
        }.start(wait = true)
    }
    
    /**
     * Ktor应用模块配置
     */
    private fun Application.configureApplication() {
        // 配置Koin依赖注入
        install(Koin) {
            // 加载WebSocket模块和TerminalSession模块
            modules(webSocketModule)
        }
        
        // 启动事件总线
        startEventBus()
        
        // 配置WebSocket功能
        configureWebSocket()
        
        // 配置日志
        configureLogging()
    }
    
    /**
     * 启动事件总线
     */
    private fun Application.startEventBus() {
        val logger = TerminalLogger.getLogger(WebSocketApplication::class.java)
        try {
            // 获取事件总线实例并启动
            val eventBus = koin().get<EventBus>()
            if (!eventBus.isRunning()) {
                eventBus.start()
                logger.info("✅ Event bus started successfully")
            } else {
                logger.info("ℹ️ Event bus is already running")
            }
        } catch (e: Exception) {
            logger.error("❌ Failed to start event bus: {}", e.message)
        }
    }
    
    /**
     * 配置日志
     */
    private fun Application.configureLogging() {
        // 这里可以配置日志，但Ktor默认会使用logback
        // 项目已经配置了logback，所以这里不需要额外配置
    }
    
    /**
     * 主函数，用于独立运行WebSocket Gateway
     */
    @JvmStatic
    fun main(args: Array<String>) {
        // 解析命令行参数
        val (port, environment, osType) = parseCommandLineArgs(args)
        
        // 初始化配置管理器（支持环境配置和操作系统配置）
        ConfigurationManager.initialize(environment = environment, osType = osType)
        
        // 初始化日志系统
        TerminalLogger.initialize()
        
        val logger = TerminalLogger.getLogger(WebSocketApplication::class.java)
        val actualPort = port ?: ConfigurationManager.getServerPort()
        
        logger.info("🚀 Starting WebSocket Gateway on port {}", actualPort)
        logger.info("📋 Configuration: environment={}, osType={}", environment ?: "default", osType ?: "auto")
        start(actualPort)
    }
    
    /**
     * 解析命令行参数
     * 支持格式：--port=8080 --env=prod --os=windows
     */
    private fun parseCommandLineArgs(args: Array<String>): Triple<Int?, String?, String?> {
        var port: Int? = null
        var environment: String? = null
        var osType: String? = null
        
        args.forEach { arg ->
            when {
                arg.startsWith("--port=") -> {
                    port = arg.substring(7).toIntOrNull()
                }
                arg.startsWith("--env=") -> {
                    environment = arg.substring(6)
                }
                arg.startsWith("--os=") -> {
                    osType = arg.substring(5)
                }
                arg.toIntOrNull() != null -> {
                    // 向后兼容：第一个参数如果是数字，认为是端口
                    if (port == null) {
                        port = arg.toIntOrNull()
                    }
                }
            }
        }
        
        return Triple(port, environment, osType)
    }
}
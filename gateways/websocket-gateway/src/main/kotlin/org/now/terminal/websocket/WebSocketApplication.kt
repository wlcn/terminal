package org.now.terminal.websocket

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.koin.ktor.plugin.Koin
import org.now.terminal.infrastructure.configuration.ConfigurationManager
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
        
        // 配置WebSocket功能
        configureWebSocket()
        
        // 配置日志
        configureLogging()
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
        val port = if (args.isNotEmpty()) args[0].toIntOrNull() else null
        
        // 初始化配置管理器
        ConfigurationManager.initialize()
        
        // 初始化日志系统
        TerminalLogger.initialize()
        
        val logger = TerminalLogger.getLogger(WebSocketApplication::class.java)
        val actualPort = port ?: ConfigurationManager.getServerPort()
        
        logger.info("🚀 Starting WebSocket Gateway on port {}", actualPort)
        start(actualPort)
    }
}
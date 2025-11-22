package org.now.terminal.websocket

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.koin.ktor.plugin.Koin
import org.now.terminal.websocket.di.webSocketModule

/**
 * WebSocket Gateway应用入口点
 * 启动Ktor服务器并配置WebSocket功能
 */
object WebSocketApplication {
    
    /**
     * 启动WebSocket服务器
     * @param port 服务器端口，默认8080
     */
    fun start(port: Int = 8080) {
        embeddedServer(Netty, port = port) {
            configureApplication()
        }.start(wait = true)
    }
    
    /**
     * Ktor应用模块配置
     */
    private fun Application.configureApplication() {
        // 配置Koin依赖注入
        install(Koin) {
            // Koin 4.x版本使用不同的日志配置方式
            // 直接使用默认配置，项目已经配置了logback
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
        val port = if (args.isNotEmpty()) args[0].toIntOrNull() ?: 8080 else 8080
        println("🚀 Starting WebSocket Gateway on port $port...")
        start(port)
    }
}
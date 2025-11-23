package org.now.terminal.server

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.koin.ktor.plugin.Koin
import org.koin.ktor.plugin.koin
import org.now.terminal.infrastructure.configuration.di.configurationModule
import org.now.terminal.infrastructure.eventbus.di.eventBusModule
import org.now.terminal.infrastructure.eventbus.LogEventHandler
import org.now.terminal.infrastructure.logging.di.loggingModule
import org.now.terminal.session.di.terminalSessionModule
import org.now.terminal.websocket.di.webSocketModule
import org.now.terminal.shared.events.Event
import org.slf4j.LoggerFactory

// 导入配置方法
import org.now.terminal.server.TerminalWebSocketGateway.configureWebSocketGateway
import org.now.terminal.server.TerminalManagementApi.configureManagementApi



/**
 * 终端服务器应用容器
 * 启动Ktor服务器并配置完整的终端服务功能
 * 
 * 职责：
 * - 应用启动和生命周期管理
 * - 依赖注入容器配置
 * - 基础设施初始化
 * - 业务模块集成
 */
object TerminalServerApplication {
    
    /**
     * 启动终端服务器
     * @param port 服务器端口，默认从配置管理器获取
     */
    fun start(port: Int? = null) {
        embeddedServer(Netty, port = port ?: 8080) {
            configureApplication()
        }.start(wait = true)
    }
    
    /**
     * Ktor应用模块配置
     */
    private fun Application.configureApplication() {
        val logger = LoggerFactory.getLogger("TerminalServerApplication")
        
        // 配置Content Negotiation（JSON序列化）
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = false  // 设置为false以确保所有字段都被序列化
                encodeDefaults = true      // 确保默认值也被序列化
            })
        }
        
        logger.info("🔧 配置Content Negotiation完成")
        
        // 配置Koin依赖注入
        install(Koin) {
            // 加载基础设施模块和业务模块
            modules(configurationModule, eventBusModule, loggingModule, terminalSessionModule, webSocketModule)
        }
        
        logger.info("🔧 配置Koin依赖注入完成")
        
        // 初始化基础设施
        initializeInfrastructure()
        
        logger.info("🔧 初始化基础设施完成")
        
        // 配置WebSocket网关功能
        configureWebSocketGateway()
        
        logger.info("🔧 配置WebSocket网关完成")
        
        // 配置管理API端点
        configureManagementApi()
        
        logger.info("🔧 配置管理API完成")
    }
    
    /**
     * 初始化基础设施
     */
    private fun Application.initializeInfrastructure() {
        // 通过Koin获取基础设施服务并初始化
        val koin = koin()
        
        // 初始化配置系统
        val configurationService = koin.get<org.now.terminal.infrastructure.configuration.ConfigurationLifecycleService>()
        configurationService.initialize()
        
        // 初始化日志系统
        val loggingService = koin.get<org.now.terminal.infrastructure.logging.LoggingLifecycleService>()
        loggingService.initialize()
        
        // 启动事件总线
        val eventBus = koin.get<org.now.terminal.infrastructure.eventbus.EventBus>()
        eventBus.start()
        
        // 注册默认事件处理器（使用协程执行挂起函数）
        val logEventHandler = koin.get<LogEventHandler>()
        runBlocking {
            eventBus.subscribe(Event::class.java, logEventHandler)
        }
        
        val logger = LoggerFactory.getLogger("TerminalServerApplication")
        logger.info("✅ 默认事件处理器注册完成")
    }
    

    

    

    
    /**
     * 主函数，用于独立运行终端服务器
     */
    @JvmStatic
    fun main(args: Array<String>) {
        // 解析命令行参数
        val (port, environment, osType) = parseCommandLineArgs(args)
        
        // 设置环境变量，供配置系统使用
        environment?.let { System.setProperty("APP_ENV", it) }
        osType?.let { System.setProperty("OS_TYPE", it) }
        
        println("🚀 Starting Terminal Server on port ${port ?: 8080}")
        println("📋 Configuration: environment=${environment ?: "default"}, osType=${osType ?: "auto"}")
        start(port)
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
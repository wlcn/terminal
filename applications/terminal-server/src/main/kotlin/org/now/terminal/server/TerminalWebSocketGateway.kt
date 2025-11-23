package org.now.terminal.server

import io.ktor.server.application.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import org.koin.ktor.plugin.inject
import org.slf4j.LoggerFactory
import org.now.terminal.session.application.usecases.HandleInputUseCase
import org.now.terminal.session.application.usecases.CheckSessionActiveUseCase

/**
 * WebSocket网关配置
 * WebSocket仅用于命令行交互，管理操作通过API处理
 */
object TerminalWebSocketGateway {
    
    /**
     * 配置WebSocket网关功能
     */
    fun Application.configureWebSocketGateway() {
        install(WebSockets)
        
        val logger = LoggerFactory.getLogger("TerminalWebSocketGateway")
        
        // WebSocket endpoint for reconnecting to existing sessions
        webSocket("/ws/{sessionId}") {
            val webSocketServer by inject<org.now.terminal.websocket.WebSocketServer>()
            val checkSessionActiveUseCase by inject<CheckSessionActiveUseCase>()
            val sessionIdParam = call.parameters["sessionId"]
            
            if (sessionIdParam == null) {
                logger.warn("❌ WebSocket连接缺少会话ID参数")
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Session ID is required"))
                return@webSocket
            }
            
            // Check if session exists and is active using use case
            val isActive = checkSessionActiveUseCase.execute(sessionIdParam)
            if (!isActive) {
                logger.warn("❌ WebSocket连接尝试访问不存在的会话: {}", sessionId.value)
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Session not found"))
                return@webSocket
            }
            
            logger.info("🔌 WebSocket连接建立 - 会话ID: {}", sessionId.value)
            
            // Use WebSocketServer to handle the connection
            webSocketServer.handleConnection(
                sessionId = sessionIdParam,
                session = this,
                onMessage = { sessionId, input ->
                    // Handle command line input only
                    handleCommandLineInput(sessionId, input)
                },
                onClose = { sessionId ->
                    logger.info("🔌 WebSocket连接关闭 - 会话ID: {}", sessionId)
                }
            )
        }
    }
    
    /**
     * 处理WebSocket命令行输入
     * WebSocket仅用于命令行交互，所有输入都当作命令行处理
     */
    private suspend fun Application.handleCommandLineInput(
        sessionId: String, 
        input: String
    ) {
        try {
            val logger = LoggerFactory.getLogger("TerminalWebSocketGateway")
            val koin = koin()
            val handleInputUseCase = koin.get<HandleInputUseCase>()
            
            logger.info("📨 处理会话 {} 的命令行输入: {}", sessionId, input.trim())
            
            // 使用用例处理命令行输入
            handleInputUseCase.execute(sessionId, input)
            
            logger.info("✅ 命令行输入处理完成 - 会话ID: {}", sessionId)
        } catch (e: Exception) {
            val logger = LoggerFactory.getLogger("TerminalWebSocketGateway")
            logger.error("❌ 处理命令行输入时发生错误 - 会话ID: {}, 错误: {}", sessionId, e.message, e)
        }
    }
}
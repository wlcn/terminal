package org.now.terminal.websocket

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.consumeAsFlow
import org.now.terminal.shared.valueobjects.SessionId
import org.now.terminal.shared.valueobjects.UserId
import org.now.terminal.session.domain.services.TerminalSessionService
import org.koin.ktor.plugin.Koin
import org.koin.ktor.ext.get
import org.koin.ktor.ext.inject
import kotlin.time.Duration.Companion.seconds
import org.slf4j.LoggerFactory

/**
 * WebSocket服务器
 * 负责WebSocket连接管理和消息路由
 */
class WebSocketServer(
    private val outputPublisher: org.now.terminal.session.domain.services.TerminalOutputPublisher,
    private val terminalSessionService: TerminalSessionService
) {
    
    private val logger = LoggerFactory.getLogger(WebSocketServer::class.java)
    
    /**
     * 处理WebSocket连接
     * @param sessionId 会话ID
     * @param session WebSocket会话
     */
    suspend fun handleConnection(sessionId: SessionId, session: WebSocketSession) {
        logger.info("🔌 WebSocket connection established for session: {}", sessionId.value)
        
        try {
            // 注册会话（需要扩展TerminalOutputPublisher接口）
            if (outputPublisher is WebSocketOutputPublisher) {
                outputPublisher.registerSession(sessionId, session)
                logger.info("✅ Session registered: {}", sessionId.value)
            } else {
                logger.warn("⚠️  OutputPublisher is not WebSocketOutputPublisher, session registration skipped")
            }
            
            // 监听消息和连接关闭
            session.incoming.consumeAsFlow().collect { frame ->
                when (frame) {
                    is Frame.Text -> {
                        // 处理文本消息（终端输入）
                        val input = frame.readText()
                        logger.info("📨 Received input from session {}: {}", sessionId.value, input.trim())
                        handleTerminalInput(sessionId, input)
                    }
                    is Frame.Close -> {
                        logger.info("🔌 WebSocket connection closed for session: {}", sessionId.value)
                        // 连接关闭时注销会话
                        if (outputPublisher is WebSocketOutputPublisher) {
                            outputPublisher.unregisterSession(sessionId)
                            logger.info("✅ Session unregistered: {}", sessionId.value)
                        }
                    }
                    else -> {
                        logger.debug("📡 Received frame type: {}", frame::class.simpleName)
                        // 忽略其他类型的帧
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("❌ Error in WebSocket connection for session {}: {}", sessionId.value, e.message, e)
            // 异常处理
            if (outputPublisher is WebSocketOutputPublisher) {
                outputPublisher.unregisterSession(sessionId)
                logger.info("✅ Session unregistered due to error: {}", sessionId.value)
            }
            throw e
        }
    }
    
    /**
     * 处理终端输入
     * @param sessionId 会话ID
     * @param input 输入内容
     */
    private suspend fun handleTerminalInput(sessionId: SessionId, input: String) {
        logger.info("🔄 Processing terminal input for session {}: {}", sessionId.value, input.trim())
        
        try {
            // 检查会话是否活跃
            val isActive = terminalSessionService.isSessionActive(sessionId)
            logger.info("📊 Session {} active status: {}", sessionId.value, isActive)
            
            if (isActive) {
                // 会话已存在，直接处理输入
                logger.info("✅ Session exists, handling input")
                terminalSessionService.handleInput(sessionId, input)
                logger.info("✅ Input handled successfully")
            } else {
                // 会话不存在，创建新会话
                logger.info("🆕 Session does not exist, creating new session")
                val userId = org.now.terminal.shared.valueobjects.UserId.generate()
                val ptyConfig = org.now.terminal.session.domain.valueobjects.PtyConfiguration.createDefault(
                    org.now.terminal.session.domain.valueobjects.TerminalCommand("bash")
                )
                logger.info("🔧 Creating session with userId: {}, ptyConfig: {}", userId.value, ptyConfig)
                terminalSessionService.createSession(userId, ptyConfig)
                logger.info("✅ Session created successfully")
                terminalSessionService.handleInput(sessionId, input)
                logger.info("✅ Input handled for new session")
            }
        } catch (e: Exception) {
            logger.error("❌ Error processing terminal input for session {}: {}", sessionId.value, e.message, e)
            // 发送错误消息到前端
            if (outputPublisher is WebSocketOutputPublisher) {
                try {
                    val errorMessage = "\r\n❌ Error processing command: ${e.message}\r\n$ "
                    logger.info("📤 Sending error message to frontend: {}", errorMessage.trim())
                    outputPublisher.publishOutput(sessionId, errorMessage)
                    logger.info("✅ Error message sent successfully")
                } catch (sendError: Exception) {
                    logger.error("❌ Failed to send error message: {}", sendError.message, sendError)
                }
            }
        }
    }
    
    /**
     * 关闭所有WebSocket连接
     */
    suspend fun shutdown() {
        outputPublisher.closeAllSessions()
    }
    
    /**
     * 获取活跃会话数量
     * @return 活跃会话数量
     */
    suspend fun getActiveSessionCount(): Int {
        return outputPublisher.getActiveSessionCount()
    }
}

/**
 * Ktor应用扩展函数
 * 配置WebSocket路由和功能
 */
fun Application.configureWebSocket() {
    val logger = LoggerFactory.getLogger("WebSocketServer")
    
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    
    routing {
        // 简化的WebSocket连接端点 - 直接创建新会话
        webSocket("/ws") {
            try {
                logger.info("🔌 新的WebSocket连接请求")
                
                // 创建会话
                val userId = org.now.terminal.shared.valueobjects.UserId.generate()
                val ptyConfig = org.now.terminal.session.domain.valueobjects.PtyConfiguration.createDefault(
                    org.now.terminal.session.domain.valueobjects.TerminalCommand("bash")
                )
                
                val terminalSessionService by inject<TerminalSessionService>()
                val sessionId = terminalSessionService.createSession(userId, ptyConfig)
                logger.info("✅ 会话创建成功 - 会话ID: {}, 用户ID: {}", sessionId.value, userId.value)
                
                // 立即发送Session ID给前端
                send("SESSION_ID:${sessionId.value}")
                logger.info("📤 发送Session ID给前端: {}", sessionId.value)
                
                // 处理WebSocket连接
                val webSocketServer by inject<WebSocketServer>()
                webSocketServer.handleConnection(sessionId, this)
                
            } catch (e: Exception) {
                logger.error("❌ WebSocket连接处理异常", e)
                close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, "Internal server error"))
            }
        }
        
        // 保留原有的会话连接端点（用于重连等场景）
        webSocket("/ws/{sessionId}") {
            val sessionIdParam = call.parameters["sessionId"] ?: ""
            logger.info("🔌 WebSocket连接请求 - 会话ID: {}", sessionIdParam)
            
            try {
                val sessionId = SessionId.create(sessionIdParam)
                logger.info("✅ 会话ID验证成功: {}", sessionId.value)
                
                // 检查会话是否存在
                val terminalSessionService by inject<TerminalSessionService>()
                val isActive = terminalSessionService.isSessionActive(sessionId)
                if (!isActive) {
                    logger.warn("⚠️ 会话不存在或已终止: {}", sessionId.value)
                    close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Session not found or terminated"))
                    return@webSocket
                }
                
                // 处理WebSocket连接
                val webSocketServer by inject<WebSocketServer>()
                webSocketServer.handleConnection(sessionId, this)
                
            } catch (e: IllegalArgumentException) {
                logger.error("❌ 无效的会话ID格式: {}", sessionIdParam)
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Invalid session ID"))
            } catch (e: Exception) {
                logger.error("❌ WebSocket连接处理异常", e)
                close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, "Internal server error"))
            }
        }
    }
}
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
import org.now.terminal.session.domain.services.TerminalOutputPublisher
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
    private val outputPublisher: TerminalOutputPublisher
) {
    
    private val logger = LoggerFactory.getLogger(WebSocketServer::class.java)
    
    /**
     * 处理WebSocket连接
     * @param sessionId 会话ID
     * @param session WebSocket会话
     * @param onMessage 消息处理回调函数（业务无关）
     * @param onClose 连接关闭回调函数（业务无关）
     */
    suspend fun handleConnection(
        sessionId: SessionId, 
        session: WebSocketSession,
        onMessage: suspend (SessionId, String) -> Unit = { _, _ -> },
        onClose: suspend (SessionId) -> Unit = { _ -> }
    ) {
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
                        // 处理文本消息（业务无关，只负责消息转发）
                        val input = frame.readText()
                        logger.info("📨 Received input from session {}: {}", sessionId.value, input.trim())
                        onMessage(sessionId, input)
                    }
                    is Frame.Close -> {
                        logger.info("🔌 WebSocket connection closed for session: {}", sessionId.value)
                        // 连接关闭时注销会话
                        if (outputPublisher is WebSocketOutputPublisher) {
                            outputPublisher.unregisterSession(sessionId)
                            logger.info("✅ Session unregistered: {}", sessionId.value)
                        }
                        onClose(sessionId)
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
     * 处理WebSocket连接（字符串sessionId版本）
     * @param sessionId 会话ID字符串
     * @param session WebSocket会话
     * @param onMessage 消息处理回调函数（业务无关）
     * @param onClose 连接关闭回调函数（业务无关）
     */
    suspend fun handleConnection(
        sessionId: String, 
        session: WebSocketSession,
        onMessage: suspend (String, String) -> Unit = { _, _ -> },
        onClose: suspend (String) -> Unit = { _ -> }
    ) {
        // 验证sessionId格式
        val sessionIdObj = SessionId.create(sessionId)
        
        // 调用对象版本的方法，并在回调中进行类型转换
        handleConnection(
            sessionId = sessionIdObj,
            session = session,
            onMessage = { id, input -> onMessage(id.value, input) },
            onClose = { id -> onClose(id.value) }
        )
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
 * Ktor应用扩展函数（业务无关）
 * 配置WebSocket路由和功能，业务逻辑通过回调函数处理
 */
fun Application.configureWebSocket(
    onNewConnection: suspend (WebSocketSession) -> SessionId = { 
        throw UnsupportedOperationException("New connection handler not implemented") 
    },
    onReconnect: suspend (SessionId, WebSocketSession) -> Boolean = { _, _ -> 
        throw UnsupportedOperationException("Reconnect handler not implemented") 
    },
    onMessage: suspend (SessionId, String) -> Unit = { _, _ -> }
) {
    val logger = LoggerFactory.getLogger("WebSocketServer")
    
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    
    routing {
        // 新连接端点 - 业务无关，通过回调处理
        webSocket("/ws") {
            try {
                logger.info("🔌 新的WebSocket连接请求")
                
                // 通过回调函数处理新连接业务逻辑
                val sessionId = onNewConnection(this)
                logger.info("✅ 会话创建成功 - 会话ID: {}", sessionId.value)
                
                // 立即发送Session ID给前端
                send("SESSION_ID:${sessionId.value}")
                logger.info("📤 发送Session ID给前端: {}", sessionId.value)
                
                // 处理WebSocket连接
                val webSocketServer by inject<WebSocketServer>()
                webSocketServer.handleConnection(sessionId, this, onMessage)
                
            } catch (e: Exception) {
                logger.error("❌ WebSocket连接处理异常", e)
                close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, "Internal server error"))
            }
        }
        
        // 重连端点 - 业务无关，通过回调处理
        webSocket("/ws/{sessionId}") {
            val sessionIdParam = call.parameters["sessionId"] ?: ""
            logger.info("🔌 WebSocket连接请求 - 会话ID: {}", sessionIdParam)
            
            try {
                val sessionId = SessionId.create(sessionIdParam)
                logger.info("✅ 会话ID验证成功: {}", sessionId.value)
                
                // 通过回调函数处理重连业务逻辑
                val isActive = onReconnect(sessionId, this)
                if (!isActive) {
                    logger.warn("⚠️ 会话不存在或已终止: {}", sessionId.value)
                    close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Session not found or terminated"))
                    return@webSocket
                }
                
                // 处理WebSocket连接
                val webSocketServer by inject<WebSocketServer>()
                webSocketServer.handleConnection(sessionId, this, onMessage)
                
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
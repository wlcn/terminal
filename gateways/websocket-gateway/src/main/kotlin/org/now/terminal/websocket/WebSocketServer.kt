package org.now.terminal.websocket

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.consumeAsFlow
import org.now.terminal.shared.valueobjects.SessionId
import org.koin.ktor.plugin.Koin
import org.koin.ktor.ext.get
import org.koin.ktor.ext.inject
import kotlin.time.Duration.Companion.seconds

/**
 * WebSocket服务器
 * 负责WebSocket连接管理和消息路由
 */
class WebSocketServer(
    private val outputPublisher: WebSocketOutputPublisher
) {
    
    /**
     * 处理WebSocket连接
     * @param sessionId 会话ID
     * @param session WebSocket会话
     */
    suspend fun handleConnection(sessionId: SessionId, session: WebSocketSession) {
        try {
            // 注册会话
            outputPublisher.registerSession(sessionId, session)
            
            // 监听连接关闭
            session.incoming.consumeAsFlow().collect {
                // 连接关闭时注销会话
                if (it is Frame.Close) {
                    outputPublisher.unregisterSession(sessionId)
                }
            }
        } catch (e: Exception) {
            // 异常处理
            outputPublisher.unregisterSession(sessionId)
            throw e
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
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    
    routing {
        webSocket("/ws/{sessionId}") {
            val sessionIdParam = call.parameters["sessionId"]
            if (sessionIdParam != null) {
                try {
                    val sessionId = SessionId.create(sessionIdParam)
                    val webSocketServer by inject<WebSocketServer>()
                    webSocketServer.handleConnection(sessionId, this)
                } catch (e: IllegalArgumentException) {
                    close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Invalid session ID"))
                }
            } else {
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Session ID required"))
            }
        }
    }
}
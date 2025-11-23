package org.now.terminal.server

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import org.koin.ktor.ext.inject
import org.now.terminal.session.application.usecases.HandleInputUseCase
import org.now.terminal.session.application.usecases.CheckSessionActiveUseCase
import org.now.terminal.shared.valueobjects.SessionId

/**
 * WebSocket网关配置
 * 仅负责参数传递和用例调用，不包含业务逻辑
 */
object TerminalWebSocketGateway {
    
    /**
     * 配置WebSocket网关功能
     */
    fun Application.configureWebSocketGateway() {
        install(WebSockets)
        
        routing {
            webSocket("/ws/{sessionId}") {
                val webSocketServer by inject<org.now.terminal.websocket.WebSocketServer>()
                val checkSessionActiveUseCase by inject<CheckSessionActiveUseCase>()
                val handleInputUseCase by inject<HandleInputUseCase>()
                val sessionIdParam = call.parameters["sessionId"] ?: ""
                
                // 使用用例检查会话状态
                val isActive = checkSessionActiveUseCase.execute(sessionIdParam)
                
                // 使用WebSocketServer处理连接
                webSocketServer.handleConnection(
                    sessionId = sessionIdParam,
                    session = this,
                    onMessage = { sessionId: String, input: String ->
                        // 使用用例处理命令行输入
                        handleInputUseCase.execute(sessionId, input)
                    },
                    onClose = { sessionId: String ->
                        // 连接关闭，无需业务逻辑
                    }
                )
            }
        }
    }
}
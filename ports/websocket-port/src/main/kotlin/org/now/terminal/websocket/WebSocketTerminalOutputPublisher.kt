package org.now.terminal.websocket

import org.now.terminal.session.domain.services.TerminalOutputPublisher
import org.now.terminal.session.domain.valueobjects.SessionId
import jakarta.inject.Singleton

/**
 * WebSocket终端输出发布器实现
 * 这是具体的技术实现，与业务模块解耦
 */
@Singleton
class WebSocketTerminalOutputPublisher : TerminalOutputPublisher {
    
    override fun publishOutput(sessionId: SessionId, output: String) {
        // TODO: 实现WebSocket推送逻辑
        // 这里应该通过WebSocket连接将输出推送到前端
        println("WebSocket推送 - 会话: $sessionId, 输出: ${output.take(50)}...")
    }
}
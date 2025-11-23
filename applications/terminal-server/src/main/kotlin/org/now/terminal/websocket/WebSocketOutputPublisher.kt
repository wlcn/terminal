package org.now.terminal.websocket

import org.now.terminal.session.domain.services.TerminalOutputPublisher
import org.now.terminal.shared.valueobjects.SessionId
import io.ktor.websocket.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * WebSocket终端输出发布器
 * Gateway层实现，负责WebSocket连接的推送功能
 * 直接实现TerminalOutputPublisher接口，简化架构
 */
class WebSocketOutputPublisher : TerminalOutputPublisher {
    
    /**
     * WebSocket会话管理器
     * 使用ConcurrentHashMap存储会话，确保线程安全
     */
    private val sessions = ConcurrentHashMap<SessionId, WebSocketSession>()
    
    /**
     * 互斥锁，用于确保会话操作的线程安全
     */
    private val mutex = Mutex()
    
    /**
     * 发布终端输出到指定会话
     * 优化：使用会话级别的锁避免全局锁竞争，提升并发性能
     * @param sessionId 会话ID
     * @param output 输出内容
     */
    override suspend fun publishOutput(sessionId: SessionId, output: String) {
        val session = sessions[sessionId]
        if (session != null) {
            try {
                // 直接发送，避免全局锁竞争
                session.send(Frame.Text(output))
            } catch (e: Exception) {
                // 发送失败时移除会话（使用互斥锁确保线程安全）
                mutex.withLock {
                    sessions.remove(sessionId)
                }
                throw WebSocketPublishException("Failed to send output to session $sessionId", e)
            }
        } else {
            throw WebSocketSessionNotFoundException("WebSocket session not found for sessionId: $sessionId")
        }
    }
    
    /**
     * 注册WebSocket会话
     * @param sessionId 会话ID
     * @param webSocketSession Ktor WebSocket会话
     */
    suspend fun registerSession(sessionId: SessionId, webSocketSession: WebSocketSession) {
        mutex.withLock {
            sessions[sessionId] = webSocketSession
        }
    }
    
    /**
     * 注册WebSocket会话（字符串sessionId版本）
     * @param sessionId 会话ID字符串
     * @param webSocketSession Ktor WebSocket会话
     */
    suspend fun registerSession(sessionId: String, webSocketSession: WebSocketSession) {
        val sessionIdObj = SessionId.create(sessionId)
        registerSession(sessionIdObj, webSocketSession)
    }
    
    /**
     * 注销WebSocket会话
     * @param sessionId 会话ID
     */
    suspend fun unregisterSession(sessionId: SessionId) {
        mutex.withLock {
            sessions.remove(sessionId)
        }
    }
    
    /**
     * 注销WebSocket会话（字符串sessionId版本）
     * @param sessionId 会话ID字符串
     */
    suspend fun unregisterSession(sessionId: String) {
        val sessionIdObj = SessionId.create(sessionId)
        unregisterSession(sessionIdObj)
    }
    
    /**
     * 检查会话是否已连接
     * @param sessionId 会话ID
     * @return 是否已连接
     */
    override suspend fun isSessionConnected(sessionId: SessionId): Boolean {
        return mutex.withLock {
            sessions.containsKey(sessionId)
        }
    }
    
    /**
     * 获取活跃会话数量
     * @return 活跃会话数量
     */
    override suspend fun getActiveSessionCount(): Int {
        return mutex.withLock {
            sessions.size
        }
    }
    
    /**
     * 关闭所有WebSocket连接
     */
    override suspend fun closeAllSessions() {
        mutex.withLock {
            sessions.forEach { (sessionId, session) ->
                try {
                    session.close(CloseReason(CloseReason.Codes.NORMAL, "Server shutdown"))
                } catch (e: Exception) {
                    // 忽略关闭异常
                }
            }
            sessions.clear()
        }
    }
}

/**
 * WebSocket发布异常
 */
class WebSocketPublishException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * WebSocket会话未找到异常
 */
class WebSocketSessionNotFoundException(
    message: String
) : RuntimeException(message)
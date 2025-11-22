package org.now.terminal.session.infrastructure

import org.now.terminal.session.domain.services.TerminalOutputPublisher
import org.now.terminal.shared.valueobjects.SessionId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 测试用的终端输出发布器实现
 * 用于在测试环境中提供TerminalOutputPublisher接口的实现
 */
class TestTerminalOutputPublisher : TerminalOutputPublisher {
    
    private val sessions = mutableMapOf<SessionId, Boolean>()
    private val mutex = Mutex()
    
    override suspend fun publishOutput(sessionId: SessionId, output: String) {
        mutex.withLock {
            // 在测试中，我们只是记录输出，不实际推送
            println("TestTerminalOutputPublisher: Publishing output to session $sessionId: $output")
        }
    }
    
    override suspend fun getActiveSessionCount(): Int {
        return mutex.withLock {
            sessions.count { it.value }
        }
    }
    
    override suspend fun isSessionConnected(sessionId: SessionId): Boolean {
        return mutex.withLock {
            sessions[sessionId] ?: false
        }
    }
    
    override suspend fun closeAllSessions() {
        mutex.withLock {
            sessions.clear()
        }
    }
    
    /**
     * 测试专用方法：注册会话
     */
    suspend fun registerSession(sessionId: SessionId) {
        mutex.withLock {
            sessions[sessionId] = true
        }
    }
    
    /**
     * 测试专用方法：注销会话
     */
    suspend fun unregisterSession(sessionId: SessionId) {
        mutex.withLock {
            sessions.remove(sessionId)
        }
    }
}
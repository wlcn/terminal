package org.now.terminal.boundedcontexts.terminalsession.domain.service

import kotlinx.coroutines.*
import org.now.terminal.boundedcontexts.terminalsession.domain.model.TerminalSession
import org.now.terminal.boundedcontexts.terminalsession.domain.model.TerminalSessionStatus
import org.now.terminal.boundedcontexts.terminalsession.domain.model.TerminalSize
import org.now.terminal.boundedcontexts.terminalsession.domain.service.TerminalProcessManager
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class TerminalSessionService(
    private val defaultShellType: String,
    private val sessionTimeoutMs: Long = 30 * 60 * 1000, // 默认30分钟超时
    private val terminalProcessManager: TerminalProcessManager? = null
) {
    // 存储会话的ConcurrentHashMap
    private val sessions = ConcurrentHashMap<String, TerminalSession>()
    
    // 会话过期管理器
    private val sessionExpiryManager = SessionExpiryManager(sessionTimeoutMs, terminalProcessManager)
    
    fun createSession(
        userId: String,
        title: String?,
        workingDirectory: String,
        shellType: String = defaultShellType,
        size: TerminalSize? = null
    ): TerminalSession {
        val now = System.currentTimeMillis()
        val session = TerminalSession(
            id = UUID.randomUUID().toString(),
            userId = userId,
            title = title,
            workingDirectory = workingDirectory,
            shellType = shellType,
            status = TerminalSessionStatus.ACTIVE,
            terminalSize = size ?: TerminalSize(80, 24),
            createdAt = now,
            updatedAt = now,
            lastActiveTime = now,
            expiredAt = now + sessionTimeoutMs
        )
        sessions[session.id] = session
        
        // 为新会话启动过期检查
        sessionExpiryManager.startExpiryCheck(session) { expiredSession ->
            // 会话过期回调，从map中移除
            sessions.remove(expiredSession.id)
        }
        
        return session
    }
    
    fun getSessionById(id: String): TerminalSession? {
        return sessions[id]?.also {
            // 更新活动时间
            updateSessionActivity(it)
        }
    }
    
    fun getSessionsByUserId(userId: String): List<TerminalSession> {
        return sessions.values.filter { it.userId == userId }
    }
    
    fun getAllSessions(): List<TerminalSession> {
        return sessions.values.toList()
    }
    
    fun resizeTerminal(id: String, columns: Int, rows: Int): TerminalSession? {
        return sessions[id]?.also {
            it.terminalSize = TerminalSize(columns, rows)
            updateSessionActivity(it)
        }
    }
    
    fun terminateSession(id: String, reason: String? = null): TerminalSession? {
        return sessions[id]?.also {
            // 取消过期检查
            sessionExpiryManager.cancelExpiryCheck(id)
            
            it.status = TerminalSessionStatus.TERMINATED
            it.updatedAt = System.currentTimeMillis()
            
            // 清理相关资源
            terminalProcessManager?.terminateProcess(id)
            
            // 从map中移除
            sessions.remove(id)
        }
    }
    
    fun updateSessionStatus(id: String, status: TerminalSessionStatus): TerminalSession? {
        return sessions[id]?.also {
            it.status = status
            updateSessionActivity(it)
        }
    }
    
    fun deleteSession(id: String): Boolean {
        // 取消过期检查
        sessionExpiryManager.cancelExpiryCheck(id)
        
        val session = sessions.remove(id)
        if (session != null) {
            // 清理相关资源
            terminalProcessManager?.terminateProcess(id)
        }
        return session != null
    }
    
    /**
     * 更新session活动时间
     */
    fun updateSessionActivity(session: TerminalSession) {
        val now = System.currentTimeMillis()
        session.lastActiveTime = now
        session.updatedAt = now
        session.expiredAt = now + sessionTimeoutMs
        
        // 重新启动过期检查
        sessionExpiryManager.restartExpiryCheck(session) { expiredSession ->
            // 会话过期回调，从map中移除
            sessions.remove(expiredSession.id)
        }
    }
    
    /**
     * 关闭服务，清理资源
     */
    fun shutdown() {
        // 关闭会话过期管理器
        sessionExpiryManager.shutdown()
    }
}

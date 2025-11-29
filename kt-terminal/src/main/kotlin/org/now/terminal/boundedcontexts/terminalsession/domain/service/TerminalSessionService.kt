package org.now.terminal.boundedcontexts.terminalsession.domain.service

import kotlinx.coroutines.*
import org.now.terminal.boundedcontexts.terminalsession.domain.model.TerminalSession
import org.now.terminal.boundedcontexts.terminalsession.domain.model.TerminalSessionStatus
import org.now.terminal.boundedcontexts.terminalsession.domain.model.TerminalSize
import org.now.terminal.boundedcontexts.terminalsession.domain.service.TerminalProcessManager
import java.util.*

class TerminalSessionService(
    private val terminalConfig: org.now.terminal.boundedcontexts.terminalsession.domain.model.TerminalConfig,
    private val sessionStorage: SessionStorage = InMemorySessionStorage(),
    private val terminalProcessManager: TerminalProcessManager? = null
) {
    private val defaultShellType = terminalConfig.defaultShellType
    private val sessionTimeoutMs = terminalConfig.sessionTimeoutMs
    
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
            terminalSize = size ?: terminalConfig.defaultTerminalSize,
            createdAt = now,
            updatedAt = now,
            lastActiveTime = now,
            expiredAt = now + sessionTimeoutMs
        )
        sessionStorage.save(session)
        
        // 为新会话启动过期检查
        sessionExpiryManager.startExpiryCheck(session) { expiredSession ->
            // 会话过期回调，从存储中移除
            sessionStorage.deleteById(expiredSession.id)
        }
        
        return session
    }
    
    fun getSessionById(id: String): TerminalSession? {
        return sessionStorage.getById(id)?.also {
            // 更新活动时间
            updateSessionActivity(it)
        }
    }
    
    fun getSessionsByUserId(userId: String): List<TerminalSession> {
        return sessionStorage.getByUserId(userId)
    }
    
    fun getAllSessions(): List<TerminalSession> {
        return sessionStorage.getAll()
    }
    
    fun resizeTerminal(id: String, columns: Int, rows: Int): TerminalSession? {
        return sessionStorage.getById(id)?.also {
            it.terminalSize = TerminalSize(columns, rows)
            updateSessionActivity(it)
            sessionStorage.update(it)
        }
    }
    
    fun terminateSession(id: String, reason: String? = null): TerminalSession? {
        return sessionStorage.getById(id)?.also {
            // 取消过期检查
            sessionExpiryManager.cancelExpiryCheck(id)
            
            it.status = TerminalSessionStatus.TERMINATED
            it.updatedAt = System.currentTimeMillis()
            
            // 清理相关资源
            terminalProcessManager?.terminateProcess(id)
            
            // 从存储中移除
            sessionStorage.deleteById(id)
        }
    }
    
    fun updateSessionStatus(id: String, status: TerminalSessionStatus): TerminalSession? {
        return sessionStorage.getById(id)?.also {
            it.status = status
            updateSessionActivity(it)
            sessionStorage.update(it)
        }
    }
    
    fun deleteSession(id: String): Boolean {
        // 取消过期检查
        sessionExpiryManager.cancelExpiryCheck(id)
        
        val session = sessionStorage.deleteById(id)
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
        
        // 更新存储中的会话
        sessionStorage.update(session)
        
        // 重新启动过期检查
        sessionExpiryManager.restartExpiryCheck(session) { expiredSession ->
            // 会话过期回调，从存储中移除
            sessionStorage.deleteById(expiredSession.id)
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

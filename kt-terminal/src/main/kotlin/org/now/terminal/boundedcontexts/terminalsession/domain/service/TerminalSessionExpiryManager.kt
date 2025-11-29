package org.now.terminal.boundedcontexts.terminalsession.domain.service

import kotlinx.coroutines.*
import org.now.terminal.boundedcontexts.terminalsession.domain.TerminalSession
import org.now.terminal.boundedcontexts.terminalsession.domain.TerminalSessionStatus
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 会话过期管理器
 * 负责处理会话的过期检查和清理逻辑
 */
class SessionExpiryManager(
    private val sessionTimeoutMs: Long,
    private val terminalProcessManager: TerminalProcessManager? = null
) {
    private val logger = LoggerFactory.getLogger(SessionExpiryManager::class.java)
    
    // 使用协程作用域和SupervisorJob来管理协程生命周期
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // 存储每个会话的过期检查协程
    private val sessionExpiryJobs = ConcurrentHashMap<String, Job>()
    
    /**
     * 为会话启动过期检查
     */
    fun startExpiryCheck(session: TerminalSession, onSessionExpired: (TerminalSession) -> Unit) {
        // 取消可能存在的旧协程
        sessionExpiryJobs.remove(session.id)?.cancel()
        
        // 启动新的过期检查协程
        val job = scope.launch {
            val delayTime = session.expiredAt?.minus(System.currentTimeMillis()) ?: sessionTimeoutMs
            
            if (delayTime > 0) {
                // 等待过期时间
                delay(delayTime)
                
                // 检查会话是否仍然存在且处于活动状态
                val now = System.currentTimeMillis()
                
                if (session.status == TerminalSessionStatus.ACTIVE && 
                    session.expiredAt != null && 
                    now > session.expiredAt!!) {
                    
                    try {
                        logger.info("Cleaning up expired session: {}, expired at: {}", session.id, Date(session.expiredAt!!))
                        
                        // 更新session状态
                        session.status = TerminalSessionStatus.TERMINATED
                        session.updatedAt = now
                        
                        // 清理相关资源
                        terminalProcessManager?.terminateProcess(session.id)
                        
                        // 调用回调函数，通知会话已过期
                        onSessionExpired(session)
                        
                        // 从map中移除
                        sessionExpiryJobs.remove(session.id)
                    } catch (e: Exception) {
                        logger.error("Error cleaning up session {}: {}", session.id, e.message)
                    }
                }
            } else {
                // 会话已经过期，立即清理
                cleanupExpiredSession(session, onSessionExpired)
            }
        }
        
        // 存储协程引用
        sessionExpiryJobs[session.id] = job
    }
    
    /**
     * 重新启动会话的过期检查
     */
    fun restartExpiryCheck(session: TerminalSession, onSessionExpired: (TerminalSession) -> Unit) {
        startExpiryCheck(session, onSessionExpired)
    }
    
    /**
     * 清理单个过期会话
     */
    private fun cleanupExpiredSession(session: TerminalSession, onSessionExpired: (TerminalSession) -> Unit) {
        val now = System.currentTimeMillis()
        
        if (session.status == TerminalSessionStatus.ACTIVE && 
            session.expiredAt != null && 
            now > session.expiredAt!!) {
            
            try {
                logger.info("Cleaning up expired session: ${session.id}, expired at: ${Date(session.expiredAt!!)}")
                
                // 更新session状态
                session.status = TerminalSessionStatus.TERMINATED
                session.updatedAt = now
                
                // 清理相关资源
                terminalProcessManager?.terminateProcess(session.id)
                
                // 调用回调函数，通知会话已过期
                onSessionExpired(session)
                
                // 从map中移除
                sessionExpiryJobs.remove(session.id)
            } catch (e: Exception) {
                logger.error("Error cleaning up session ${session.id}: ${e.message}")
            }
        }
    }
    
    /**
     * 取消会话的过期检查
     */
    fun cancelExpiryCheck(sessionId: String) {
        sessionExpiryJobs.remove(sessionId)?.cancel()
    }
    
    /**
     * 关闭过期管理器，清理所有资源
     */
    fun shutdown() {
        // 取消所有协程
        scope.cancel()
        
        // 清空map
        sessionExpiryJobs.clear()
    }
}

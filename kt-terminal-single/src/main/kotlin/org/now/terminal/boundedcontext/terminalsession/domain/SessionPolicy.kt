package org.now.terminal.boundedcontext.terminalsession.domain

import org.now.terminal.boundedcontext.terminalsession.domain.repositories.SessionRepository
import org.now.terminal.boundedcontext.user.domain.User
import org.now.terminal.boundedcontext.user.domain.TerminalSize

/**
 * 会话策略 - 控制会话的生命周期和资源限制
 */
class SessionPolicy(
    private val user: User,
    private val sessionRepository: SessionRepository
) {
    
    /**
     * 验证是否可以创建新会话
     */
    suspend fun validateSessionCreation(): SessionCreationResult {
        val activeSessions = sessionRepository.getActiveSessionsByUser(user.id)
        
        return when {
            !user.canCreateSession(activeSessions.size) -> 
                SessionCreationResult.ExceededLimit("用户已达到最大并发会话数")
            
            activeSessions.any { it.isExpired() } -> 
                SessionCreationResult.ExpiredSessionsExist("存在过期会话，请先清理")
            
            else -> SessionCreationResult.Allowed
        }
    }
    
    /**
     * 验证终端大小是否允许
     */
    fun validateTerminalSize(size: TerminalSize): Boolean {
        return size.columns <= user.maxTerminalSize.columns && 
               size.rows <= user.maxTerminalSize.rows
    }
    
    /**
     * 验证shell是否允许使用
     */
    fun validateShell(shell: String): Boolean {
        return user.hasPermissionForShell(shell)
    }
    
    /**
     * 获取会话超时时间
     */
    fun getSessionTimeout(): Long {
        return user.sessionLimit.maxSessionDuration
    }
}

/**
 * 会话创建结果
 */
sealed class SessionCreationResult {
    object Allowed : SessionCreationResult()
    data class ExceededLimit(val message: String) : SessionCreationResult()
    data class ExpiredSessionsExist(val message: String) : SessionCreationResult()
}
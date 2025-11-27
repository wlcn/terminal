package org.now.terminal.boundedcontext.terminalsession.domain

import java.time.Instant

/**
 * 会话统计信息值对象
 */
data class SessionStats(
    val startTime: Instant,
    val lastActivity: Instant,
    val bytesTransferred: Long,
    val commandsExecuted: Int
) {
    
    /**
     * 检查会话是否过期
     */
    fun isExpired(timeout: Long): Boolean {
        return Instant.now().toEpochMilli() - lastActivity.toEpochMilli() > timeout
    }
    
    /**
     * 更新活动时间
     */
    fun updateActivity(): SessionStats {
        return copy(lastActivity = Instant.now())
    }
}
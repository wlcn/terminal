package org.now.terminal.boundedcontexts.terminalsession.domain

import kotlinx.serialization.Serializable

@Serializable
class TerminalSession(
    val id: String,
    val userId: String,
    val title: String?,
    val workingDirectory: String,
    val shellType: String,
    var status: TerminalSessionStatus,
    var terminalSize: TerminalSize = TerminalSize(80, 24),
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    var lastActiveTime: Long = System.currentTimeMillis(),
    var expiredAt: Long? = null
) {
    /**
     * 更新会话活动时间
     */
    fun updateActivity(now: Long = System.currentTimeMillis()) {
        this.lastActiveTime = now
        this.updatedAt = now
    }
    
    /**
     * 计算并更新过期时间
     */
    fun updateExpiryTime(timeoutMs: Long, now: Long = System.currentTimeMillis()) {
        this.expiredAt = now + timeoutMs
        this.updatedAt = now
    }
    
    /**
     * 调整终端大小
     */
    fun resize(columns: Int, rows: Int) {
        this.terminalSize = TerminalSize(columns, rows)
        this.updatedAt = System.currentTimeMillis()
    }
    
    /**
     * 终止会话
     */
    fun terminate() {
        this.status = TerminalSessionStatus.TERMINATED
        this.updatedAt = System.currentTimeMillis()
    }
    
    /**
     * 检查会话是否过期
     */
    fun isExpired(now: Long = System.currentTimeMillis()): Boolean {
        return this.expiredAt?.let { it < now } ?: false
    }
    
    /**
     * 更新会话状态
     */
    fun updateStatus(newStatus: TerminalSessionStatus) {
        this.status = newStatus
        this.updatedAt = System.currentTimeMillis()
    }
}

@Serializable
enum class TerminalSessionStatus {
    ACTIVE,
    INACTIVE,
    TERMINATED
}

@Serializable
class TerminalSize(
    val columns: Int,
    val rows: Int
)

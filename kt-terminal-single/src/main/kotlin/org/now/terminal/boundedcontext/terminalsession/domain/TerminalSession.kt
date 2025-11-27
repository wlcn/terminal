package org.now.terminal.boundedcontext.terminalsession.domain

import org.now.terminal.kernel.communication.TerminalOutputMessage
import org.now.terminal.kernel.communication.TerminalResizeMessage
import org.now.terminal.boundedcontext.user.domain.TerminalSize
import org.now.terminal.boundedcontext.user.domain.User
import java.time.Instant

/**
 * 终端会话聚合根 - 核心领域模型
 */
class TerminalSession(
    val id: SessionId,
    val user: User,
    private val terminal: Terminal,
    private val communication: SessionCommunication,
    private val sessionPolicy: SessionPolicy,
    private var stats: SessionStats = SessionStats(
        startTime = Instant.now(),
        lastActivity = Instant.now(),
        bytesTransferred = 0,
        commandsExecuted = 0
    ),
    private var status: SessionStatus = SessionStatus.ACTIVE
) {
    
    /**
     * 处理用户输入 - 包含业务规则验证
     */
    suspend fun handleInput(input: String) {
        validateSessionActive()
        validateInputSize(input)
        
        terminal.write(input)
        updateActivity()
        stats = stats.copy(bytesTransferred = stats.bytesTransferred + input.length.toLong())
    }
    
    /**
     * 处理终端输出
     */
    suspend fun handleOutput(output: String) {
        validateSessionActive()
        
        communication.sendMessage(TerminalOutputMessage(output, id.value))
        updateActivity()
        stats = stats.copy(bytesTransferred = stats.bytesTransferred + output.length.toLong())
    }
    
    /**
     * 调整终端大小 - 包含策略验证
     */
    suspend fun resize(columns: Int, rows: Int) {
        validateSessionActive()
        
        val newSize = TerminalSize(columns, rows)
        if (!sessionPolicy.validateTerminalSize(newSize)) {
            throw SessionException("终端大小超出用户限制")
        }
        
        terminal.resize(columns, rows)
        communication.sendMessage(TerminalResizeMessage(columns, rows, id.value))
        updateActivity()
    }
    
    /**
     * 启动终端会话
     */
    suspend fun start(shell: String) {
        if (!sessionPolicy.validateShell(shell)) {
            throw SessionException("用户无权使用该shell")
        }
        
        terminal.start(this, shell)
        status = SessionStatus.ACTIVE
    }
    
    /**
     * 关闭终端会话
     */
    suspend fun close(reason: String = "正常关闭") {
        terminal.close()
        communication.close()
        status = SessionStatus.TERMINATED
        
        // 记录会话结束
        stats = stats.copy(lastActivity = Instant.now())
    }
    
    /**
     * 检查会话是否过期
     */
    fun isExpired(): Boolean {
        return stats.isExpired(sessionPolicy.getSessionTimeout())
    }
    
    /**
     * 强制过期会话
     */
    suspend fun expire() {
        if (status == SessionStatus.ACTIVE) {
            status = SessionStatus.EXPIRED
            close("会话过期")
        }
    }
    
    val isActive: Boolean
        get() = status == SessionStatus.ACTIVE && terminal.isRunning && communication.isConnected
    
    val sessionStats: SessionStats
        get() = stats
    
    // 私有方法
    private fun validateSessionActive() {
        if (!isActive) {
            throw SessionException("会话已结束")
        }
    }
    
    private fun validateInputSize(input: String) {
        if (input.length > 1024) { // 限制单次输入大小
            throw SessionException("输入数据过大")
        }
    }
    
    private fun updateActivity() {
        stats = stats.updateActivity()
    }
}

/**
 * 会话异常
 */
class SessionException(message: String) : RuntimeException(message)
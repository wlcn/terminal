package org.now.terminal.boundedcontext.terminalsession

import org.now.terminal.kernel.communication.TerminalMessage
import org.now.terminal.kernel.communication.TerminalOutputMessage
import org.now.terminal.kernel.communication.TerminalResizeMessage

/**
 * 终端会话聚合根 - 核心领域模型
 */
class TerminalSession(
    val id: SessionId,
    private var terminal: Terminal,
    private var communication: SessionCommunication
) {
    
    /**
     * 处理用户输入
     */
    suspend fun handleInput(input: String) {
        terminal.write(input)
    }
    
    /**
     * 处理终端输出
     */
    suspend fun handleOutput(output: String) {
        communication.sendMessage(TerminalOutputMessage(output, id.value))
    }
    
    /**
     * 调整终端大小
     */
    suspend fun resize(columns: Int, rows: Int) {
        terminal.resize(columns, rows)
        communication.sendMessage(TerminalResizeMessage(columns, rows, id.value))
    }
    
    /**
     * 启动终端会话
     */
    suspend fun start() {
        terminal.start(this)
    }
    
    /**
     * 关闭终端会话
     */
    suspend fun close() {
        terminal.close()
        communication.close()
    }
    
    val isActive: Boolean
        get() = terminal.isRunning && communication.isConnected
}

/**
 * 会话ID值对象
 */
@JvmInline
value class SessionId(val value: String) {
    init {
        require(value.isNotBlank()) { "Session ID cannot be blank" }
    }
}

/**
 * 终端接口 - 协议无关的终端操作抽象
 */
interface Terminal {
    suspend fun write(data: String)
    suspend fun resize(columns: Int, rows: Int)
    suspend fun start(session: TerminalSession)
    suspend fun close()
    val isRunning: Boolean
}

/**
 * 会话通讯接口
 */
interface SessionCommunication {
    suspend fun sendMessage(message: TerminalMessage)
    suspend fun receiveMessage(): TerminalMessage?
    suspend fun close()
    val isConnected: Boolean
}
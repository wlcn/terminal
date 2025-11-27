package org.now.terminal.kernel.communication

/**
 * 通讯协议抽象 - 支持WebSocket和WebTransport的协议无关接口
 */
interface CommunicationProtocol {
    suspend fun sendMessage(message: TerminalMessage)
    suspend fun receiveMessage(): TerminalMessage?
    fun close(reason: String = "")
    val isConnected: Boolean
}

/**
 * 终端消息协议
 */
sealed class TerminalMessage {
    abstract val type: MessageType
    abstract val data: String
}

data class TerminalInputMessage(
    override val data: String,
    val sessionId: String
) : TerminalMessage() {
    override val type: MessageType = MessageType.INPUT
}

data class TerminalOutputMessage(
    override val data: String,
    val sessionId: String
) : TerminalMessage() {
    override val type: MessageType = MessageType.OUTPUT
}

data class TerminalResizeMessage(
    val columns: Int,
    val rows: Int,
    val sessionId: String
) : TerminalMessage() {
    override val type: MessageType = MessageType.RESIZE
    override val data: String = "$columns,$rows"
}

data class TerminalErrorMessage(
    override val data: String,
    val sessionId: String,
    val errorCode: String
) : TerminalMessage() {
    override val type: MessageType = MessageType.ERROR
}

enum class MessageType {
    INPUT, OUTPUT, RESIZE, ERROR
}
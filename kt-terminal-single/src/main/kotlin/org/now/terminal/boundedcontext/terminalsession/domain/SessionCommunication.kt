package org.now.terminal.boundedcontext.terminalsession.domain

import org.now.terminal.kernel.communication.TerminalMessage

/**
 * 会话通讯接口
 */
interface SessionCommunication {
    suspend fun sendMessage(message: TerminalMessage)
    suspend fun receiveMessage(): TerminalMessage?
    suspend fun close()
    val isConnected: Boolean
}
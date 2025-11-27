package org.now.terminal.boundedcontext.terminalsession.domain

/**
 * 终端接口 - 协议无关的终端操作抽象
 */
interface Terminal {
    suspend fun write(data: String)
    suspend fun resize(columns: Int, rows: Int)
    suspend fun start(session: TerminalSession, shell: String)
    suspend fun close()
    val isRunning: Boolean
}
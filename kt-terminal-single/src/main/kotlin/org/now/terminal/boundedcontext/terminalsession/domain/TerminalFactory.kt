package org.now.terminal.boundedcontext.terminalsession.domain

/**
 * 终端工厂接口
 */
interface TerminalFactory {
    fun createTerminal(columns: Int, rows: Int): Terminal
}
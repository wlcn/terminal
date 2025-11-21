package org.now.terminal.session.domain

import org.now.terminal.session.domain.valueobjects.TerminalSize

/**
 * 进程接口 - 定义终端进程的基本操作
 */
interface Process {
    
    /**
     * 写入输入到进程
     */
    fun writeInput(input: String)
    
    /**
     * 读取进程输出
     */
    fun readOutput(): String
    
    /**
     * 调整终端尺寸
     */
    fun resize(size: TerminalSize)
    
    /**
     * 终止进程
     */
    fun terminate()
    
    /**
     * 检查进程是否存活
     */
    fun isAlive(): Boolean
    
    /**
     * 获取进程退出码
     */
    fun getExitCode(): Int?
}
package org.now.terminal.session.domain.ports

import org.now.terminal.session.domain.aggregates.PtyConfiguration
import org.now.terminal.session.domain.entities.TerminalProcess
import org.now.terminal.session.domain.valueobjects.TerminalCommand
import org.now.terminal.session.domain.valueobjects.TerminalSize

/**
 * 进程管理器端口接口
 * 定义进程管理的外部依赖，遵循依赖倒置原则
 */
interface ProcessManagerPort {
    
    /**
     * 创建新的终端进程
     */
    fun createProcess(configuration: PtyConfiguration): TerminalProcess
    
    /**
     * 执行终端命令
     */
    fun executeCommand(process: TerminalProcess, command: TerminalCommand)
    
    /**
     * 调整终端尺寸
     */
    fun resizeTerminal(process: TerminalProcess, newSize: TerminalSize)
    
    /**
     * 终止进程
     */
    fun terminateProcess(process: TerminalProcess)
    
    /**
     * 检查进程是否存活
     */
    fun isProcessAlive(process: TerminalProcess): Boolean
    
    /**
     * 获取进程输出
     */
    fun getProcessOutput(process: TerminalProcess): String
    
    /**
     * 清理进程资源
     */
    fun cleanupProcess(process: TerminalProcess)
    
    /**
     * 获取进程统计信息
     */
    fun getProcessStats(process: TerminalProcess): ProcessStats
}

/**
 * 进程统计信息数据类
 */
data class ProcessStats(
    val cpuUsage: Double,
    val memoryUsage: Long,
    val uptime: Long,
    val outputLines: Int
)
package org.now.terminal.session.domain.valueobjects

import kotlinx.serialization.Serializable

/**
 * Shell类型枚举
 * 用于指定PTY进程应该使用哪种shell
 */
@Serializable
enum class ShellType {
    /**
     * 自动检测 - 根据命令内容自动选择合适的shell
     */
    AUTO,
    
    /**
     * Unix shell (sh, bash, zsh等)
     */
    UNIX,
    
    /**
     * Windows命令提示符 (cmd.exe)
     */
    WINDOWS_CMD,
    
    /**
     * Windows PowerShell
     */
    WINDOWS_POWERSHELL,
    
    /**
     * 直接执行命令，不通过shell包装
     */
    DIRECT
}
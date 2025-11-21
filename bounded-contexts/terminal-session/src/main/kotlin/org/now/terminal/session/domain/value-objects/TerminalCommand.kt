package org.now.terminal.session.domain.valueobjects

import kotlinx.serialization.Serializable

/**
 * 终端命令值对象
 * 表示一个终端命令，包含验证逻辑
 */
@JvmInline
@Serializable
value class TerminalCommand private constructor(val value: String) {
    companion object {
        /**
         * 创建终端命令
         */
        fun create(command: String): TerminalCommand {
            require(command.isNotBlank()) { "Command cannot be blank" }
            require(command.length <= 1024) { "Command too long" }
            return TerminalCommand(command.trim())
        }
    }
    
    /**
     * 检查命令是否有效
     */
    fun isValid(): Boolean = value.isNotBlank() && value.length <= 1024
    
    /**
     * 检查是否为危险命令
     */
    fun isDangerous(): Boolean {
        val dangerousCommands = listOf("rm -rf", "dd if=", "mkfs", ":(){ :|:& };:")
        return dangerousCommands.any { value.contains(it, ignoreCase = true) }
    }
    
    override fun toString(): String = value
}
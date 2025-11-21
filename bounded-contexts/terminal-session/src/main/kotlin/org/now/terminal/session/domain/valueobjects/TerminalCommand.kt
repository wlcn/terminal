package org.now.terminal.session.domain.valueobjects

import kotlinx.serialization.Serializable

/**
 * 终端命令值对象
 */
@JvmInline
@Serializable
value class TerminalCommand(val value: String) {
    init {
        require(value.isNotBlank()) { "Command cannot be blank" }
        require(value.length <= 1024) { "Command too long" }
    }
    
    companion object {
        fun fromString(value: String): TerminalCommand = TerminalCommand(value.trim())
    }
}
package org.now.terminal.boundedcontext.terminalsession.domain.valueobjects

import kotlinx.serialization.Serializable

/**
 * Shell Type Value Object
 */
@Serializable
enum class ShellType {
    BASH,
    ZSH,
    FISH,
    POWERSHELL,
    CMD;

    companion object {
        fun fromString(value: String): ShellType {
            return valueOf(value.uppercase())
        }
        
        fun isValidShell(shell: String): Boolean {
            return values().any { it.name.equals(shell, ignoreCase = true) }
        }
    }
}
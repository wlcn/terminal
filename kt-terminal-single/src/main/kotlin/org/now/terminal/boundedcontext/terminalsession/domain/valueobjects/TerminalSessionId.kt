package org.now.terminal.boundedcontext.terminalsession.domain.valueobjects

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Terminal Session ID Value Object
 */
@Serializable
@JvmInline
value class TerminalSessionId(val value: String) {
    init {
        require(value.isNotBlank()) { "Terminal Session ID cannot be blank" }
        require(value.startsWith("ts_")) { "Terminal Session ID must start with 'ts_' prefix" }
    }

    companion object {
        /**
         * Generate a new unique terminal session ID
         */
        fun generate(): TerminalSessionId {
            return TerminalSessionId("ts_${UUID.randomUUID()}")
        }
    }

    override fun toString(): String = value
}
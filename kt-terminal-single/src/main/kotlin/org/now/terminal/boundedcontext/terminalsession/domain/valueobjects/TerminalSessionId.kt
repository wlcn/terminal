package org.now.terminal.boundedcontext.terminalsession.domain.valueobjects

import kotlinx.serialization.Serializable

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

    override fun toString(): String = value
}
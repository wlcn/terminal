package org.now.terminal.boundedcontext.terminalsession.domain

/**
 * 会话ID值对象
 */
@JvmInline
value class SessionId(val value: String) {
    init {
        require(value.isNotBlank()) { "Session ID cannot be blank" }
    }
}
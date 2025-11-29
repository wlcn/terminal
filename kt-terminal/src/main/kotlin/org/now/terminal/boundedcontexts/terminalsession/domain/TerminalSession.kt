package org.now.terminal.boundedcontexts.terminalsession.domain

import kotlinx.serialization.Serializable

@Serializable
class TerminalSession(
    val id: String,
    val userId: String,
    val title: String?,
    val workingDirectory: String,
    val shellType: String,
    var status: TerminalSessionStatus,
    var terminalSize: TerminalSize = TerminalSize(80, 24),
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    var lastActiveTime: Long = System.currentTimeMillis(),
    var expiredAt: Long? = null
)

@Serializable
enum class TerminalSessionStatus {
    ACTIVE,
    INACTIVE,
    TERMINATED
}

@Serializable
class TerminalSize(
    val columns: Int,
    val rows: Int
)

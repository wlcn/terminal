package org.now.terminal.boundedcontexts.terminalsession.domain.model

import java.util.UUID

class TerminalSession(
    val id: UUID,
    val userId: String,
    val title: String?,
    val workingDirectory: String,
    val shellType: String,
    var status: TerminalSessionStatus,
    var terminalSize: TerminalSize = TerminalSize(80, 24),
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
)

enum class TerminalSessionStatus {
    ACTIVE,
    INACTIVE,
    TERMINATED
}

class TerminalSize(
    val columns: Int,
    val rows: Int
)

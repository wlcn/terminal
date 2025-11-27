package org.now.terminal.boundedcontexts.terminalsession.domain.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
class TerminalSession(
    @Contextual val id: UUID,
    val userId: String,
    val title: String?,
    val workingDirectory: String,
    val shellType: String,
    var status: TerminalSessionStatus,
    var terminalSize: TerminalSize = TerminalSize(80, 24),
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
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

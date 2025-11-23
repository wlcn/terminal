package org.now.terminal.server.api.models

import kotlinx.serialization.Serializable
import org.now.terminal.session.domain.valueobjects.ShellType
import org.now.terminal.session.domain.valueobjects.TerminalSize

/**
 * 创建会话API响应模型
 */
@Serializable
data class CreateSessionResponse(
    val sessionId: String,
    val status: String,
    val shellType: ShellType,
    val terminalSize: TerminalSize = TerminalSize(80, 24)
)
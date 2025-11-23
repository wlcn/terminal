package org.now.terminal.server.api.models

import kotlinx.serialization.Serializable
import org.now.terminal.session.domain.valueobjects.TerminalSize

/**
 * 调整终端尺寸API响应模型
 */
@Serializable
data class ResizeTerminalResponse(
    val sessionId: String,
    val terminalSize: TerminalSize,
    val status: String
)
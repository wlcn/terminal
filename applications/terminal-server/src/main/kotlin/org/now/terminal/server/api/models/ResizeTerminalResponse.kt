package org.now.terminal.server.api.models

import kotlinx.serialization.Serializable

/**
 * 调整终端尺寸API响应模型
 */
@Serializable
data class ResizeTerminalResponse(
    val sessionId: String,
    val columns: Int,
    val rows: Int,
    val status: String
)
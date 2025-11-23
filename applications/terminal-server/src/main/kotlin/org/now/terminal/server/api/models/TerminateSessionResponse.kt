package org.now.terminal.server.api.models

import kotlinx.serialization.Serializable

/**
 * 终止会话API响应模型
 */
@Serializable
data class TerminateSessionResponse(
    val sessionId: String,
    val reason: String,
    val status: String
)
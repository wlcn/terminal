package org.now.terminal.server.api.models

import kotlinx.serialization.Serializable

/**
 * 创建会话API响应模型
 */
@Serializable
data class CreateSessionResponse(
    val sessionId: String,
    val status: String
)
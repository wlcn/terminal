package org.now.terminal.server.api.models

import kotlinx.serialization.Serializable

/**
 * 会话列表API响应模型
 */
@Serializable
data class SessionListResponse(
    val sessions: List<String>,
    val count: Int
)
package org.now.terminal.server.api.models

import kotlinx.serialization.Serializable

/**
 * 错误响应模型
 */
@Serializable
data class ErrorResponse(
    val error: String
)
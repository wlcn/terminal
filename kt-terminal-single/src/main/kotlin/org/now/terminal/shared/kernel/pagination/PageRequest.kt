package org.now.terminal.shared.kernel.pagination

import kotlinx.serialization.Serializable

/**
 * 统一分页请求参数
 */
@Serializable
sealed class PageRequest {
    abstract val pageSize: Int
    
    /**
     * 基于游标的分页请求
     */
    @Serializable
    data class CursorBased(
        val cursor: String? = null,
        override val pageSize: Int = 20
    ) : PageRequest()
    
    /**
     * 基于偏移量的分页请求
     */
    @Serializable
    data class OffsetBased(
        val pageNumber: Int = 0,
        override val pageSize: Int = 20
    ) : PageRequest()
}
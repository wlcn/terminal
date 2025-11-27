package org.now.terminal.shared.kernel.pagination

import kotlinx.serialization.Serializable

/**
 * Unified pagination request parameters
 */
@Serializable
sealed class PageRequest {
    abstract val pageSize: Int
    
    /**
     * Cursor-based pagination request
     */
    @Serializable
    data class CursorBased(
        val cursor: String? = null,
        override val pageSize: Int = 20
    ) : PageRequest()
    
    /**
     * Offset-based pagination request
     */
    @Serializable
    data class OffsetBased(
        val pageNumber: Int = 0,
        override val pageSize: Int = 20
    ) : PageRequest()
}
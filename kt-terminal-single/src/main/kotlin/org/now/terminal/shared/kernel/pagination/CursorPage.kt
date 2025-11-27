package org.now.terminal.shared.kernel.pagination

import kotlinx.serialization.Serializable

/**
 * 基于游标的分页结果
 * 适用于无限滚动和性能敏感场景
 */
@Serializable
data class CursorPage<T>(
    val items: List<T>,
    val nextCursor: String?,
    val hasNext: Boolean,
    val totalCount: Long? = null
) {
    companion object {
        fun <T> of(
            items: List<T>,
            nextCursor: String?,
            hasNext: Boolean = nextCursor != null,
            totalCount: Long? = null
        ): CursorPage<T> {
            return CursorPage(items, nextCursor, hasNext, totalCount)
        }
    }
}
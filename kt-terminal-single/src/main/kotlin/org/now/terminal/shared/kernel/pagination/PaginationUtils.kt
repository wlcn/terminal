package org.now.terminal.shared.kernel.pagination

/**
 * 分页工具类
 */
object PaginationUtils {
    
    /**
     * 生成游标（基于时间戳和ID）
     */
    fun generateCursor(timestamp: Long, id: String): String {
        return "${timestamp}_$id"
    }
    
    /**
     * 解析游标
     */
    fun parseCursor(cursor: String): Pair<Long, String>? {
        return try {
            val parts = cursor.split("_")
            if (parts.size == 2) {
                parts[0].toLong() to parts[1]
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 验证分页参数
     */
    fun validatePageRequest(request: PageRequest): Boolean {
        return when (request) {
            is PageRequest.CursorBased -> request.pageSize in 1..100
            is PageRequest.OffsetBased -> request.pageNumber >= 0 && request.pageSize in 1..100
        }
    }
    
    /**
     * 计算总页数
     */
    fun calculateTotalPages(totalElements: Long, pageSize: Int): Int {
        return if (pageSize > 0) {
            ((totalElements + pageSize - 1) / pageSize).toInt()
        } else {
            0
        }
    }
}
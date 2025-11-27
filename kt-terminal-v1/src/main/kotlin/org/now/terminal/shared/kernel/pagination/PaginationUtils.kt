package org.now.terminal.shared.kernel.pagination

/**
 * Pagination utility class
 */
object PaginationUtils {
    
    /**
     * Generate cursor (based on timestamp and ID)
     */
    fun generateCursor(timestamp: Long, id: String): String {
        return "${timestamp}_$id"
    }
    
    /**
     * Parse cursor
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
     * Validate pagination parameters
     */
    fun validatePageRequest(request: PageRequest): Boolean {
        return when (request) {
            is PageRequest.CursorBased -> request.pageSize in 1..100
            is PageRequest.OffsetBased -> request.pageNumber >= 0 && request.pageSize in 1..100
        }
    }
    
    /**
     * Calculate total pages
     */
    fun calculateTotalPages(totalElements: Long, pageSize: Int): Int {
        return if (pageSize > 0) {
            ((totalElements + pageSize - 1) / pageSize).toInt()
        } else {
            0
        }
    }
}
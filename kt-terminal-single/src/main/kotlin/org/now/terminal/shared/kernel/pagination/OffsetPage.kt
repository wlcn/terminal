package org.now.terminal.shared.kernel.pagination

import kotlinx.serialization.Serializable

/**
 * 基于偏移量的分页结果
 * 传统但稳定的实现，适用于需要跳页的场景
 */
@Serializable
data class OffsetPage<T>(
    val content: List<T>,
    val pageNumber: Int,
    val pageSize: Int,
    val totalElements: Long,
    val totalPages: Int
) {
    
    val hasPrevious: Boolean
        get() = pageNumber > 0
    
    val hasNext: Boolean
        get() = pageNumber < totalPages - 1
    
    val isFirst: Boolean
        get() = pageNumber == 0
    
    val isLast: Boolean
        get() = pageNumber == totalPages - 1
    
    val offset: Int
        get() = pageNumber * pageSize
    
    companion object {
        fun <T> empty(pageNumber: Int = 0, pageSize: Int = 20): OffsetPage<T> {
            return OffsetPage(
                content = emptyList(),
                pageNumber = pageNumber,
                pageSize = pageSize,
                totalElements = 0,
                totalPages = 0
            )
        }
        
        fun <T> of(
            content: List<T>,
            pageNumber: Int,
            pageSize: Int,
            totalElements: Long
        ): OffsetPage<T> {
            val totalPages = if (pageSize > 0) {
                (totalElements + pageSize - 1) / pageSize
            } else {
                0
            }
            
            return OffsetPage(
                content = content,
                pageNumber = pageNumber,
                pageSize = pageSize,
                totalElements = totalElements,
                totalPages = totalPages.toInt()
            )
        }
    }
}
package org.now.terminal.interfaces.web.responses

/**
 * Generic API response interface
 * Defines unified API response format, implementations are placed in infrastructure layer
 */
interface ApiResponse<T> {
    val success: Boolean
    val code: String
    val message: String
    val data: T?
    val timestamp: Long
}

/**
 * Paginated response interface
 */
interface PageResponse<T> : ApiResponse<List<T>> {
    val page: Int
    val size: Int
    val total: Long
    val totalPages: Int
}

/**
 * Error response interface
 */
interface ErrorResponse : ApiResponse<Nothing> {
    val errorCode: String
    val details: Map<String, Any>?
}
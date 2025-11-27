package org.now.terminal.infrastructure.web.responses

import kotlinx.serialization.Serializable
import org.now.terminal.interfaces.web.responses.ApiResponse
import org.now.terminal.interfaces.web.responses.ErrorResponse
import org.now.terminal.interfaces.web.responses.PageResponse

/**
 * Generic API response implementation
 */
@Serializable
data class ApiResponseImpl<T>(
    override val success: Boolean,
    override val code: String,
    override val message: String,
    override val data: T?,
    override val timestamp: Long = System.currentTimeMillis()
) : ApiResponse<T> {

    companion object {
        
        /**
         * Create success response
         */
        fun <T> success(
            data: T? = null,
            message: String = "Success",
            code: String = "200"
        ): ApiResponseImpl<T> {
            return ApiResponseImpl(
                success = true,
                code = code,
                message = message,
                data = data
            )
        }
        
        /**
         * Create error response
         */
        fun <T> error(
            message: String = "Error",
            code: String = "500",
            data: T? = null
        ): ApiResponseImpl<T> {
            return ApiResponseImpl(
                success = false,
                code = code,
                message = message,
                data = data
            )
        }
    }
}

/**
 * Paginated response implementation
 */
@Serializable
data class PageResponseImpl<T>(
    override val success: Boolean,
    override val code: String,
    override val message: String,
    override val data: List<T>?,
    override val timestamp: Long = System.currentTimeMillis(),
    override val page: Int,
    override val size: Int,
    override val total: Long,
    override val totalPages: Int
) : PageResponse<T> {

    companion object {
        
        /**
         * Create paginated success response
         */
        fun <T> success(
            data: List<T>,
            page: Int,
            size: Int,
            total: Long,
            message: String = "Success",
            code: String = "200"
        ): PageResponseImpl<T> {
            val totalPages = if (size > 0) Math.ceil(total.toDouble() / size).toInt() else 0
            
            return PageResponseImpl(
                success = true,
                code = code,
                message = message,
                data = data,
                page = page,
                size = size,
                total = total,
                totalPages = totalPages
            )
        }
    }
}

/**
 * Error response implementation
 */
@Serializable
data class ErrorResponseImpl(
    override val success: Boolean,
    override val code: String,
    override val message: String,
    override val timestamp: Long = System.currentTimeMillis(),
    override val errorCode: String,
    override val details: Map<String, @kotlinx.serialization.Contextual Any>? = null
) : ErrorResponse {
    
    override val data: Nothing? = null

    companion object {
        
        /**
         * Create error response
         */
        fun create(
            message: String,
            errorCode: String,
            code: String = "500",
            details: Map<String, @kotlinx.serialization.Contextual Any>? = null
        ): ErrorResponseImpl {
            return ErrorResponseImpl(
                success = false,
                code = code,
                message = message,
                errorCode = errorCode,
                details = details
            )
        }
    }
}
package org.now.terminal.shared.exceptions

/**
 * 领域异常数据类 - 完全避免继承，使用数据类组合
 * 每个异常都是独立的数据类，通过工厂方法创建
 */
data class DomainException(
    val code: String,
    val message: String,
    val cause: Throwable? = null,
    val context: Map<String, Any> = emptyMap()
) : RuntimeException(message, cause)

/**
 * 异常类型枚举 - 定义所有可能的异常类型和错误码
 */
enum class ExceptionType(val category: String, val errorCode: String) {
    // 验证相关异常
    VALIDATION_ERROR("validation", "VAL_001"),
    INVALID_USER_ID("validation", "VAL_002"),
    INVALID_SESSION_ID("validation", "VAL_003"),
    INVALID_TERMINAL_SIZE("validation", "VAL_004"),
    INVALID_EVENT_ID("validation", "VAL_005"),
    
    // 业务规则相关异常
    BUSINESS_RULE_VIOLATION("business", "BUS_001"),
    PERMISSION_DENIED("business", "BUS_002"),
    SESSION_ALREADY_EXISTS("business", "BUS_003"),
    
    // 资源相关异常
    RESOURCE_NOT_FOUND("resource", "RES_001"),
    USER_NOT_FOUND("resource", "RES_002"),
    SESSION_NOT_FOUND("resource", "RES_003"),
    
    // 系统相关异常
    SYSTEM_UNAVAILABLE("system", "SYS_001"),
    COMMAND_EXECUTION_FAILED("system", "SYS_002"),
    COMMAND_TIMEOUT("system", "SYS_003"),
    
    // 并发相关异常
    CONCURRENCY_ERROR("concurrency", "CON_001")
}

/**
 * 异常工厂 - 提供便捷的异常创建方法，完全避免继承
 */
object DomainExceptionFactory {
    
    // 验证异常
    fun invalidUserId(userId: String, cause: Throwable? = null): DomainException =
        DomainException(
            code = ExceptionType.INVALID_USER_ID.errorCode,
            message = "Invalid user ID: $userId",
            cause = cause,
            context = mapOf("userId" to userId, "type" to "validation")
        )
    
    fun invalidSessionId(sessionId: String, cause: Throwable? = null): DomainException =
        DomainException(
            code = ExceptionType.INVALID_SESSION_ID.errorCode,
            message = "Invalid session ID: $sessionId",
            cause = cause,
            context = mapOf("sessionId" to sessionId, "type" to "validation")
        )
    
    fun invalidTerminalSize(rows: Int, columns: Int, cause: Throwable? = null): DomainException =
        DomainException(
            code = ExceptionType.INVALID_TERMINAL_SIZE.errorCode,
            message = "Invalid terminal size: ${rows}x$columns",
            cause = cause,
            context = mapOf("rows" to rows, "columns" to columns, "type" to "validation")
        )
    
    fun invalidEventId(eventId: String, cause: Throwable? = null): DomainException =
        DomainException(
            code = ExceptionType.INVALID_EVENT_ID.errorCode,
            message = "Invalid event ID: $eventId",
            cause = cause,
            context = mapOf("eventId" to eventId, "type" to "validation")
        )
    
    // 资源异常
    fun sessionNotFound(sessionId: String, cause: Throwable? = null): DomainException =
        DomainException(
            code = ExceptionType.SESSION_NOT_FOUND.errorCode,
            message = "Session not found: $sessionId",
            cause = cause,
            context = mapOf("sessionId" to sessionId, "resourceType" to "Session", "type" to "resource")
        )
    
    fun userNotFound(userId: String, cause: Throwable? = null): DomainException =
        DomainException(
            code = ExceptionType.USER_NOT_FOUND.errorCode,
            message = "User not found: $userId",
            cause = cause,
            context = mapOf("userId" to userId, "resourceType" to "User", "type" to "resource")
        )
    
    fun resourceNotFound(resourceType: String, resourceId: String, cause: Throwable? = null): DomainException =
        DomainException(
            code = ExceptionType.RESOURCE_NOT_FOUND.errorCode,
            message = "$resourceType not found: $resourceId",
            cause = cause,
            context = mapOf("resourceType" to resourceType, "resourceId" to resourceId, "type" to "resource")
        )
    
    // 业务规则异常
    fun permissionDenied(userId: String, sessionId: String, cause: Throwable? = null): DomainException =
        DomainException(
            code = ExceptionType.PERMISSION_DENIED.errorCode,
            message = "Permission denied for user $userId on session $sessionId",
            cause = cause,
            context = mapOf("userId" to userId, "sessionId" to sessionId, "type" to "business")
        )
    
    fun sessionAlreadyExists(sessionId: String, cause: Throwable? = null): DomainException =
        DomainException(
            code = ExceptionType.SESSION_ALREADY_EXISTS.errorCode,
            message = "Session already exists: $sessionId",
            cause = cause,
            context = mapOf("sessionId" to sessionId, "type" to "business")
        )
    
    fun businessRuleViolation(ruleName: String, details: String, cause: Throwable? = null): DomainException =
        DomainException(
            code = ExceptionType.BUSINESS_RULE_VIOLATION.errorCode,
            message = "Business rule '$ruleName' violated: $details",
            cause = cause,
            context = mapOf("ruleName" to ruleName, "details" to details, "type" to "business")
        )
    
    // 系统异常
    fun commandExecutionFailed(command: String, error: String, cause: Throwable? = null): DomainException =
        DomainException(
            code = ExceptionType.COMMAND_EXECUTION_FAILED.errorCode,
            message = "Command execution failed: $command - $error",
            cause = cause,
            context = mapOf("command" to command, "error" to error, "type" to "system")
        )
    
    fun commandTimeout(command: String, timeoutMs: Long, cause: Throwable? = null): DomainException =
        DomainException(
            code = ExceptionType.COMMAND_TIMEOUT.errorCode,
            message = "Command timeout: $command (timeout: ${timeoutMs}ms)",
            cause = cause,
            context = mapOf("command" to command, "timeoutMs" to timeoutMs, "type" to "system")
        )
    
    fun systemUnavailable(component: String, cause: Throwable? = null): DomainException =
        DomainException(
            code = ExceptionType.SYSTEM_UNAVAILABLE.errorCode,
            message = "System unavailable: $component",
            cause = cause,
            context = mapOf("component" to component, "type" to "system")
        )
    
    // 并发异常
    fun concurrencyError(resourceId: String, expectedVersion: Long? = null, actualVersion: Long? = null, cause: Throwable? = null): DomainException =
        DomainException(
            code = ExceptionType.CONCURRENCY_ERROR.errorCode,
            message = "Concurrency error for resource $resourceId",
            cause = cause,
            context = mapOf("resourceId" to resourceId, "expectedVersion" to expectedVersion, "actualVersion" to actualVersion, "type" to "concurrency")
        )
    
    // 通用验证异常
    fun validationError(field: String, value: Any?, reason: String, cause: Throwable? = null): DomainException =
        DomainException(
            code = ExceptionType.VALIDATION_ERROR.errorCode,
            message = "Validation error for field '$field': $reason",
            cause = cause,
            context = mapOf("field" to field, "value" to value, "reason" to reason, "type" to "validation")
        )
}

/**
 * 异常扩展函数 - 提供便捷的异常处理功能
 */
fun DomainException.isValidationError(): Boolean = context["type"] == "validation"
fun DomainException.isBusinessError(): Boolean = context["type"] == "business"
fun DomainException.isResourceError(): Boolean = context["type"] == "resource"
fun DomainException.isSystemError(): Boolean = context["type"] == "system"
fun DomainException.isConcurrencyError(): Boolean = context["type"] == "concurrency"

fun DomainException.getUserId(): String? = context["userId"] as? String
fun DomainException.getSessionId(): String? = context["sessionId"] as? String
fun DomainException.getResourceType(): String? = context["resourceType"] as? String
fun DomainException.getResourceId(): String? = context["resourceId"] as? String
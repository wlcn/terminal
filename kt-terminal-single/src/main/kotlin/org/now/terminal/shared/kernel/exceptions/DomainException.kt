package org.now.terminal.shared.kernel.exceptions

/**
 * 领域异常基类
 */
open class DomainException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * 验证异常
 */
class ValidationException(
    message: String,
    val field: String? = null,
    cause: Throwable? = null
) : DomainException(message, cause)

/**
 * 未找到异常
 */
class NotFoundException(
    message: String,
    val resource: String? = null,
    cause: Throwable? = null
) : DomainException(message, cause)

/**
 * 已存在异常
 */
class AlreadyExistsException(
    message: String,
    val resource: String? = null,
    cause: Throwable? = null
) : DomainException(message, cause)

/**
 * 业务规则异常
 */
class BusinessRuleException(
    message: String,
    val rule: String? = null,
    cause: Throwable? = null
) : DomainException(message, cause)
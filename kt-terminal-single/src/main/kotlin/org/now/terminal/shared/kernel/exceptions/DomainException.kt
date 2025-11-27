package org.now.terminal.shared.kernel.exceptions

/**
 * Base class for domain exceptions
 */
open class DomainException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * Validation exception
 */
class ValidationException(
    message: String,
    val field: String? = null,
    cause: Throwable? = null
) : DomainException(message, cause)

/**
 * Not found exception
 */
class NotFoundException(
    message: String,
    val resource: String? = null,
    cause: Throwable? = null
) : DomainException(message, cause)

/**
 * Already exists exception
 */
class AlreadyExistsException(
    message: String,
    val resource: String? = null,
    cause: Throwable? = null
) : DomainException(message, cause)

/**
 * Business rule exception
 */
class BusinessRuleException(
    message: String,
    val rule: String? = null,
    cause: Throwable? = null
) : DomainException(message, cause)
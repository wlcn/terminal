package org.now.terminal.infrastructure.eventbus

import org.now.terminal.shared.exceptions.DomainException

/**
 * 事件总线异常密封类
 * 封装所有与事件总线相关的异常类型
 */
sealed class EventBusException(
    val code: String,
    override val message: String,
    override val cause: Throwable? = null,
    val context: Map<String, Any> = emptyMap()
) : RuntimeException(message, cause)

/**
 * 事件发布异常
 */
data class EventPublishException(
    val eventType: String,
    val publisherId: String,
    override val message: String,
    override val cause: Throwable? = null
) : EventBusException(
    code = "EVENT_PUBLISH_ERROR",
    message = "Failed to publish event of type '$eventType' by publisher '$publisherId': $message",
    cause = cause,
    context = mapOf(
        "eventType" to eventType,
        "publisherId" to publisherId
    )
)

/**
 * 事件处理异常
 */
data class EventHandlingException(
    val eventType: String,
    val handlerId: String,
    override val message: String,
    override val cause: Throwable? = null
) : EventBusException(
    code = "EVENT_HANDLING_ERROR",
    message = "Failed to handle event of type '$eventType' by handler '$handlerId': $message",
    cause = cause,
    context = mapOf(
        "eventType" to eventType,
        "handlerId" to handlerId
    )
)

/**
 * 事件订阅异常
 */
data class EventSubscriptionException(
    val eventType: String,
    val subscriberId: String,
    override val message: String,
    override val cause: Throwable? = null
) : EventBusException(
    code = "EVENT_SUBSCRIPTION_ERROR",
    message = "Failed to subscribe to event of type '$eventType' by subscriber '$subscriberId': $message",
    cause = cause,
    context = mapOf(
        "eventType" to eventType,
        "subscriberId" to subscriberId
    )
)
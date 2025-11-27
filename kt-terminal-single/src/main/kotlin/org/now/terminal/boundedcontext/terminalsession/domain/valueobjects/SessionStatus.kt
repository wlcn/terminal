package org.now.terminal.boundedcontext.terminalsession.domain.valueobjects

import kotlinx.serialization.Serializable

/**
 * Session Status Value Object
 */
@Serializable
enum class SessionStatus {
    ACTIVE,
    INACTIVE,
    TERMINATED,
    ERROR;

    val isActive: Boolean
        get() = this == ACTIVE

    val canBeTerminated: Boolean
        get() = this == ACTIVE || this == INACTIVE
}
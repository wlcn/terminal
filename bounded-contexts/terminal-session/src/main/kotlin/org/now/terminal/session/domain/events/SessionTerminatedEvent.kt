package org.now.terminal.session.domain.events

import org.now.terminal.shared.events.Event
import org.now.terminal.shared.events.EventHelper
import org.now.terminal.shared.valueobjects.SessionId
import org.now.terminal.session.domain.valueobjects.TerminationReason
import java.time.Instant

/**
 * 会话终止事件
 */
data class SessionTerminatedEvent(
    override val eventHelper: EventHelper,
    val sessionId: SessionId,
    val reason: TerminationReason,
    val exitCode: Int?,
    val terminatedAt: Instant
) : Event
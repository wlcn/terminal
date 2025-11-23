package org.now.terminal.session.domain.events

import org.now.terminal.shared.events.Event
import org.now.terminal.shared.events.EventHelper
import org.now.terminal.shared.valueobjects.SessionId
import org.now.terminal.shared.valueobjects.UserId
import org.now.terminal.session.domain.valueobjects.PtyConfiguration
import java.time.Instant

/**
 * 会话创建事件
 */
data class SessionCreatedEvent(
    override val eventHelper: EventHelper,
    override val sessionId: SessionId,
    val userId: UserId,
    val configuration: PtyConfiguration,
    val createdAt: Instant
) : Event
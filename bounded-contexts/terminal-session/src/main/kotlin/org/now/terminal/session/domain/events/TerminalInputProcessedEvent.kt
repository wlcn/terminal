package org.now.terminal.session.domain.events

import org.now.terminal.shared.events.Event
import org.now.terminal.shared.events.EventHelper
import org.now.terminal.shared.valueobjects.SessionId
import java.time.Instant

/**
 * 终端输入处理事件
 */
data class TerminalInputProcessedEvent(
    override val eventHelper: EventHelper,
    override val sessionId: SessionId,
    val input: String,
    val processedAt: Instant
) : Event
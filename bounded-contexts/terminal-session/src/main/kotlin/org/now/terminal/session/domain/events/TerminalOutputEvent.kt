package org.now.terminal.session.domain.events

import org.now.terminal.shared.events.Event
import org.now.terminal.shared.events.EventHelper
import org.now.terminal.shared.valueobjects.SessionId
import java.time.Instant

/**
 * 终端输出事件
 */
data class TerminalOutputEvent(
    override val eventHelper: EventHelper,
    val sessionId: SessionId,
    val output: String,
    val outputAt: Instant
) : Event
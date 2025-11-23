package org.now.terminal.session.domain.events

import org.now.terminal.shared.events.Event
import org.now.terminal.shared.events.EventHelper
import org.now.terminal.shared.valueobjects.SessionId
import java.time.Instant

/**
 * 终端调整大小事件
 */
data class TerminalResizedEvent(
    override val eventHelper: EventHelper,
    override val sessionId: SessionId,
    val columns: Int,
    val rows: Int,
    val resizedAt: Instant
) : Event
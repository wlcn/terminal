package org.now.terminal.infrastructure.eventbus

import org.now.terminal.shared.events.Event
import org.now.terminal.shared.events.EventHelper
import org.now.terminal.shared.valueobjects.EventId
import java.time.Instant

/**
 * 测试事件 - 用于事件总线测试
 * 注意：这个类只在测试包中使用，生产代码不应该依赖它
 */
data class TestEvent(
    override val eventHelper: EventHelper = EventHelper(
        eventId = EventId.generate(),
        occurredAt = Instant.now(),
        eventType = "TestEvent",
        aggregateId = null,
        aggregateType = null,
        version = 1
    ),
    val testData: String = "test-data"
) : Event
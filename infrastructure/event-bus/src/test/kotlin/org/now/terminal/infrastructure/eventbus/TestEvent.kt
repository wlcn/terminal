package org.now.terminal.infrastructure.eventbus

import kotlinx.serialization.Serializable
import java.time.Instant
import org.now.terminal.infrastructure.eventbus.Event

/**
 * 测试事件 - 用于事件总线测试
 * 注意：这个类只在测试包中使用，生产代码不应该依赖它
 */
@Serializable
data class TestEvent(
    override val eventId: String = "test-event-" + System.currentTimeMillis(),
    override val occurredAt: Instant = Instant.now(),
    override val eventType: String = "TestEvent",
    override val aggregateId: String? = null,
    override val aggregateType: String? = null,
    override val version: Int = 1,
    val testData: String = "test-data"
) : Event
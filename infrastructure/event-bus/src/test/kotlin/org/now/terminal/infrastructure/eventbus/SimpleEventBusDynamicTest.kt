package org.now.terminal.infrastructure.eventbus

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.*
import org.now.terminal.shared.events.Event
import org.now.terminal.shared.events.EventHandler
import org.now.terminal.shared.events.EventHelper
import org.now.terminal.shared.valueobjects.EventId
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

/**
 * 测试事件总线的动态注册和监听功能
 */
class SimpleEventBusDynamicTest : BehaviorSpec({
    
    lateinit var eventBus: SimpleEventBus
    
    beforeTest {
        eventBus = SimpleEventBus()
    }
    
    afterTest {
        if (eventBus.isRunning()) {
            eventBus.stop()
        }
    }
    
    given("一个简单的事件总线") {
        `when`("发布和订阅事件") {
            then("应该能够正确接收事件") {
                runBlocking {
                    val testEvent = DynamicTestEvent("Test content")
                    var receivedEvent: DynamicTestEvent? = null
                    
                    // 注册事件处理器
                    eventBus.subscribe(DynamicTestEvent::class.java, object : EventHandler<DynamicTestEvent> {
                        override suspend fun handle(event: DynamicTestEvent) {
                            receivedEvent = event
                        }
                        
                        override fun canHandle(eventType: String): Boolean = eventType == "dynamic-test-event"
                    })
                    
                    // 启动事件总线
                    eventBus.start()
                    
                    // 发布事件
                    eventBus.publish(testEvent)
                    
                    // 等待事件处理完成
                    delay(100)
                    
                    receivedEvent shouldNotBe null
                    receivedEvent!!.eventType shouldBe "dynamic-test-event"
                    receivedEvent!!.content shouldBe "Test content"
                }
            }
        }
        
        `when`("动态注册事件处理器") {
            then("新注册的处理器应该能够接收后续事件") {
                runBlocking {
                    val eventCount = AtomicInteger(0)
                    
                    // 启动事件总线
                    eventBus.start()
                    
                    // 先发布一个事件（应该没有处理器接收）
                    eventBus.publish(DynamicTestEvent("First event"))
                    delay(50)
                    
                    // 动态注册处理器
                    eventBus.subscribe(DynamicTestEvent::class.java, object : EventHandler<DynamicTestEvent> {
                        override suspend fun handle(event: DynamicTestEvent) {
                            eventCount.incrementAndGet()
                        }
                        
                        override fun canHandle(eventType: String): Boolean = eventType == "dynamic-test-event"
                    })
                    
                    // 等待处理器注册完成
                    delay(50)
                    
                    // 重新发布事件（新注册的处理器应该能够接收）
                    eventBus.publish(DynamicTestEvent("Second event"))
                    delay(200) // 增加等待时间确保事件被处理
                    
                    eventCount.get() shouldBe 1
                }
            }
        }
        
        `when`("动态取消注册事件处理器") {
            then("取消注册后处理器不应该接收后续事件") {
                runBlocking {
                    val eventCount = AtomicInteger(0)
                    
                    val handler = object : EventHandler<DynamicTestEvent> {
                        override suspend fun handle(event: DynamicTestEvent) {
                            eventCount.incrementAndGet()
                        }
                        
                        override fun canHandle(eventType: String): Boolean = eventType == "dynamic-test-event"
                    }
                    
                    // 注册处理器并启动事件总线
                    eventBus.subscribe(DynamicTestEvent::class.java, handler)
                    eventBus.start()
                    
                    // 发布第一个事件
                    eventBus.publish(DynamicTestEvent("First event"))
                    delay(50)
                    
                    // 取消注册处理器
                    eventBus.unsubscribe(DynamicTestEvent::class.java, handler)
                    
                    // 发布第二个事件（应该没有处理器接收）
                    eventBus.publish(DynamicTestEvent("Second event"))
                    delay(50)
                    
                    eventCount.get() shouldBe 1
                }
            }
        }
        
        `when`("处理多个事件类型") {
            then("不同类型的事件应该由各自的处理器处理") {
                runBlocking {
                    val testEvent1Count = AtomicInteger(0)
                    val testEvent2Count = AtomicInteger(0)
                    
                    // 注册两个不同类型的事件处理器
                    eventBus.subscribe(DynamicTestEvent::class.java, object : EventHandler<DynamicTestEvent> {
                        override suspend fun handle(event: DynamicTestEvent) {
                            testEvent1Count.incrementAndGet()
                        }
                        
                        override fun canHandle(eventType: String): Boolean = eventType == "dynamic-test-event"
                    })
                    
                    eventBus.subscribe(AnotherDynamicTestEvent::class.java, object : EventHandler<AnotherDynamicTestEvent> {
                        override suspend fun handle(event: AnotherDynamicTestEvent) {
                            testEvent2Count.incrementAndGet()
                        }
                        
                        override fun canHandle(eventType: String): Boolean = eventType == "another-dynamic-test-event"
                    })
                    
                    eventBus.start()
                    
                    // 同时发布两种类型的事件
                    eventBus.publish(DynamicTestEvent("Event 1"))
                    eventBus.publish(AnotherDynamicTestEvent("Event 2"))
                    
                    delay(100)
                    
                    testEvent1Count.get() shouldBe 1
                    testEvent2Count.get() shouldBe 1
                }
            }
        }
        
        `when`("检查事件总线状态") {
            then("状态信息应该正确反映当前状态") {
                runBlocking {
                    // 初始状态
                    var status = eventBus.getStatus()
                    status.isActive shouldBe false
                    status.activeSubscriptions shouldBe 0
                    
                    // 注册处理器后的状态
                    eventBus.subscribe(DynamicTestEvent::class.java, object : EventHandler<DynamicTestEvent> {
                        override suspend fun handle(event: DynamicTestEvent) {}
                        override fun canHandle(eventType: String): Boolean = eventType == "dynamic-test-event"
                    })
                    
                    status = eventBus.getStatus()
                    status.isActive shouldBe false
                    status.activeSubscriptions shouldBe 1
                    
                    // 启动后的状态
                    eventBus.start()
                    status = eventBus.getStatus()
                    status.isActive shouldBe true
                    status.activeSubscriptions shouldBe 1
                    
                    // 停止后的状态
                    eventBus.stop()
                    status = eventBus.getStatus()
                    status.isActive shouldBe false
                    status.activeSubscriptions shouldBe 1
                }
            }
        }
        
        `when`("处理异常事件") {
            then("异常不应该影响其他处理器的执行") {
                runBlocking {
                    val successfulHandlerCount = AtomicInteger(0)
                    
                    // 注册一个会抛出异常的处理器
                    eventBus.subscribe(DynamicTestEvent::class.java, object : EventHandler<DynamicTestEvent> {
                        override suspend fun handle(event: DynamicTestEvent) {
                            throw RuntimeException("Test exception")
                        }
                        
                        override fun canHandle(eventType: String): Boolean = true
                    })
                    
                    // 注册一个正常的处理器
                    eventBus.subscribe(DynamicTestEvent::class.java, object : EventHandler<DynamicTestEvent> {
                        override suspend fun handle(event: DynamicTestEvent) {
                            successfulHandlerCount.incrementAndGet()
                        }
                        
                        override fun canHandle(eventType: String): Boolean = true
                    })
                    
                    eventBus.start()
                    
                    // 发布事件
                    eventBus.publish(DynamicTestEvent("Test content"))
                    delay(100)
                    
                    // 验证正常处理器仍然被执行
                    successfulHandlerCount.get() shouldBe 1
                }
            }
        }
    }
})

/**
 * 动态测试事件类
 */
data class DynamicTestEvent(
    val content: String,
    override val eventHelper: EventHelper = EventHelper(
        eventId = EventId.generate(),
        occurredAt = Instant.now(),
        eventType = "dynamic-test-event",
        aggregateId = null,
        aggregateType = null,
        version = 1
    )
) : Event

/**
 * 另一个动态测试事件类
 */
data class AnotherDynamicTestEvent(
    val data: String,
    override val eventHelper: EventHelper = EventHelper(
        eventId = EventId.generate(),
        occurredAt = Instant.now(),
        eventType = "another-dynamic-test-event",
        aggregateId = null,
        aggregateType = null,
        version = 1
    )
) : Event
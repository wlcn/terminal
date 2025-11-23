package org.now.terminal.infrastructure.eventbus

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import org.now.terminal.shared.events.EventHandler
import org.now.terminal.infrastructure.eventbus.SimpleEventBus
import org.now.terminal.infrastructure.eventbus.EventBusFactory
import org.now.terminal.infrastructure.eventbus.TestEvent

class SimpleEventBusTest : BehaviorSpec({
    
    given("一个简单事件总线") {
        
        `when`("启动和停止事件总线") {
            then("事件总线应该正常启动和停止") {
                runBlocking {
                    val eventBus = SimpleEventBus(bufferSize = 100)
                    
                    eventBus.isRunning() shouldBe false
                    
                    // 注册一个处理器
                    eventBus.subscribe(TestEvent::class.java, object : EventHandler<TestEvent> {
                        override suspend fun handle(event: TestEvent) {}
                        override fun canHandle(eventType: String): Boolean = eventType == "test-event"
                    })
                    
                    eventBus.start()
                    eventBus.isRunning() shouldBe true
                    
                    eventBus.stop()
                    eventBus.isRunning() shouldBe false
                }
            }
        }
        
        `when`("事件总线配置") {
            then("应该能够创建不同配置的事件总线") {
                runBlocking {
                    val customEventBus = SimpleEventBus(bufferSize = 500)
                    
                    // 注册一个处理器
                    customEventBus.subscribe(TestEvent::class.java, object : EventHandler<TestEvent> {
                        override suspend fun handle(event: TestEvent) {}
                        override fun canHandle(eventType: String): Boolean = eventType == "test-event"
                    })
                    
                    customEventBus.start()
                    customEventBus.isRunning() shouldBe true
                    customEventBus.stop()
                    customEventBus.isRunning() shouldBe false
                }
            }
        }
        
        `when`("事件总线工厂") {
            then("应该能够通过工厂创建事件总线") {
                runBlocking {
                    val factoryBus = EventBusFactory.createDefault()
                    
                    // 注册一个处理器
                    factoryBus.subscribe(TestEvent::class.java, object : EventHandler<TestEvent> {
                        override suspend fun handle(event: TestEvent) {}
                        override fun canHandle(eventType: String): Boolean = eventType == "test-event"
                    })
                    
                    factoryBus.start()
                    factoryBus.isRunning() shouldBe true
                    factoryBus.stop()
                    factoryBus.isRunning() shouldBe false
                }
            }
        }
        
        `when`("发布和订阅事件") {
            then("应该能够正确接收事件") {
                runBlocking {
                    val eventBus = SimpleEventBus(bufferSize = 100)
                    
                    var receivedEvent: TestEvent? = null
                    
                    // 创建事件处理器
                    val handler = object : EventHandler<TestEvent> {
                        override suspend fun handle(event: TestEvent) {
                            receivedEvent = event
                        }
                        
                        override fun canHandle(eventType: String): Boolean {
                            return eventType == "test-event"
                        }
                    }
                    
                    // 先订阅事件，再启动事件总线
                    eventBus.subscribe(TestEvent::class.java, handler)
                    eventBus.start()
                    
                    // 发布事件
                    val event = TestEvent(testData = "test-data")
                    eventBus.publish(event)
                    
                    // 等待更长时间让事件被处理
                    kotlinx.coroutines.delay(200)
                    
                    // 验证事件被正确接收
                    receivedEvent shouldNotBe null
                    receivedEvent!!.testData shouldBe "test-data"
                    
                    eventBus.stop()
                }
            }
        }
    }
})
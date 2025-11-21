package org.now.terminal.infrastructure.eventbus

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.comparables.shouldBeGreaterThan
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.now.terminal.shared.events.EventHandler
import org.now.terminal.infrastructure.eventbus.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class EventBusComprehensiveTest : BehaviorSpec({
    
    given("事件总线综合测试") {
        
        `when`("发布和订阅事件") {
            then("应该能够正确接收事件") {
                val eventBus = InMemoryEventBus(bufferSize = 100)
                eventBus.start()
                
                val receivedEvent = AtomicReference<TestEvent?>()
                val latch = CountDownLatch(1)
                
                // 创建事件处理器
                val handler = object : EventHandler<TestEvent> {
                    override suspend fun handle(event: TestEvent) {
                        receivedEvent.set(event)
                        latch.countDown()
                    }
                    
                    override fun canHandle(eventType: String): Boolean {
                        return eventType == "TestEvent"
                    }
                }
                
                // 订阅事件
                runBlocking {
                    eventBus.subscribe(TestEvent::class.java, handler)
                    
                    // 发布事件
                    val event = TestEvent(testData = "test-data-1")
                    eventBus.publish(event)
                }
                
                // 等待事件处理完成
                latch.await(5, TimeUnit.SECONDS)
                
                // 验证事件被正确接收
                receivedEvent.get() shouldNotBe null
                receivedEvent.get()!!.testData shouldBe "test-data-1"
                
                eventBus.stop()
            }
        }
        
        `when`("处理多个订阅者") {
            then("应该能够调用所有订阅者") {
                val eventBus = InMemoryEventBus(bufferSize = 100)
                eventBus.start()
                
                val handler1Count = AtomicInteger(0)
                val handler2Count = AtomicInteger(0)
                val latch = CountDownLatch(2)
                
                // 创建两个事件处理器
                val handler1 = object : EventHandler<TestEvent> {
                    override suspend fun handle(event: TestEvent) {
                        handler1Count.incrementAndGet()
                        latch.countDown()
                    }
                    
                    override fun canHandle(eventType: String): Boolean {
                        return eventType == "TestEvent"
                    }
                }
                
                val handler2 = object : EventHandler<TestEvent> {
                    override suspend fun handle(event: TestEvent) {
                        handler2Count.incrementAndGet()
                        latch.countDown()
                    }
                    
                    override fun canHandle(eventType: String): Boolean {
                        return eventType == "TestEvent"
                    }
                }
                
                runBlocking {
                    // 订阅事件
                    eventBus.subscribe(TestEvent::class.java, handler1)
                    eventBus.subscribe(TestEvent::class.java, handler2)
                    
                    // 发布事件
                    val event = TestEvent(testData = "test-data-2")
                    eventBus.publish(event)
                }
                
                // 等待事件处理完成
                latch.await(5, TimeUnit.SECONDS)
                
                // 验证两个处理器都被调用
                handler1Count.get() shouldBe 1
                handler2Count.get() shouldBe 1
                
                eventBus.stop()
            }
        }
        
        `when`("取消订阅事件") {
            then("应该能够正确取消订阅") {
                val eventBus = InMemoryEventBus(bufferSize = 100)
                eventBus.start()
                
                val handlerCount = AtomicInteger(0)
                val latch = CountDownLatch(1)
                
                // 创建事件处理器
                val handler = object : EventHandler<TestEvent> {
                    override suspend fun handle(event: TestEvent) {
                        handlerCount.incrementAndGet()
                        latch.countDown()
                    }
                    
                    override fun canHandle(eventType: String): Boolean {
                        return eventType == "TestEvent"
                    }
                }
                
                runBlocking {
                    // 订阅事件
                    eventBus.subscribe(TestEvent::class.java, handler)
                    
                    // 发布第一个事件
                    val event1 = TestEvent(testData = "test-data-3")
                    eventBus.publish(event1)
                    
                    // 等待第一个事件处理完成
                    latch.await(5, TimeUnit.SECONDS)
                    
                    // 取消订阅
                    eventBus.unsubscribe(TestEvent::class.java, handler)
                    
                    // 发布第二个事件
                    val event2 = TestEvent(testData = "test-data-4")
                    eventBus.publish(event2)
                    
                    // 等待一小段时间确保第二个事件不会被处理
                    delay(100)
                }
                
                // 验证只有第一个事件被处理
                handlerCount.get() shouldBe 1
                
                eventBus.stop()
            }
        }
        
        `when`("收集事件总线指标") {
            then("应该能够正确更新指标") {
                val eventBus = InMemoryEventBus(bufferSize = 100)
                val metrics = EventBusMetrics()
                val monitoredEventBus = MonitoredEventBus(eventBus, metrics)
                monitoredEventBus.start()
                
                val latch = CountDownLatch(1)
                
                // 创建事件处理器
                val handler = object : EventHandler<TestEvent> {
                    override suspend fun handle(event: TestEvent) {
                        latch.countDown()
                    }
                    
                    override fun canHandle(eventType: String): Boolean {
                        return eventType == "TestEvent"
                    }
                }
                
                runBlocking {
                    // 订阅事件
                    monitoredEventBus.subscribe(TestEvent::class.java, handler)
                    
                    // 获取初始指标
                    val initialMetrics = metrics.getMetricsSnapshot()
                    
                    // 发布事件
                    val event = TestEvent(testData = "test-data-5")
                    monitoredEventBus.publish(event)
                }
                
                // 等待事件处理完成
                latch.await(5, TimeUnit.SECONDS)
                
                // 获取最终指标
                val finalMetrics = metrics.getMetricsSnapshot()
                
                // 验证指标被正确更新
                finalMetrics.totalEvents shouldBeGreaterThan 0
                finalMetrics.eventCounts["TestEvent"] shouldBe 1L
                
                monitoredEventBus.stop()
            }
        }
        
        `when`("处理事件处理异常") {
            then("应该能够继续运行") {
                val eventBus = InMemoryEventBus(bufferSize = 100)
                eventBus.start()
                
                val errorHandlerCalled = AtomicReference<Boolean>(false)
                val latch = CountDownLatch(1)
                
                // 创建会抛出异常的事件处理器
                val errorHandler = object : EventHandler<TestEvent> {
                    override suspend fun handle(event: TestEvent) {
                        errorHandlerCalled.set(true)
                        latch.countDown()
                        throw RuntimeException("Test exception")
                    }
                    
                    override fun canHandle(eventType: String): Boolean {
                        return eventType == "TestEvent"
                    }
                }
                
                runBlocking {
                    // 订阅事件
                    eventBus.subscribe(TestEvent::class.java, errorHandler)
                    
                    // 发布事件
                    val event = TestEvent(testData = "test-data-6")
                    eventBus.publish(event)
                }
                
                // 等待事件处理完成
                latch.await(5, TimeUnit.SECONDS)
                
                // 验证处理器被调用（即使抛出异常）
                errorHandlerCalled.get() shouldBe true
                
                // 事件总线应该继续运行
                eventBus.stop()
            }
        }
        
        `when`("使用事件总线构建器") {
            then("应该能够创建配置化的事件总线") {
                val config = EventBusConfig(
                    EventBusProperties(
                        type = EventBusType.IN_MEMORY,
                        bufferSize = 500,
                        enableMetrics = true
                    )
                )
                
                val metrics = EventBusMetrics()
                val builder = EventBusBuilder()
                    .withConfig(config)
                    .withMetrics(metrics)
                
                val eventBus = builder.build()
                eventBus shouldNotBe null
                
                // 验证创建的是监控事件总线
                eventBus.javaClass shouldBe MonitoredEventBus::class.java
            }
        }
    }
})
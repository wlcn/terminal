package org.now.terminal.infrastructure.eventbus

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.longs.shouldBeGreaterThan
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.now.terminal.shared.events.EventHandler
import org.now.terminal.shared.events.SystemHeartbeatEvent
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class EventBusComprehensiveTest : StringSpec({
    
    "应该能够发布和订阅事件" {
        val eventBus = InMemoryEventBus(bufferSize = 100)
        eventBus.start()
        
        val receivedEvent = AtomicReference<SystemHeartbeatEvent?>()
        val latch = CountDownLatch(1)
        
        // 创建事件处理器
        val handler = object : EventHandler<SystemHeartbeatEvent> {
            override suspend fun handle(event: SystemHeartbeatEvent) {
                receivedEvent.set(event)
                latch.countDown()
            }
            
            override fun canHandle(eventType: String): Boolean {
                return eventType == "SystemHeartbeatEvent"
            }
        }
        
        // 订阅事件
        runBlocking {
            eventBus.subscribe(SystemHeartbeatEvent::class.java, handler)
            
            // 发布事件
            val event = SystemHeartbeatEvent.createHealthy("test-system", "test-component")
            eventBus.publish(event)
        }
        
        // 等待事件处理完成
        latch.await(5, TimeUnit.SECONDS)
        
        // 验证事件被正确接收
        receivedEvent.get() shouldNotBe null
        receivedEvent.get()!!.systemId shouldBe "test-system"
        receivedEvent.get()!!.component shouldBe "test-component"
        
        eventBus.stop()
    }
    
    "应该能够处理多个订阅者" {
        val eventBus = InMemoryEventBus(bufferSize = 100)
        eventBus.start()
        
        val handler1Count = AtomicInteger(0)
        val handler2Count = AtomicInteger(0)
        val latch = CountDownLatch(2)
        
        // 创建两个事件处理器
        val handler1 = object : EventHandler<SystemHeartbeatEvent> {
            override suspend fun handle(event: SystemHeartbeatEvent) {
                handler1Count.incrementAndGet()
                latch.countDown()
            }
            
            override fun canHandle(eventType: String): Boolean {
                return eventType == "SystemHeartbeatEvent"
            }
        }
        
        val handler2 = object : EventHandler<SystemHeartbeatEvent> {
            override suspend fun handle(event: SystemHeartbeatEvent) {
                handler2Count.incrementAndGet()
                latch.countDown()
            }
            
            override fun canHandle(eventType: String): Boolean {
                return eventType == "SystemHeartbeatEvent"
            }
        }
        
        runBlocking {
            // 订阅事件
            eventBus.subscribe(SystemHeartbeatEvent::class.java, handler1)
            eventBus.subscribe(SystemHeartbeatEvent::class.java, handler2)
            
            // 发布事件
            val event = SystemHeartbeatEvent.createHealthy("test-system", "test-component")
            eventBus.publish(event as Event)
        }
        
        // 等待事件处理完成
        latch.await(5, TimeUnit.SECONDS)
        
        // 验证两个处理器都被调用
        handler1Count.get() shouldBe 1
        handler2Count.get() shouldBe 1
        
        eventBus.stop()
    }
    
    "应该能够取消订阅" {
        val eventBus = InMemoryEventBus(bufferSize = 100)
        eventBus.start()
        
        val handlerCount = AtomicInteger(0)
        val latch = CountDownLatch(1)
        
        // 创建事件处理器
        val handler = object : EventHandler<SystemHeartbeatEvent> {
            override suspend fun handle(event: SystemHeartbeatEvent) {
                handlerCount.incrementAndGet()
                latch.countDown()
            }
            
            override fun canHandle(eventType: String): Boolean {
                return eventType == "SystemHeartbeatEvent"
            }
        }
        
        runBlocking {
            // 订阅事件
            eventBus.subscribe(SystemHeartbeatEvent::class.java, handler)
            
            // 发布第一个事件
            val event1 = SystemHeartbeatEvent.createHealthy("test-system", "test-component")
            eventBus.publish(event1)
            
            // 等待第一个事件处理完成
            latch.await(5, TimeUnit.SECONDS)
            
            // 取消订阅
            eventBus.unsubscribe(SystemHeartbeatEvent::class.java, handler)
            
            // 发布第二个事件
            val event2 = SystemHeartbeatEvent.createHealthy("test-system", "test-component-2")
            eventBus.publish(event2)
            
            // 等待一小段时间确保第二个事件不会被处理
            delay(100)
        }
        
        // 验证只有第一个事件被处理
        handlerCount.get() shouldBe 1
        
        eventBus.stop()
    }
    
    "应该能够收集事件总线指标" {
        val eventBus = InMemoryEventBus(bufferSize = 100)
        val metrics = EventBusMetrics()
        val monitoredEventBus = MonitoredEventBus(eventBus, metrics)
        monitoredEventBus.start()
        
        val latch = CountDownLatch(1)
        
        // 创建事件处理器
        val handler = object : EventHandler<SystemHeartbeatEvent> {
            override suspend fun handle(event: SystemHeartbeatEvent) {
                latch.countDown()
            }
            
            override fun canHandle(eventType: String): Boolean {
                return eventType == "SystemHeartbeatEvent"
            }
        }
        
        runBlocking {
            // 订阅事件
            monitoredEventBus.subscribe(SystemHeartbeatEvent::class.java, handler)
            
            // 获取初始指标
            val initialMetrics = metrics.getMetricsSnapshot()
            
            // 发布事件
            val event = SystemHeartbeatEvent.createHealthy("test-system", "test-component")
            monitoredEventBus.publish(event)
        }
        
        // 等待事件处理完成
        latch.await(5, TimeUnit.SECONDS)
        
        // 获取最终指标
        val finalMetrics = metrics.getMetricsSnapshot()
        
        // 验证指标被正确更新
        finalMetrics.totalEvents shouldBeGreaterThan initialMetrics.totalEvents
        finalMetrics.eventCounts["SystemHeartbeatEvent"] shouldBe 1L
        
        monitoredEventBus.stop()
    }
    
    "应该能够处理事件处理异常" {
        val eventBus = InMemoryEventBus(bufferSize = 100)
        eventBus.start()
        
        val errorHandlerCalled = AtomicReference<Boolean>(false)
        val latch = CountDownLatch(1)
        
        // 创建会抛出异常的事件处理器
        val errorHandler = object : EventHandler<SystemHeartbeatEvent> {
            override suspend fun handle(event: SystemHeartbeatEvent) {
                errorHandlerCalled.set(true)
                latch.countDown()
                throw RuntimeException("Test exception")
            }
            
            override fun canHandle(eventType: String): Boolean {
                return eventType == "SystemHeartbeatEvent"
            }
        }
        
        runBlocking {
            // 订阅事件
            eventBus.subscribe(SystemHeartbeatEvent::class.java, errorHandler)
            
            // 发布事件
            val event = SystemHeartbeatEvent.createHealthy("test-system", "test-component")
            eventBus.publish(event)
        }
        
        // 等待事件处理完成
        latch.await(5, TimeUnit.SECONDS)
        
        // 验证处理器被调用（即使抛出异常）
        errorHandlerCalled.get() shouldBe true
        
        // 事件总线应该继续运行
        eventBus.stop()
    }
    
    "应该能够使用事件总线构建器创建配置化的事件总线" {
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
})
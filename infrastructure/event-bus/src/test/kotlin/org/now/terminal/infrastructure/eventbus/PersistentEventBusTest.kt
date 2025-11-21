package org.now.terminal.infrastructure.eventbus

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.now.terminal.shared.events.EventHandler
import org.now.terminal.shared.events.SystemHeartbeatEvent
import org.now.terminal.shared.events.SessionCreatedEvent
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

class PersistentEventBusTest : BehaviorSpec({
    
    val tempStorageDir = "temp-test-events"
    
    beforeEach {
        // 清理临时目录
        File(tempStorageDir).deleteRecursively()
    }
    
    afterEach {
        // 清理临时目录
        File(tempStorageDir).deleteRecursively()
    }
    
    given("a PersistentEventBus with file storage") {
        `when`("publishing events with persistence enabled") {
            then("it should store events to file system") = runTest {
                val eventStore = FileEventStore(tempStorageDir)
                val eventBus = PersistentEventBus(
                    delegate = InMemoryEventBus(),
                    eventStore = eventStore,
                    enablePersistence = true
                )
                
                eventBus.start()
                
                val eventCount = 5
                val receivedCount = AtomicInteger(0)
                
                // 订阅事件
                eventBus.subscribe(object : EventHandler {
                    override suspend fun handle(event: SystemHeartbeatEvent) {
                        receivedCount.incrementAndGet()
                    }
                }, "SystemHeartbeatEvent")
                
                // 发布事件
                repeat(eventCount) { i ->
                    val event = SystemHeartbeatEvent(
                        timestamp = System.currentTimeMillis() + i,
                        systemId = "test-system-$i"
                    )
                    eventBus.publish(event)
                }
                
                // 等待事件处理完成
                delay(100)
                
                // 检查事件是否被存储
                val storedCount = eventStore.count()
                storedCount shouldBe eventCount.toLong()
                
                // 检查文件是否创建
                val eventFiles = File(tempStorageDir).listFiles { file -> file.name.endsWith(".json") }
                eventFiles?.size shouldBe eventCount
                
                eventBus.stop()
            }
        }
        
        `when`("publishing events with persistence disabled") {
            then("it should not store events to file system") = runTest {
                val eventStore = FileEventStore(tempStorageDir)
                val eventBus = PersistentEventBus(
                    delegate = InMemoryEventBus(),
                    eventStore = eventStore,
                    enablePersistence = false
                )
                
                eventBus.start()
                
                val eventCount = 3
                
                // 发布事件
                repeat(eventCount) { i ->
                    val event = SystemHeartbeatEvent(
                        timestamp = System.currentTimeMillis() + i,
                        systemId = "test-system-$i"
                    )
                    eventBus.publish(event)
                }
                
                // 等待事件处理完成
                delay(100)
                
                // 检查事件是否未被存储
                val storedCount = eventStore.count()
                storedCount shouldBe 0
                
                // 检查文件是否未创建
                val eventFiles = File(tempStorageDir).listFiles { file -> file.name.endsWith(".json") }
                eventFiles?.size shouldBe 0
                
                eventBus.stop()
            }
        }
        
        `when`("starting and stopping the bus") {
            then("it should manage lifecycle correctly") = runTest {
                val eventBus = PersistentEventBus(
                    delegate = InMemoryEventBus(),
                    eventStore = FileEventStore(tempStorageDir),
                    enablePersistence = true
                )
                
                eventBus.isRunning() shouldBe false
                
                eventBus.start()
                eventBus.isRunning() shouldBe true
                
                eventBus.stop()
                eventBus.isRunning() shouldBe false
            }
        }
        
        `when`("using the builder pattern") {
            then("it should create bus with custom configuration") = runTest {
                val customScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
                val customStore = FileEventStore("custom-storage")
                
                val eventBus = PersistentEventBusBuilder()
                    .withDelegate(InMemoryEventBus())
                    .withEventStore(customStore)
                    .withPersistenceEnabled(true)
                    .withPersistenceScope(customScope)
                    .build()
                
                eventBus.getEventStore() shouldBe customStore
                eventBus.getDelegate() shouldNotBe null
                
                // 清理
                File("custom-storage").deleteRecursively()
                customScope.cancel()
            }
        }
        
        `when`("getting stored event count") {
            then("it should return correct count") = runTest {
                val eventStore = FileEventStore(tempStorageDir)
                val eventBus = PersistentEventBus(
                    delegate = InMemoryEventBus(),
                    eventStore = eventStore,
                    enablePersistence = true
                )
                
                eventBus.start()
                
                val eventCount = 2
                
                // 发布事件
                repeat(eventCount) { i ->
                    val event = SessionCreatedEvent(
                        timestamp = System.currentTimeMillis() + i,
                        sessionId = UUID.randomUUID().toString(),
                        userId = "user-$i"
                    )
                    eventBus.publish(event)
                }
                
                // 等待事件处理完成
                delay(100)
                
                val storedCount = eventBus.getStoredEventCount()
                storedCount shouldBe eventCount.toLong()
                
                eventBus.stop()
            }
        }
        
        `when`("cleaning up stored events") {
            then("it should remove old events") = runTest {
                val eventStore = FileEventStore(tempStorageDir)
                val eventBus = PersistentEventBus(
                    delegate = InMemoryEventBus(),
                    eventStore = eventStore,
                    enablePersistence = true
                )
                
                eventBus.start()
                
                // 发布一些事件
                val event1 = SystemHeartbeatEvent(
                    timestamp = System.currentTimeMillis() - 10000, // 10秒前
                    systemId = "old-system"
                )
                val event2 = SystemHeartbeatEvent(
                    timestamp = System.currentTimeMillis(), // 现在
                    systemId = "current-system"
                )
                
                eventBus.publish(event1)
                eventBus.publish(event2)
                
                delay(100)
                
                // 清理10秒前的事件
                val cleanupTime = System.currentTimeMillis() - 5000 // 5秒前
                val cleanedCount = eventBus.cleanupStoredEvents(cleanupTime)
                
                // 应该清理掉第一个事件
                cleanedCount shouldBe 1
                
                // 剩余事件数量
                val remainingCount = eventStore.count()
                remainingCount shouldBe 1
                
                eventBus.stop()
            }
        }
    }
    
    given("the EventBusFactory with persistent bus creation") {
        `when`("creating persistent event bus") {
            then("it should create bus with default configuration") = runTest {
                val eventBus = EventBusFactory.createPersistentEventBus()
                
                eventBus shouldNotBe null
                eventBus.shouldBeInstanceOf<PersistentEventBus>()
                
                // 测试基本功能
                eventBus.start()
                
                val receivedCount = AtomicInteger(0)
                eventBus.subscribe(object : EventHandler {
                    override suspend fun handle(event: SystemHeartbeatEvent) {
                        receivedCount.incrementAndGet()
                    }
                }, "SystemHeartbeatEvent")
                
                val testEvent = SystemHeartbeatEvent(
                    timestamp = System.currentTimeMillis(),
                    systemId = "factory-test"
                )
                
                eventBus.publish(testEvent)
                delay(50)
                
                receivedCount.get() shouldBe 1
                
                eventBus.stop()
                
                // 清理临时文件
                File("events").deleteRecursively()
            }
        }
    }
})
package org.now.terminal.infrastructure.eventbus

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import org.now.terminal.shared.events.SystemHeartbeatEvent
import org.now.terminal.shared.events.EventHandler

class SimpleEventBusTest : BehaviorSpec({
    
    given("一个简单事件总线") {
        
        `when`("启动和停止事件总线") {
            then("事件总线应该正常启动和停止") {
                runBlocking {
                    val eventBus = SimpleEventBus(bufferSize = 100)
                    
                    eventBus.isRunning() shouldBe false
                    
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
                    eventBus.start()
                    
                    var receivedEvent: SystemHeartbeatEvent? = null
                    
                    // 创建事件处理器
                    val handler = object : EventHandler<SystemHeartbeatEvent> {
                        override suspend fun handle(event: SystemHeartbeatEvent) {
                            receivedEvent = event
                        }
                        
                        override fun canHandle(eventType: String): Boolean {
                            return eventType == "SystemHeartbeatEvent"
                        }
                    }
                    
                    // 订阅事件
                    eventBus.subscribe(SystemHeartbeatEvent::class.java, handler)
                    
                    // 发布事件
                    val event = SystemHeartbeatEvent.createHealthy(
                        systemId = "test-system",
                        component = "test-component"
                    )
                    eventBus.publish(event)
                    
                    // 等待一小段时间让事件被处理
                    kotlinx.coroutines.delay(100)
                    
                    // 验证事件被正确接收
                    receivedEvent shouldNotBe null
                    receivedEvent!!.systemId shouldBe "test-system"
                    
                    eventBus.stop()
                }
            }
        }
    }
})
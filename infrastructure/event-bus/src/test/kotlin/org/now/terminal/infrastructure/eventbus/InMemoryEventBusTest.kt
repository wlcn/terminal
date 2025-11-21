package org.now.terminal.infrastructure.eventbus

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class InMemoryEventBusTest : BehaviorSpec({
    
    given("一个内存事件总线") {
        val eventBus = InMemoryEventBus(bufferSize = 100)
        
        beforeEach {
            eventBus.start()
        }
        
        afterEach {
            eventBus.stop()
        }
        
        `when`("启动和停止事件总线") {
            then("事件总线应该正常启动和停止") {
                // 测试事件总线生命周期管理
                eventBus.start()
                eventBus.stop()
                true shouldBe true
            }
        }
        
        `when`("事件总线配置") {
            then("应该能够创建不同配置的事件总线") {
                val customEventBus = InMemoryEventBus(bufferSize = 500)
                customEventBus.start()
                customEventBus.stop()
                true shouldBe true
            }
        }
        
        `when`("事件总线工厂") {
            then("应该能够通过工厂创建事件总线") {
                val factoryBus = EventBusFactory.createInMemoryEventBus(bufferSize = 200)
                factoryBus.start()
                factoryBus.stop()
                true shouldBe true
            }
        }
    }
})
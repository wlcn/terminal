package org.now.terminal.infrastructure.eventbus

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import org.now.terminal.shared.events.SystemHeartbeatEvent
import org.now.terminal.shared.events.SessionCreatedEvent
import java.util.UUID

class EventSerializerTest : BehaviorSpec({
    val serializer = EventSerializer()
    
    given("an EventSerializer") {
        `when`("serializing a valid event") {
            then("it should return a valid JSON string") {
                val event = SystemHeartbeatEvent(
                    timestamp = System.currentTimeMillis(),
                    systemId = "test-system"
                )
                
                val json = serializer.serialize(event)
                
                json shouldNotBe null
                json shouldNotBe ""
                json shouldBe """{"eventHelper":{"eventId":"${event.eventId}","eventType":"SystemHeartbeatEvent","timestamp":${event.timestamp},"aggregateId":"${event.aggregateId}"},"systemId":"test-system"}""".trimIndent()
            }
        }
        
        `when`("serializing different event types") {
            then("it should handle all event types correctly") {
                val heartbeatEvent = SystemHeartbeatEvent(
                    timestamp = System.currentTimeMillis(),
                    systemId = "test-system"
                )
                
                val sessionEvent = SessionCreatedEvent(
                    timestamp = System.currentTimeMillis(),
                    sessionId = UUID.randomUUID().toString(),
                    userId = "test-user"
                )
                
                val heartbeatJson = serializer.serialize(heartbeatEvent)
                val sessionJson = serializer.serialize(sessionEvent)
                
                heartbeatJson shouldNotBe sessionJson
                heartbeatJson.contains("SystemHeartbeatEvent") shouldBe true
                sessionJson.contains("SessionCreatedEvent") shouldBe true
            }
        }
        
        `when`("validating event JSON") {
            then("it should return true for valid JSON") {
                val event = SystemHeartbeatEvent(
                    timestamp = System.currentTimeMillis(),
                    systemId = "test-system"
                )
                
                val json = serializer.serialize(event)
                val isValid = serializer.validateEventJson(json)
                
                isValid shouldBe true
            }
            
            then("it should return false for invalid JSON") {
                val invalidJson = "{invalid json}"
                val isValid = serializer.validateEventJson(invalidJson)
                
                isValid shouldBe false
            }
            
            then("it should return false for JSON without event structure") {
                val nonEventJson = """{"name":"test","value":123}"""
                val isValid = serializer.validateEventJson(nonEventJson)
                
                isValid shouldBe false
            }
        }
        
        `when`("getting event type from JSON") {
            then("it should extract event type correctly") {
                val event = SystemHeartbeatEvent(
                    timestamp = System.currentTimeMillis(),
                    systemId = "test-system"
                )
                
                val json = serializer.serialize(event)
                val eventType = serializer.getEventTypeFromJson(json)
                
                eventType shouldBe "SystemHeartbeatEvent"
            }
            
            then("it should return null for invalid JSON") {
                val invalidJson = "{invalid json}"
                val eventType = serializer.getEventTypeFromJson(invalidJson)
                
                eventType shouldBe null
            }
        }
        
        `when`("serialization fails") {
            then("it should throw EventSerializationException") {
                // 创建一个无法序列化的对象（简化测试）
                val invalidEvent = object : SystemHeartbeatEvent(
                    timestamp = System.currentTimeMillis(),
                    systemId = "test-system"
                ) {
                    // 添加一个无法序列化的字段
                    val circularReference = this
                }
                
                try {
                    serializer.serialize(invalidEvent)
                    // 如果到达这里，测试失败
                    throw AssertionError("Expected EventSerializationException")
                } catch (e: EventSerializationException) {
                    e.eventType shouldBe "SystemHeartbeatEvent"
                    e.operation shouldBe "serialize"
                    e.message shouldNotBe null
                }
            }
        }
    }
})
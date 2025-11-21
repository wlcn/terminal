package org.now.terminal.shared.valueobjects

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.assertions.throwables.shouldThrow

class EventIdTest : StringSpec({
    
    "创建有效的EventId" {
        val eventId = EventId.create("evt_550e8400-e29b-41d4-a716-446655440000")
        
        eventId.value shouldBe "evt_550e8400-e29b-41d4-a716-446655440000"
        eventId.isValid() shouldBe true
        eventId.uuid.toString() shouldBe "550e8400-e29b-41d4-a716-446655440000"
    }
    
    "生成新的EventId" {
        val eventId = EventId.generate()
        
        eventId.isValid() shouldBe true
        eventId.value shouldContain "evt_"
        eventId.value.length shouldBe 40 // "evt_" + UUID长度
    }
    
    "从字符串解析EventId" {
        val eventId = EventId.fromString("evt_550e8400-e29b-41d4-a716-446655440000")
        
        eventId.value shouldBe "evt_550e8400-e29b-41d4-a716-446655440000"
    }
    
    "从纯UUID创建EventId" {
        val uuid = java.util.UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
        val eventId = EventId.fromUuid(uuid)
        
        eventId.value shouldBe "evt_550e8400-e29b-41d4-a716-446655440000"
    }
    
    "创建无效格式的EventId应抛出异常" {
        val exception = shouldThrow<IllegalArgumentException> {
            EventId.create("invalid-format")
        }
        
        exception.message shouldContain "ID must be in format evt_{UUID}"
    }
    
    "创建空字符串应抛出异常" {
        val exception = shouldThrow<IllegalArgumentException> {
            EventId.create("")
        }
        
        exception.message shouldContain "ID cannot be blank"
    }
    
    "验证正确的EventId格式" {
        EventId.create("evt_550e8400-e29b-41d4-a716-446655440000").isValid() shouldBe true
    }
    
    "验证错误的EventId格式" {
        val exception = shouldThrow<IllegalArgumentException> {
            EventId.create("evt_invalid-uuid")
        }
        
        exception.message shouldContain "ID must be in format evt_{UUID}"
    }
    
    "相同值的EventId应该相等" {
        val eventId1 = EventId.create("evt_550e8400-e29b-41d4-a716-446655440000")
        val eventId2 = EventId.create("evt_550e8400-e29b-41d4-a716-446655440000")
        
        eventId1 shouldBe eventId2
        eventId1.hashCode() shouldBe eventId2.hashCode()
    }
    
    "不同值的EventId应该不相等" {
        val eventId1 = EventId.create("evt_550e8400-e29b-41d4-a716-446655440000")
        val eventId2 = EventId.create("evt_550e8400-e29b-41d4-a716-446655440001")
        
        eventId1 shouldNotBe eventId2
    }
    
    "正确的字符串表示" {
        val eventId = EventId.create("evt_550e8400-e29b-41d4-a716-446655440000")
        
        eventId.toString() shouldBe "evt_550e8400-e29b-41d4-a716-446655440000"
    }
    
    "简化的ID表示" {
        val eventId = EventId.create("evt_550e8400-e29b-41d4-a716-446655440000")
        
        eventId.toShortString() shouldBe "evt_550e8400"
    }
    
    "纯UUID字符串表示" {
        val eventId = EventId.create("evt_550e8400-e29b-41d4-a716-446655440000")
        
        eventId.toUuidString() shouldBe "550e8400-e29b-41d4-a716-446655440000"
    }
})
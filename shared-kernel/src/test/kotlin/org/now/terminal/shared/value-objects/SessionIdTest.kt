package org.now.terminal.shared.valueobjects

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.assertions.throwables.shouldThrow

class SessionIdTest : StringSpec({
    
    "创建有效的SessionId" {
        val sessionId = SessionId.create("ses_550e8400-e29b-41d4-a716-446655440000")
        
        sessionId.value shouldBe "ses_550e8400-e29b-41d4-a716-446655440000"
        sessionId.isValid() shouldBe true
        sessionId.uuid.toString() shouldBe "550e8400-e29b-41d4-a716-446655440000"
    }
    
    "生成新的SessionId" {
        val sessionId = SessionId.generate()
        
        sessionId.isValid() shouldBe true
        sessionId.value shouldContain "ses_"
        sessionId.value.length shouldBe 40 // "ses_" + UUID长度
    }
    
    "从字符串解析SessionId" {
        val sessionId = SessionId.fromString("ses_550e8400-e29b-41d4-a716-446655440000")
        
        sessionId.value shouldBe "ses_550e8400-e29b-41d4-a716-446655440000"
    }
    
    "从纯UUID创建SessionId" {
        val uuid = java.util.UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
        val sessionId = SessionId.fromUuid(uuid)
        
        sessionId.value shouldBe "ses_550e8400-e29b-41d4-a716-446655440000"
    }
    
    "创建无效格式的SessionId应抛出异常" {
        val exception = shouldThrow<IllegalArgumentException> {
            SessionId.create("invalid-format")
        }
        
        exception.message shouldContain "Session ID must be in format ses_{UUID}"
    }
    
    "创建空字符串应抛出异常" {
        val exception = shouldThrow<IllegalArgumentException> {
            SessionId.create("")
        }
        
        exception.message shouldContain "Session ID cannot be blank"
    }
    
    "验证正确的SessionId格式" {
        SessionId.create("ses_550e8400-e29b-41d4-a716-446655440000").isValid() shouldBe true
    }
    
    "验证错误的SessionId格式" {
        SessionId.create("ses_invalid-uuid").isValid() shouldBe false
    }
    
    "相同值的SessionId应该相等" {
        val sessionId1 = SessionId.create("ses_550e8400-e29b-41d4-a716-446655440000")
        val sessionId2 = SessionId.create("ses_550e8400-e29b-41d4-a716-446655440000")
        
        sessionId1 shouldBe sessionId2
        sessionId1.hashCode() shouldBe sessionId2.hashCode()
    }
    
    "不同值的SessionId应该不相等" {
        val sessionId1 = SessionId.create("ses_550e8400-e29b-41d4-a716-446655440000")
        val sessionId2 = SessionId.create("ses_550e8400-e29b-41d4-a716-446655440001")
        
        sessionId1 shouldNotBe sessionId2
    }
    
    "正确的字符串表示" {
        val sessionId = SessionId.create("ses_550e8400-e29b-41d4-a716-446655440000")
        
        sessionId.toString() shouldBe "ses_550e8400-e29b-41d4-a716-446655440000"
    }
    
    "简化的ID表示" {
        val sessionId = SessionId.create("ses_550e8400-e29b-41d4-a716-446655440000")
        
        sessionId.toShortString() shouldBe "ses_550e8400"
    }
    
    "纯UUID字符串表示" {
        val sessionId = SessionId.create("ses_550e8400-e29b-41d4-a716-446655440000")
        
        sessionId.toUuidString() shouldBe "550e8400-e29b-41d4-a716-446655440000"
    }
})
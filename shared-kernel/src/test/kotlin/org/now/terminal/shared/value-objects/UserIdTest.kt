package org.now.terminal.shared.valueobjects

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.assertions.throwables.shouldThrow

class UserIdTest : StringSpec({
    
    "创建有效的UserId" {
        val userId = UserId.create("usr_123e4567-e89b-12d3-a456-426614174000")
        
        userId.value shouldBe "usr_123e4567-e89b-12d3-a456-426614174000"
        userId.isValid() shouldBe true
        userId.uuid.toString() shouldBe "123e4567-e89b-12d3-a456-426614174000"
    }
    
    "生成新的UserId" {
        val userId = UserId.generate()
        
        userId.isValid() shouldBe true
        userId.value shouldContain "usr_"
        userId.value.length shouldBe 40 // "usr_" + UUID长度
    }
    
    "从字符串解析UserId" {
        val userId = UserId.fromString("usr_123e4567-e89b-12d3-a456-426614174000")
        
        userId.value shouldBe "usr_123e4567-e89b-12d3-a456-426614174000"
    }
    
    "从纯UUID创建UserId" {
        val uuid = java.util.UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
        val userId = UserId.fromUuid(uuid)
        
        userId.value shouldBe "usr_123e4567-e89b-12d3-a456-426614174000"
    }
    
    "创建无效格式的UserId应抛出异常" {
        val exception = shouldThrow<IllegalArgumentException> {
            UserId.create("invalid-format")
        }
        
        exception.message shouldContain "ID must be in format usr_{UUID}"
    }
    
    "创建空字符串应抛出异常" {
        val exception = shouldThrow<IllegalArgumentException> {
            UserId.create("")
        }
        
        exception.message shouldContain "ID cannot be blank"
    }
    
    "验证正确的UserId格式" {
        UserId.create("usr_123e4567-e89b-12d3-a456-426614174000").isValid() shouldBe true
    }
    
    "验证错误的UserId格式" {
        val exception = shouldThrow<IllegalArgumentException> {
            UserId.create("usr_invalid-uuid")
        }
        
        exception.message shouldContain "ID must be in format usr_{UUID}"
    }
    
    "相同值的UserId应该相等" {
        val userId1 = UserId.create("usr_123e4567-e89b-12d3-a456-426614174000")
        val userId2 = UserId.create("usr_123e4567-e89b-12d3-a456-426614174000")
        
        userId1 shouldBe userId2
        userId1.hashCode() shouldBe userId2.hashCode()
    }
    
    "不同值的UserId应该不相等" {
        val userId1 = UserId.create("usr_123e4567-e89b-12d3-a456-426614174000")
        val userId2 = UserId.create("usr_123e4567-e89b-12d3-a456-426614174001")
        
        userId1 shouldNotBe userId2
    }
    
    "正确的字符串表示" {
        val userId = UserId.create("usr_123e4567-e89b-12d3-a456-426614174000")
        
        userId.toString() shouldBe "usr_123e4567-e89b-12d3-a456-426614174000"
    }
    
    "简化的ID表示" {
        val userId = UserId.create("usr_123e4567-e89b-12d3-a456-426614174000")
        
        userId.toShortString() shouldBe "usr_123e4567"
    }
    
    "纯UUID字符串表示" {
        val userId = UserId.create("usr_123e4567-e89b-12d3-a456-426614174000")
        
        userId.toUuidString() shouldBe "123e4567-e89b-12d3-a456-426614174000"
    }
})
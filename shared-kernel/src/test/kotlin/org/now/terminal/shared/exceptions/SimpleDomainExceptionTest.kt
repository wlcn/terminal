package org.now.terminal.shared.exceptions

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class SimpleDomainExceptionTest : StringSpec({
    "should create simple exception" {
        val exception = DomainException("TEST_001", "Test message")
        
        exception.code shouldBe "TEST_001"
        exception.message shouldBe "Test message"
    }
})
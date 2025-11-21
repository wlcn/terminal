package org.now.terminal.session.domain.valueobjects

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow

class TerminalSizeTest : StringSpec({
    "should create valid terminal size" {
        val size = TerminalSize.create(24, 80)
        
        size.rows shouldBe 24
        size.columns shouldBe 80
        size.area() shouldBe 1920
        size.isValid() shouldBe true
    }

    "should throw exception for zero rows" {
        shouldThrow<IllegalArgumentException> {
            TerminalSize.create(0, 80)
        }
    }

    "should throw exception for zero columns" {
        shouldThrow<IllegalArgumentException> {
            TerminalSize.create(24, 0)
        }
    }

    "should throw exception for negative rows" {
        shouldThrow<IllegalArgumentException> {
            TerminalSize.create(-1, 80)
        }
    }

    "should throw exception for negative columns" {
        shouldThrow<IllegalArgumentException> {
            TerminalSize.create(24, -1)
        }
    }

    "should parse size from string" {
        val size = TerminalSize.fromString("24x80")
        
        size.rows shouldBe 24
        size.columns shouldBe 80
        size.toString() shouldBe "24x80"
    }

    "should throw exception for invalid format" {
        shouldThrow<IllegalArgumentException> {
            TerminalSize.fromString("24-80")
        }
    }

    "should throw exception for non-numeric values" {
        shouldThrow<IllegalArgumentException> {
            TerminalSize.fromString("abcx80")
        }
    }

    "should compare sizes correctly" {
        val size = TerminalSize.create(24, 80)
        val sameSize = TerminalSize.create(24, 80)
        val largerSize = TerminalSize.create(40, 120)
        
        size.isEqualTo(sameSize) shouldBe true
        size.isLargerThan(largerSize) shouldBe false
        largerSize.isLargerThan(size) shouldBe true
        size.isSmallerThan(largerSize) shouldBe true
    }

    "should have default size" {
        val defaultSize = TerminalSize.DEFAULT
        
        defaultSize.rows shouldBe 24
        defaultSize.columns shouldBe 80
        defaultSize.isValid() shouldBe true
    }

    "should validate size limits" {
        val validSize = TerminalSize.create(24, 80)
        validSize.isValid() shouldBe true
        
        shouldThrow<IllegalArgumentException> {
            TerminalSize.create(1001, 80)
        }
        
        shouldThrow<IllegalArgumentException> {
            TerminalSize.create(24, 1001)
        }
    }

    "should handle equality correctly" {
        val size1 = TerminalSize.create(24, 80)
        val size2 = TerminalSize.create(24, 80)
        val size3 = TerminalSize.create(25, 80)
        
        size1 shouldBe size2
        size1 shouldBe size1
        size1 shouldNotBe size3
    }
})
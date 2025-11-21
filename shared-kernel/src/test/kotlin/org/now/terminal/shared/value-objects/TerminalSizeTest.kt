package org.now.terminal.shared.valueobjects

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.assertions.throwables.shouldThrow

class TerminalSizeTest : StringSpec({
    "should create terminal size with valid dimensions" {
        val size = TerminalSize.create(24, 80)
        
        size.rows shouldBe 24
        size.columns shouldBe 80
        size.isValid() shouldBe true
    }

    "should throw exception for zero rows" {
        val exception = shouldThrow<IllegalArgumentException> {
            TerminalSize.create(0, 80)
        }
        
        exception.message shouldBe "Rows must be positive"
    }

    "should throw exception for zero columns" {
        val exception = shouldThrow<IllegalArgumentException> {
            TerminalSize.create(24, 0)
        }
        
        exception.message shouldBe "Columns must be positive"
    }

    "should throw exception for negative rows" {
        val exception = shouldThrow<IllegalArgumentException> {
            TerminalSize.create(-1, 80)
        }
        
        exception.message shouldBe "Rows must be positive"
    }

    "should throw exception for negative columns" {
        val exception = shouldThrow<IllegalArgumentException> {
            TerminalSize.create(24, -1)
        }
        
        exception.message shouldBe "Columns must be positive"
    }

    "should parse terminal size from string" {
        val size = TerminalSize.fromString("24x80")
        
        size.rows shouldBe 24
        size.columns shouldBe 80
    }

    "should throw exception for invalid format" {
        val exception = shouldThrow<IllegalArgumentException> {
            TerminalSize.fromString("24-80")
        }
        
        exception.message shouldBe "Size string must be in format 'rowsxcolumns'"
    }

    "should throw exception for non-numeric values" {
        val exception = shouldThrow<IllegalArgumentException> {
            TerminalSize.fromString("abcx80")
        }
        
        exception.message shouldBe "Invalid rows format"
    }

    "should calculate area correctly" {
        val size = TerminalSize.create(24, 80)
        
        size.area() shouldBe 1920
    }

    "should check size comparison correctly" {
        val size1 = TerminalSize.create(24, 80)
        val size2 = TerminalSize.create(40, 120)
        val size3 = TerminalSize.create(24, 80)
        
        size2.isLargerThan(size1) shouldBe true
        size1.isSmallerThan(size2) shouldBe true
        size1.isEqualTo(size3) shouldBe true
    }

    "should convert to string representation" {
        val size = TerminalSize.create(24, 80)
        
        size.toString() shouldBe "24x80"
    }

    "should use default size" {
        val defaultSize = TerminalSize.DEFAULT
        
        defaultSize.rows shouldBe 24
        defaultSize.columns shouldBe 80
    }

    "should check validity correctly" {
        val validSize = TerminalSize.create(24, 80)
        
        validSize.isValid() shouldBe true
    }

    "should throw exception for rows exceeding max" {
        val exception = shouldThrow<IllegalArgumentException> {
            TerminalSize.create(1001, 80)
        }
        
        exception.message shouldBe "Rows cannot exceed 1000"
    }

    "should throw exception for columns exceeding max" {
        val exception = shouldThrow<IllegalArgumentException> {
            TerminalSize.create(24, 1001)
        }
        
        exception.message shouldBe "Columns cannot exceed 1000"
    }

    "should be equal when dimensions are same" {
        val size1 = TerminalSize.create(24, 80)
        val size2 = TerminalSize.create(24, 80)
        
        size1 shouldBe size2
        size1.hashCode() shouldBe size2.hashCode()
    }

    "should not be equal when dimensions are different" {
        val size1 = TerminalSize.create(24, 80)
        val size2 = TerminalSize.create(25, 80)
        
        (size1 == size2) shouldBe false
    }
})
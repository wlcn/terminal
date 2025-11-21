package org.now.terminal.shared.valueobjects

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TerminalSizeTest {

    @Test
    fun `should create terminal size with valid dimensions`() {
        val size = TerminalSize.create(24, 80)
        
        assertEquals(24, size.rows)
        assertEquals(80, size.columns)
        assertTrue(size.isValid())
    }

    @Test
    fun `should throw exception for zero rows`() {
        val exception = org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            TerminalSize.create(0, 80)
        }
        
        assertEquals("Rows must be positive", exception.message)
    }

    @Test
    fun `should throw exception for zero columns`() {
        val exception = org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            TerminalSize.create(24, 0)
        }
        
        assertEquals("Columns must be positive", exception.message)
    }

    @Test
    fun `should throw exception for negative rows`() {
        val exception = org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            TerminalSize.create(-1, 80)
        }
        
        assertEquals("Rows must be positive", exception.message)
    }

    @Test
    fun `should throw exception for negative columns`() {
        val exception = org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            TerminalSize.create(24, -1)
        }
        
        assertEquals("Columns must be positive", exception.message)
    }

    @Test
    fun `should parse terminal size from string`() {
        val size = TerminalSize.fromString("24x80")
        
        assertEquals(24, size.rows)
        assertEquals(80, size.columns)
    }

    @Test
    fun `should throw exception for invalid format`() {
        val exception = org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            TerminalSize.fromString("24-80")
        }
        
        assertEquals("Size string must be in format 'rowsxcolumns'", exception.message)
    }

    @Test
    fun `should throw exception for non-numeric values`() {
        val exception = org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            TerminalSize.fromString("abcx80")
        }
        
        assertEquals("Invalid rows format", exception.message)
    }

    @Test
    fun `should calculate area correctly`() {
        val size = TerminalSize.create(24, 80)
        
        assertEquals(1920, size.area())
    }

    @Test
    fun `should check size comparison correctly`() {
        val size1 = TerminalSize.create(24, 80)
        val size2 = TerminalSize.create(40, 120)
        val size3 = TerminalSize.create(24, 80)
        
        assertTrue(size2.isLargerThan(size1))
        assertTrue(size1.isSmallerThan(size2))
        assertTrue(size1.isEqualTo(size3))
    }

    @Test
    fun `should convert to string representation`() {
        val size = TerminalSize.create(24, 80)
        
        assertEquals("24x80", size.toString())
    }

    @Test
    fun `should use default size`() {
        val defaultSize = TerminalSize.DEFAULT
        
        assertEquals(24, defaultSize.rows)
        assertEquals(80, defaultSize.columns)
    }

    @Test
    fun `should check validity correctly`() {
        val validSize = TerminalSize.create(24, 80)
        
        assertTrue(validSize.isValid())
    }

    @Test
    fun `should throw exception for rows exceeding max`() {
        val exception = org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            TerminalSize.create(1001, 80)
        }
        
        assertEquals("Rows cannot exceed 1000", exception.message)
    }

    @Test
    fun `should throw exception for columns exceeding max`() {
        val exception = org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            TerminalSize.create(24, 1001)
        }
        
        assertEquals("Columns cannot exceed 1000", exception.message)
    }

    @Test
    fun `should be equal when dimensions are same`() {
        val size1 = TerminalSize.create(24, 80)
        val size2 = TerminalSize.create(24, 80)
        
        assertEquals(size1, size2)
        assertEquals(size1.hashCode(), size2.hashCode())
    }

    @Test
    fun `should not be equal when dimensions are different`() {
        val size1 = TerminalSize.create(24, 80)
        val size2 = TerminalSize.create(25, 80)
        
        assertFalse(size1 == size2)
    }
}
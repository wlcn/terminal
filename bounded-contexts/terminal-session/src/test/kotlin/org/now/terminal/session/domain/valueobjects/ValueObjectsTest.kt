package org.now.terminal.session.domain.valueobjects

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ValueObjectsTest {
    
    @Test
    fun `should create valid TerminalCommand`() {
        // When
        val command = TerminalCommand("ls -la")
        
        // Then
        assertEquals("ls -la", command.value)
    }
    
    @Test
    fun `should throw exception for blank TerminalCommand`() {
        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            TerminalCommand("   ")
        }
        
        assertEquals("Command cannot be blank", exception.message)
    }
    
    @Test
    fun `should throw exception for too long TerminalCommand`() {
        // Given
        val longCommand = "a".repeat(1025)
        
        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            TerminalCommand(longCommand)
        }
        
        assertEquals("Command too long", exception.message)
    }
    
    @Test
    fun `should create valid TerminalSize`() {
        // When
        val size = TerminalSize(80, 24)
        
        // Then
        assertEquals(80, size.columns)
        assertEquals(24, size.rows)
    }
    
    @Test
    fun `should throw exception for invalid TerminalSize columns`() {
        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            TerminalSize(0, 24)
        }
        
        assertEquals("Columns must be positive", exception.message)
    }
    
    @Test
    fun `should throw exception for invalid TerminalSize rows`() {
        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            TerminalSize(80, 0)
        }
        
        assertEquals("Rows must be positive", exception.message)
    }
    
    @Test
    fun `should throw exception for too large TerminalSize columns`() {
        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            TerminalSize(501, 24)
        }
        
        assertEquals("Columns too large", exception.message)
    }
    
    @Test
    fun `should throw exception for too large TerminalSize rows`() {
        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            TerminalSize(80, 201)
        }
        
        assertEquals("Rows too large", exception.message)
    }
    
    @Test
    fun `should create TerminalSize from string`() {
        // When
        val size = TerminalSize.fromString("80x24")
        
        // Then
        assertEquals(80, size.columns)
        assertEquals(24, size.rows)
    }
    
    @Test
    fun `should throw exception for invalid TerminalSize string format`() {
        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            TerminalSize.fromString("80-24")
        }
        
        assertEquals("Terminal size format should be 'columnsxrows'", exception.message)
    }
    
    @Test
    fun `should create valid PtyConfiguration`() {
        // When
        val config = PtyConfiguration(
            command = TerminalCommand("/bin/bash"),
            environment = mapOf("PATH" to "/usr/bin"),
            size = TerminalSize.DEFAULT
        )
        
        // Then
        assertEquals("/bin/bash", config.command.value)
        assertEquals("/usr/bin", config.environment["PATH"])
        assertEquals(TerminalSize.DEFAULT, config.size)
    }
    
    @Test
    fun `should throw exception for empty environment in PtyConfiguration`() {
        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            PtyConfiguration(
                command = TerminalCommand("/bin/bash"),
                environment = emptyMap(),
                size = TerminalSize.DEFAULT
            )
        }
        
        assertEquals("Environment cannot be empty", exception.message)
    }
    
    @Test
    fun `should create default PtyConfiguration`() {
        // When
        val config = PtyConfiguration.createDefault(TerminalCommand("/bin/bash"))
        
        // Then
        assertEquals("/bin/bash", config.command.value)
        assertTrue(config.environment.isNotEmpty())
        assertEquals(TerminalSize.DEFAULT, config.size)
    }
    
    @Test
    fun `should manage OutputBuffer content`() {
        // Given
        val buffer = OutputBuffer()
        
        // When
        buffer.append("Hello")
        buffer.append(" World")
        
        // Then
        assertEquals("Hello World", buffer.getContent())
        assertEquals(11, buffer.size())
    }
    
    @Test
    fun `should clear OutputBuffer`() {
        // Given
        val buffer = OutputBuffer()
        buffer.append("Test content")
        
        // When
        buffer.clear()
        
        // Then
        assertEquals("", buffer.getContent())
        assertEquals(0, buffer.size())
    }
    
    @Test
    fun `should respect OutputBuffer size limit`() {
        // Given
        val buffer = OutputBuffer()
        val longContent = "a".repeat(100_000 + 100)
        
        // When
        buffer.append(longContent)
        
        // Then
        assertEquals(100_000, buffer.size())
    }
    
    @Test
    fun `should have valid TerminationReason values`() {
        // Then
        assertEquals(5, TerminationReason.values().size)
        assertTrue(TerminationReason.values().contains(TerminationReason.NORMAL))
        assertTrue(TerminationReason.values().contains(TerminationReason.USER_REQUESTED))
        assertTrue(TerminationReason.values().contains(TerminationReason.PROCESS_ERROR))
        assertTrue(TerminationReason.values().contains(TerminationReason.TIMEOUT))
        assertTrue(TerminationReason.values().contains(TerminationReason.SYSTEM_ERROR))
    }
}
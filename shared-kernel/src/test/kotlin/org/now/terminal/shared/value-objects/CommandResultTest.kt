package org.now.terminal.shared.valueobjects

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommandResultTest {

    @Test
    fun `should create success result with default exit code`() {
        val result = CommandResult.Success("Command executed successfully")
        
        assertEquals("Command executed successfully", result.output)
        assertEquals(0, result.code)
        assertTrue(result.isSuccess())
        assertFalse(result.isFailure())
        assertFalse(result.isTimeout())
    }

    @Test
    fun `should create success result with custom exit code`() {
        val result = CommandResult.Success("Command executed", 1)
        
        assertEquals("Command executed", result.output)
        assertEquals(1, result.code)
    }

    @Test
    fun `should create failure result with default exit code`() {
        val result = CommandResult.Failure("Command failed")
        
        assertEquals("Command failed", result.errorMessage)
        assertEquals(1, result.code)
        assertFalse(result.isSuccess())
        assertTrue(result.isFailure())
        assertFalse(result.isTimeout())
    }

    @Test
    fun `should create failure result with custom exit code`() {
        val result = CommandResult.Failure("Permission denied", 126)
        
        assertEquals("Permission denied", result.errorMessage)
        assertEquals(126, result.code)
    }

    @Test
    fun `should create timeout result`() {
        val result = CommandResult.Timeout(5000L)
        
        assertEquals(5000L, result.timeoutMs)
        assertFalse(result.isSuccess())
        assertFalse(result.isFailure())
        assertTrue(result.isTimeout())
    }

    @Test
    fun `should get output from success result`() {
        val result = CommandResult.Success("output text")
        
        assertEquals("output text", result.getOutputOrNull())
    }

    @Test
    fun `should return null output from non-success result`() {
        val failureResult = CommandResult.Failure("error")
        val timeoutResult = CommandResult.Timeout(1000L)
        
        assertNull(failureResult.getOutputOrNull())
        assertNull(timeoutResult.getOutputOrNull())
    }

    @Test
    fun `should get error message from failure result`() {
        val result = CommandResult.Failure("error occurred")
        
        assertEquals("error occurred", result.getErrorMessageOrNull())
    }

    @Test
    fun `should return null error message from non-failure result`() {
        val successResult = CommandResult.Success("output")
        val timeoutResult = CommandResult.Timeout(1000L)
        
        assertNull(successResult.getErrorMessageOrNull())
        assertNull(timeoutResult.getErrorMessageOrNull())
    }

    @Test
    fun `should get exit code from success result`() {
        val result = CommandResult.Success("output", 0)
        
        assertEquals(0, result.getExitCode())
    }

    @Test
    fun `should get exit code from failure result`() {
        val result = CommandResult.Failure("error", 127)
        
        assertEquals(127, result.getExitCode())
    }

    @Test
    fun `should get exit code -1 from timeout result`() {
        val result = CommandResult.Timeout(1000L)
        
        assertEquals(-1, result.getExitCode())
    }

    @Test
    fun `should check result types correctly`() {
        val success = CommandResult.Success("success")
        val failure = CommandResult.Failure("failure")
        val timeout = CommandResult.Timeout(1000L)
        
        assertTrue(success.isSuccess())
        assertFalse(success.isFailure())
        assertFalse(success.isTimeout())
        
        assertFalse(failure.isSuccess())
        assertTrue(failure.isFailure())
        assertFalse(failure.isTimeout())
        
        assertFalse(timeout.isSuccess())
        assertFalse(timeout.isFailure())
        assertTrue(timeout.isTimeout())
    }

    @Test
    fun `should use when expression with sealed class`() {
        val success = CommandResult.Success("success")
        val failure = CommandResult.Failure("failure")
        val timeout = CommandResult.Timeout(1000L)
        
        val successType = when (success) {
            is CommandResult.Success -> "success"
            is CommandResult.Failure -> "failure"
            is CommandResult.Timeout -> "timeout"
        }
        
        val failureType = when (failure) {
            is CommandResult.Success -> "success"
            is CommandResult.Failure -> "failure"
            is CommandResult.Timeout -> "timeout"
        }
        
        val timeoutType = when (timeout) {
            is CommandResult.Success -> "success"
            is CommandResult.Failure -> "failure"
            is CommandResult.Timeout -> "timeout"
        }
        
        assertEquals("success", successType)
        assertEquals("failure", failureType)
        assertEquals("timeout", timeoutType)
    }
}
package org.now.terminal.shared.exceptions

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DomainExceptionTest {

    @Test
    fun `should create invalid user id exception`() {
        val exception = DomainException.InvalidUserId("invalid-user-id")
        
        assertEquals("invalid-user-id", exception.userId)
        assertEquals("Invalid user ID: invalid-user-id", exception.message)
        assertIs<DomainException>(exception)
    }

    @Test
    fun `should create invalid session id exception`() {
        val exception = DomainException.InvalidSessionId("invalid-session-id")
        
        assertEquals("invalid-session-id", exception.sessionId)
        assertEquals("Invalid session ID: invalid-session-id", exception.message)
        assertIs<DomainException>(exception)
    }

    @Test
    fun `should create invalid terminal size exception`() {
        val exception = DomainException.InvalidTerminalSize(0, 0)
        
        assertEquals(0, exception.columns)
        assertEquals(0, exception.rows)
        assertEquals("Invalid terminal size: 0x0", exception.message)
    }

    @Test
    fun `should create invalid event id exception`() {
        val exception = DomainException.InvalidEventId("invalid-event-id")
        
        assertEquals("invalid-event-id", exception.eventId)
        assertEquals("Invalid event ID: invalid-event-id", exception.message)
    }

    @Test
    fun `should create session not found exception`() {
        val exception = DomainException.SessionNotFound("session-123")
        
        assertEquals("session-123", exception.sessionId)
        assertEquals("Session not found: session-123", exception.message)
    }

    @Test
    fun `should create user not found exception`() {
        val exception = DomainException.UserNotFound("user-456")
        
        assertEquals("user-456", exception.userId)
        assertEquals("User not found: user-456", exception.message)
    }

    @Test
    fun `should create session already exists exception`() {
        val exception = DomainException.SessionAlreadyExists("session-123")
        
        assertEquals("session-123", exception.sessionId)
        assertEquals("Session already exists: session-123", exception.message)
    }

    @Test
    fun `should create permission denied exception`() {
        val exception = DomainException.PermissionDenied("user-456", "session-123")
        
        assertEquals("user-456", exception.userId)
        assertEquals("session-123", exception.sessionId)
        assertEquals("Permission denied for user user-456 on session session-123", exception.message)
    }

    @Test
    fun `should create command execution failed exception`() {
        val exception = DomainException.CommandExecutionFailed("ls -la", "Permission denied")
        
        assertEquals("ls -la", exception.command)
        assertEquals("Permission denied", exception.error)
        assertEquals("Command execution failed: ls -la - Permission denied", exception.message)
    }

    @Test
    fun `should create command timeout exception`() {
        val exception = DomainException.CommandTimeout("sleep 10", 5000L)
        
        assertEquals("sleep 10", exception.command)
        assertEquals(5000L, exception.timeoutMs)
        assertEquals("Command timeout: sleep 10 (timeout: 5000ms)", exception.message)
    }

    @Test
    fun `should create system unavailable exception`() {
        val exception = DomainException.SystemUnavailable("session-manager")
        
        assertEquals("session-manager", exception.component)
        assertEquals("System unavailable: session-manager", exception.message)
    }

    @Test
    fun `should check exception inheritance hierarchy`() {
        val invalidUserId = DomainException.InvalidUserId("test")
        val sessionNotFound = DomainException.SessionNotFound("test")
        val commandFailed = DomainException.CommandExecutionFailed("test", "error")
        
        assertTrue(invalidUserId is DomainException)
        assertTrue(sessionNotFound is DomainException)
        assertTrue(commandFailed is DomainException)
        
        assertTrue(invalidUserId is RuntimeException)
        assertTrue(sessionNotFound is RuntimeException)
        assertTrue(commandFailed is RuntimeException)
    }

    @Test
    fun `should use when expression with sealed class`() {
        val invalidUserId = DomainException.InvalidUserId("user-1")
        val sessionNotFound = DomainException.SessionNotFound("session-1")
        val permissionDenied = DomainException.PermissionDenied("user-1", "session-1")
        
        val userIdType = when (invalidUserId) {
            is DomainException.InvalidUserId -> "invalid_user_id"
            is DomainException.InvalidSessionId -> "invalid_session_id"
            is DomainException.SessionNotFound -> "session_not_found"
            is DomainException.UserNotFound -> "user_not_found"
            is DomainException.PermissionDenied -> "permission_denied"
            is DomainException.CommandExecutionFailed -> "command_failed"
            is DomainException.CommandTimeout -> "command_timeout"
            is DomainException.SystemUnavailable -> "system_unavailable"
            is DomainException.InvalidTerminalSize -> "invalid_terminal_size"
            is DomainException.InvalidEventId -> "invalid_event_id"
            is DomainException.SessionAlreadyExists -> "session_already_exists"
        }
        
        val sessionNotFoundType = when (sessionNotFound) {
            is DomainException.InvalidUserId -> "invalid_user_id"
            is DomainException.InvalidSessionId -> "invalid_session_id"
            is DomainException.SessionNotFound -> "session_not_found"
            is DomainException.UserNotFound -> "user_not_found"
            is DomainException.PermissionDenied -> "permission_denied"
            is DomainException.CommandExecutionFailed -> "command_failed"
            is DomainException.CommandTimeout -> "command_timeout"
            is DomainException.SystemUnavailable -> "system_unavailable"
            is DomainException.InvalidTerminalSize -> "invalid_terminal_size"
            is DomainException.InvalidEventId -> "invalid_event_id"
            is DomainException.SessionAlreadyExists -> "session_already_exists"
        }
        
        val permissionDeniedType = when (permissionDenied) {
            is DomainException.InvalidUserId -> "invalid_user_id"
            is DomainException.InvalidSessionId -> "invalid_session_id"
            is DomainException.SessionNotFound -> "session_not_found"
            is DomainException.UserNotFound -> "user_not_found"
            is DomainException.PermissionDenied -> "permission_denied"
            is DomainException.CommandExecutionFailed -> "command_failed"
            is DomainException.CommandTimeout -> "command_timeout"
            is DomainException.SystemUnavailable -> "system_unavailable"
            is DomainException.InvalidTerminalSize -> "invalid_terminal_size"
            is DomainException.InvalidEventId -> "invalid_event_id"
            is DomainException.SessionAlreadyExists -> "session_already_exists"
        }
        
        assertEquals("invalid_user_id", userIdType)
        assertEquals("session_not_found", sessionNotFoundType)
        assertEquals("permission_denied", permissionDeniedType)
    }
}
package org.now.terminal.shared.events

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionLifecycleEventTest {

    @Test
    fun `should create session created event`() {
        val event = SessionLifecycleEvent.SessionCreated(
            sessionId = "session-123",
            userId = "user-456",
            terminalSize = SessionLifecycleEvent.TerminalSize(80, 24),
            command = "/bin/bash",
            workingDirectory = "/home/user"
        )
        
        assertEquals("session-123", event.sessionId)
        assertEquals("user-456", event.userId)
        assertEquals(80, event.terminalSize.columns)
        assertEquals(24, event.terminalSize.rows)
        assertEquals("/bin/bash", event.command)
        assertEquals("/home/user", event.workingDirectory)
        assertTrue(event.isSessionCreated())
    }

    @Test
    fun `should create session terminated event`() {
        val event = SessionLifecycleEvent.SessionTerminated(
            sessionId = "session-123",
            exitCode = 0,
            reason = SessionLifecycleEvent.TerminationReason.NORMAL
        )
        
        assertEquals("session-123", event.sessionId)
        assertEquals(0, event.exitCode)
        assertEquals(SessionLifecycleEvent.TerminationReason.NORMAL, event.reason)
        assertTrue(event.isSessionTerminated())
    }

    @Test
    fun `should create session resized event`() {
        val event = SessionLifecycleEvent.SessionResized(
            sessionId = "session-123",
            newSize = SessionLifecycleEvent.TerminalSize(120, 40)
        )
        
        assertEquals("session-123", event.sessionId)
        assertEquals(120, event.newSize.columns)
        assertEquals(40, event.newSize.rows)
        assertTrue(event.isSessionResized())
    }

    @Test
    fun `should create session activity event`() {
        val event = SessionLifecycleEvent.SessionActivity(
            sessionId = "session-123",
            activityType = SessionLifecycleEvent.ActivityType.INPUT,
            data = "ls -la"
        )
        
        assertEquals("session-123", event.sessionId)
        assertEquals(SessionLifecycleEvent.ActivityType.INPUT, event.activityType)
        assertEquals("ls -la", event.data)
        assertTrue(event.isSessionActivity())
    }

    @Test
    fun `should check event types correctly`() {
        val created = SessionLifecycleEvent.SessionCreated(
            sessionId = "session-1",
            userId = "user-1",
            terminalSize = SessionLifecycleEvent.TerminalSize(80, 24),
            command = "/bin/bash",
            workingDirectory = "/home/user"
        )
        
        val terminated = SessionLifecycleEvent.SessionTerminated(
            sessionId = "session-1",
            exitCode = 0,
            reason = SessionLifecycleEvent.TerminationReason.NORMAL
        )
        
        val resized = SessionLifecycleEvent.SessionResized(
            sessionId = "session-1",
            newSize = SessionLifecycleEvent.TerminalSize(120, 40)
        )
        
        val activity = SessionLifecycleEvent.SessionActivity(
            sessionId = "session-1",
            activityType = SessionLifecycleEvent.ActivityType.OUTPUT,
            data = "output data"
        )
        
        assertTrue(created.isSessionCreated())
        assertFalse(created.isSessionTerminated())
        assertFalse(created.isSessionResized())
        assertFalse(created.isSessionActivity())
        
        assertFalse(terminated.isSessionCreated())
        assertTrue(terminated.isSessionTerminated())
        assertFalse(terminated.isSessionResized())
        assertFalse(terminated.isSessionActivity())
        
        assertFalse(resized.isSessionCreated())
        assertFalse(resized.isSessionTerminated())
        assertTrue(resized.isSessionResized())
        assertFalse(resized.isSessionActivity())
        
        assertFalse(activity.isSessionCreated())
        assertFalse(activity.isSessionTerminated())
        assertFalse(activity.isSessionResized())
        assertTrue(activity.isSessionActivity())
    }

    @Test
    fun `should use when expression with sealed class`() {
        val created = SessionLifecycleEvent.SessionCreated(
            sessionId = "session-1",
            userId = "user-1",
            terminalSize = SessionLifecycleEvent.TerminalSize(80, 24),
            command = "/bin/bash",
            workingDirectory = "/home/user"
        )
        
        val terminated = SessionLifecycleEvent.SessionTerminated(
            sessionId = "session-1",
            exitCode = 1,
            reason = SessionLifecycleEvent.TerminationReason.ERROR
        )
        
        val resized = SessionLifecycleEvent.SessionResized(
            sessionId = "session-1",
            newSize = SessionLifecycleEvent.TerminalSize(120, 40)
        )
        
        val activity = SessionLifecycleEvent.SessionActivity(
            sessionId = "session-1",
            activityType = SessionLifecycleEvent.ActivityType.INPUT,
            data = "command"
        )
        
        val createdType = when (created) {
            is SessionLifecycleEvent.SessionCreated -> "created"
            is SessionLifecycleEvent.SessionTerminated -> "terminated"
            is SessionLifecycleEvent.SessionResized -> "resized"
            is SessionLifecycleEvent.SessionActivity -> "activity"
        }
        
        val terminatedType = when (terminated) {
            is SessionLifecycleEvent.SessionCreated -> "created"
            is SessionLifecycleEvent.SessionTerminated -> "terminated"
            is SessionLifecycleEvent.SessionResized -> "resized"
            is SessionLifecycleEvent.SessionActivity -> "activity"
        }
        
        val resizedType = when (resized) {
            is SessionLifecycleEvent.SessionCreated -> "created"
            is SessionLifecycleEvent.SessionTerminated -> "terminated"
            is SessionLifecycleEvent.SessionResized -> "resized"
            is SessionLifecycleEvent.SessionActivity -> "activity"
        }
        
        val activityType = when (activity) {
            is SessionLifecycleEvent.SessionCreated -> "created"
            is SessionLifecycleEvent.SessionTerminated -> "terminated"
            is SessionLifecycleEvent.SessionResized -> "resized"
            is SessionLifecycleEvent.SessionActivity -> "activity"
        }
        
        assertEquals("created", createdType)
        assertEquals("terminated", terminatedType)
        assertEquals("resized", resizedType)
        assertEquals("activity", activityType)
    }

    @Test
    fun `should have correct termination reason values`() {
        val reasons = SessionLifecycleEvent.TerminationReason.values()
        
        assertEquals(4, reasons.size)
        assertTrue(reasons.contains(SessionLifecycleEvent.TerminationReason.NORMAL))
        assertTrue(reasons.contains(SessionLifecycleEvent.TerminationReason.ERROR))
        assertTrue(reasons.contains(SessionLifecycleEvent.TerminationReason.TIMEOUT))
        assertTrue(reasons.contains(SessionLifecycleEvent.TerminationReason.FORCE))
    }

    @Test
    fun `should have correct activity type values`() {
        val activityTypes = SessionLifecycleEvent.ActivityType.values()
        
        assertEquals(3, activityTypes.size)
        assertTrue(activityTypes.contains(SessionLifecycleEvent.ActivityType.INPUT))
        assertTrue(activityTypes.contains(SessionLifecycleEvent.ActivityType.OUTPUT))
        assertTrue(activityTypes.contains(SessionLifecycleEvent.ActivityType.ERROR))
    }

    @Test
    fun `should create terminal size correctly`() {
        val size = SessionLifecycleEvent.TerminalSize(80, 24)
        
        assertEquals(80, size.columns)
        assertEquals(24, size.rows)
        assertEquals(1920, size.area) // 80 * 24
    }

    @Test
    fun `should calculate terminal area correctly`() {
        val size1 = SessionLifecycleEvent.TerminalSize(80, 24)
        val size2 = SessionLifecycleEvent.TerminalSize(120, 40)
        val size3 = SessionLifecycleEvent.TerminalSize(40, 20)
        
        assertEquals(1920, size1.area)
        assertEquals(4800, size2.area)
        assertEquals(800, size3.area)
    }
}
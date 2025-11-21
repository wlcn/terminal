package org.now.terminal.shared.events

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldContain
import org.now.terminal.shared.valueobjects.SessionId
import org.now.terminal.shared.valueobjects.UserId
import java.time.Instant

class SessionLifecycleEventTest : StringSpec({
    "should create session created event" {
        val sessionId = SessionId.fromString("ses_12345678-1234-1234-1234-123456789012")
        val userId = UserId.fromString("usr_12345678-1234-1234-1234-123456789012")
        
        val event = SessionLifecycleEvent.SessionCreated(
            sessionId = sessionId,
            userId = userId,
            terminalType = "xterm",
            initialSize = "24x80",
            environment = mapOf("PATH" to "/usr/bin")
        )
        
        event.sessionId shouldBe sessionId
        event.userId shouldBe userId
        event.terminalType shouldBe "xterm"
        event.initialSize shouldBe "24x80"
        event.environment["PATH"] shouldBe "/usr/bin"
        event.isCreationEvent() shouldBe true
        event.isTerminationEvent() shouldBe false
    }

    "should create session terminated event" {
        val sessionId = SessionId.fromString("ses_12345678-1234-1234-1234-123456789012")
        val userId = UserId.fromString("usr_12345678-1234-1234-1234-123456789012")
        
        val event = SessionLifecycleEvent.SessionTerminated(
            sessionId = sessionId,
            userId = userId,
            exitCode = 0,
            reason = SessionLifecycleEvent.TerminationReason.USER_REQUEST
        )
        
        event.sessionId shouldBe sessionId
        event.userId shouldBe userId
        event.exitCode shouldBe 0
        event.reason shouldBe SessionLifecycleEvent.TerminationReason.USER_REQUEST
        event.isCreationEvent() shouldBe false
        event.isTerminationEvent() shouldBe true
    }

    "should create session active event" {
        val sessionId = SessionId.fromString("ses_12345678-1234-1234-1234-123456789012")
        val userId = UserId.fromString("usr_12345678-1234-1234-1234-123456789012")
        
        val event = SessionLifecycleEvent.SessionActive(
            sessionId = sessionId,
            userId = userId,
            lastActivityAt = Instant.now(),
            commandCount = 5
        )
        
        event.sessionId shouldBe sessionId
        event.userId shouldBe userId
        event.lastActivityAt shouldNotBe null
        event.commandCount shouldBe 5
        event.isCreationEvent() shouldBe false
        event.isTerminationEvent() shouldBe false
    }

    "should create session idle event" {
        val sessionId = SessionId.fromString("ses_12345678-1234-1234-1234-123456789012")
        val userId = UserId.fromString("usr_12345678-1234-1234-1234-123456789012")
        
        val event = SessionLifecycleEvent.SessionIdle(
            sessionId = sessionId,
            userId = userId,
            idleDurationSeconds = 300L
        )
        
        event.sessionId shouldBe sessionId
        event.userId shouldBe userId
        event.idleDurationSeconds shouldBe 300L
        event.isCreationEvent() shouldBe false
        event.isTerminationEvent() shouldBe false
    }

    "should check event types correctly" {
        val sessionId = SessionId.fromString("ses_12345678-1234-1234-1234-123456789012")
        val userId = UserId.fromString("usr_12345678-1234-1234-1234-123456789012")
        
        val created = SessionLifecycleEvent.SessionCreated(
            sessionId = sessionId,
            userId = userId,
            terminalType = "xterm",
            initialSize = "24x80",
            environment = mapOf("PATH" to "/usr/bin")
        )
        
        val terminated = SessionLifecycleEvent.SessionTerminated(
            sessionId = sessionId,
            userId = userId,
            exitCode = 0,
            reason = SessionLifecycleEvent.TerminationReason.USER_REQUEST
        )
        
        val active = SessionLifecycleEvent.SessionActive(
            sessionId = sessionId,
            userId = userId,
            lastActivityAt = Instant.now(),
            commandCount = 5
        )
        
        val idle = SessionLifecycleEvent.SessionIdle(
            sessionId = sessionId,
            userId = userId,
            idleDurationSeconds = 300L
        )
        
        created.isCreationEvent() shouldBe true
        created.isTerminationEvent() shouldBe false
        
        terminated.isCreationEvent() shouldBe false
        terminated.isTerminationEvent() shouldBe true
        
        active.isCreationEvent() shouldBe false
        active.isTerminationEvent() shouldBe false
        
        idle.isCreationEvent() shouldBe false
        idle.isTerminationEvent() shouldBe false
    }

    "should use when expression with sealed class" {
        val sessionId = SessionId.fromString("ses_12345678-1234-1234-1234-123456789012")
        val userId = UserId.fromString("usr_12345678-1234-1234-1234-123456789012")
        
        val created = SessionLifecycleEvent.SessionCreated(
            sessionId = sessionId,
            userId = userId,
            terminalType = "xterm",
            initialSize = "24x80",
            environment = mapOf("PATH" to "/usr/bin")
        )
        
        val terminated = SessionLifecycleEvent.SessionTerminated(
            sessionId = sessionId,
            userId = userId,
            exitCode = 1,
            reason = SessionLifecycleEvent.TerminationReason.ERROR
        )
        
        val active = SessionLifecycleEvent.SessionActive(
            sessionId = sessionId,
            userId = userId,
            lastActivityAt = Instant.now(),
            commandCount = 3
        )
        
        val idle = SessionLifecycleEvent.SessionIdle(
            sessionId = sessionId,
            userId = userId,
            idleDurationSeconds = 300L
        )
        
        val createdType = when (created) {
            is SessionLifecycleEvent.SessionCreated -> "created"
            is SessionLifecycleEvent.SessionTerminated -> "terminated"
            is SessionLifecycleEvent.SessionActive -> "active"
            is SessionLifecycleEvent.SessionIdle -> "idle"
        }
        
        val terminatedType = when (terminated) {
            is SessionLifecycleEvent.SessionCreated -> "created"
            is SessionLifecycleEvent.SessionTerminated -> "terminated"
            is SessionLifecycleEvent.SessionActive -> "active"
            is SessionLifecycleEvent.SessionIdle -> "idle"
        }
        
        val activeType = when (active) {
            is SessionLifecycleEvent.SessionCreated -> "created"
            is SessionLifecycleEvent.SessionTerminated -> "terminated"
            is SessionLifecycleEvent.SessionActive -> "active"
            is SessionLifecycleEvent.SessionIdle -> "idle"
        }
        
        val idleType = when (idle) {
            is SessionLifecycleEvent.SessionCreated -> "created"
            is SessionLifecycleEvent.SessionTerminated -> "terminated"
            is SessionLifecycleEvent.SessionActive -> "active"
            is SessionLifecycleEvent.SessionIdle -> "idle"
        }
        
        createdType shouldBe "created"
        terminatedType shouldBe "terminated"
        activeType shouldBe "active"
        idleType shouldBe "idle"
    }

    "should have correct termination reason values" {
        val reasons = SessionLifecycleEvent.TerminationReason.values()
        
        reasons.size shouldBe 5
        reasons shouldContain SessionLifecycleEvent.TerminationReason.USER_REQUEST
        reasons shouldContain SessionLifecycleEvent.TerminationReason.ERROR
        reasons shouldContain SessionLifecycleEvent.TerminationReason.TIMEOUT
        reasons shouldContain SessionLifecycleEvent.TerminationReason.SYSTEM_SHUTDOWN
        reasons shouldContain SessionLifecycleEvent.TerminationReason.RESOURCE_LIMIT
    }

    "should handle all termination reasons correctly" {
        SessionLifecycleEvent.TerminationReason.USER_REQUEST.name shouldBe "USER_REQUEST"
        SessionLifecycleEvent.TerminationReason.ERROR.name shouldBe "ERROR"
        SessionLifecycleEvent.TerminationReason.TIMEOUT.name shouldBe "TIMEOUT"
        SessionLifecycleEvent.TerminationReason.SYSTEM_SHUTDOWN.name shouldBe "SYSTEM_SHUTDOWN"
        SessionLifecycleEvent.TerminationReason.RESOURCE_LIMIT.name shouldBe "RESOURCE_LIMIT"
    }
})
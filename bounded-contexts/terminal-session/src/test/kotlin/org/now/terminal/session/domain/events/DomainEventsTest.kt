package org.now.terminal.session.domain.events

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.now.terminal.infrastructure.configuration.ConfigurationManager
import org.now.terminal.shared.events.EventHelper
import org.now.terminal.shared.valueobjects.EventId
import org.now.terminal.shared.valueobjects.SessionId
import org.now.terminal.shared.valueobjects.UserId
import org.now.terminal.session.domain.valueobjects.PtyConfiguration
import org.now.terminal.session.domain.valueobjects.TerminalSize
import org.now.terminal.session.domain.valueobjects.TerminalCommand
import org.now.terminal.session.domain.valueobjects.TerminationReason
import java.time.Instant

class DomainEventsTest : BehaviorSpec({
    
    beforeSpec {
        // 初始化配置管理器
        ConfigurationManager.initialize(environment = "test")
    }
    
    afterSpec {
        // 清理配置管理器
        ConfigurationManager.reset()
    }
    
    given("SessionCreatedEvent") {
        val eventHelper = EventHelper(eventType = "session-created")
        val sessionId = SessionId.generate()
        val userId = UserId.generate()
        val ptyConfig = PtyConfiguration(
            command = TerminalCommand("/bin/bash"),
            environment = mapOf("TERM" to "xterm-256color"),
            size = TerminalSize(80, 24),
            workingDirectory = "/home/user"
        )
        val createdAt = Instant.now()
        
        `when`("创建SessionCreatedEvent实例") {
            val event = SessionCreatedEvent(eventHelper, sessionId, userId, ptyConfig, createdAt)
            
            then("应该成功创建事件实例") {
                event.shouldBeInstanceOf<SessionCreatedEvent>()
                event.eventHelper shouldBe eventHelper
                event.sessionId shouldBe sessionId
                event.userId shouldBe userId
                event.configuration shouldBe ptyConfig
                event.createdAt shouldBe createdAt
            }
        }
    }
    
    given("SessionTerminatedEvent") {
        val eventHelper = EventHelper(eventType = "session-terminated")
        val sessionId = SessionId.generate()
        val exitCode = 0
        val terminatedAt = Instant.now()
        
        `when`("创建SessionTerminatedEvent实例") {
            val event = SessionTerminatedEvent(eventHelper, sessionId, TerminationReason.NORMAL, exitCode, terminatedAt)
            
            then("应该成功创建事件实例") {
                event.shouldBeInstanceOf<SessionTerminatedEvent>()
                event.eventHelper shouldBe eventHelper
                event.sessionId shouldBe sessionId
                event.reason shouldBe TerminationReason.NORMAL
                event.exitCode shouldBe exitCode
                event.terminatedAt shouldBe terminatedAt
            }
        }
    }
    
    given("TerminalInputProcessedEvent") {
        val eventHelper = EventHelper(eventType = "terminal-input-processed")
        val sessionId = SessionId.generate()
        val input = "ls -la"
        val processedAt = Instant.now()
        
        `when`("创建TerminalInputProcessedEvent实例") {
            val event = TerminalInputProcessedEvent(eventHelper, sessionId, input, processedAt)
            
            then("应该成功创建事件实例") {
                event.shouldBeInstanceOf<TerminalInputProcessedEvent>()
                event.eventHelper shouldBe eventHelper
                event.sessionId shouldBe sessionId
                event.input shouldBe input
                event.processedAt shouldBe processedAt
            }
        }
    }
    
    given("TerminalOutputEvent") {
        val eventHelper = EventHelper(eventType = "terminal-output")
        val sessionId = SessionId.generate()
        val output = "total 16"
        val outputAt = Instant.now()
        
        `when`("创建TerminalOutputEvent实例") {
            val event = TerminalOutputEvent(eventHelper, sessionId, output, outputAt)
            
            then("应该成功创建事件实例") {
                event.shouldBeInstanceOf<TerminalOutputEvent>()
                event.eventHelper shouldBe eventHelper
                event.sessionId shouldBe sessionId
                event.output shouldBe output
                event.outputAt shouldBe outputAt
            }
        }
    }
    
    given("TerminalResizedEvent") {
        val eventHelper = EventHelper(eventType = "terminal-resized")
        val sessionId = SessionId.generate()
        val newSize = TerminalSize(120, 40)
        val resizedAt = Instant.now()
        
        `when`("创建TerminalResizedEvent实例") {
            val event = TerminalResizedEvent(eventHelper, sessionId, newSize.columns, newSize.rows, resizedAt)
            
            then("应该成功创建事件实例") {
                event.shouldBeInstanceOf<TerminalResizedEvent>()
                event.eventHelper shouldBe eventHelper
                event.sessionId shouldBe sessionId
                event.columns shouldBe newSize.columns
                event.rows shouldBe newSize.rows
                event.resizedAt shouldBe resizedAt
            }
        }
    }
    
    given("事件相等性测试") {
        val eventHelper = EventHelper(eventType = "test-event")
        val sessionId = SessionId.generate()
        val userId = UserId.generate()
        val ptyConfig = PtyConfiguration(
            command = TerminalCommand("/bin/bash"),
            environment = mapOf("TERM" to "xterm-256color"),
            size = TerminalSize(80, 24),
            workingDirectory = "/home/user"
        )
        val timestamp = Instant.now()
        
        `when`("比较相同的事件") {
            val event1 = SessionCreatedEvent(eventHelper, sessionId, userId, ptyConfig, timestamp)
            val event2 = SessionCreatedEvent(eventHelper, sessionId, userId, ptyConfig, timestamp)
            
            then("应该相等") {
                event1 shouldBe event2
                event1.hashCode() shouldBe event2.hashCode()
            }
        }
        
        `when`("比较不同的事件") {
            val event1 = SessionCreatedEvent(eventHelper, sessionId, userId, ptyConfig, timestamp)
            val event2 = SessionCreatedEvent(EventHelper(eventType = "different"), sessionId, userId, ptyConfig, timestamp)
            
            then("应该不相等") {
                event1 shouldNotBe event2
            }
        }
    }
})
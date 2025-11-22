package org.now.terminal.session.application.handlers

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.now.terminal.session.domain.events.TerminalOutputEvent
import org.now.terminal.session.domain.services.TerminalOutputPublisher
import org.now.terminal.shared.events.EventHelper
import org.now.terminal.shared.valueobjects.SessionId
import java.time.Instant

class TerminalOutputEventHandlerTest : BehaviorSpec({
    
    given("TerminalOutputEventHandler测试") {
        
        `when`("处理终端输出事件") {
            then("应该正确调用发布器") {
                runBlocking {
                    // Given
                    val mockPublisher = mockk<TerminalOutputPublisher>(relaxed = true)
                    val handler = TerminalOutputEventHandler(mockPublisher)
                    val sessionId = SessionId.generate()
                    val eventHelper = EventHelper(
                        eventType = "TerminalOutputEvent",
                        aggregateId = sessionId.value,
                        aggregateType = "TerminalSession"
                    )
                    val outputEvent = TerminalOutputEvent(eventHelper, sessionId, "Test output content", Instant.now())
                    
                    // When
                    handler.handle(outputEvent)
                    
                    // Then
                    coVerify { mockPublisher.publishOutput(sessionId, "Test output content") }
                }
            }
            
            then("应该正确识别事件类型") {
                runBlocking {
                    // Given
                    val mockPublisher = mockk<TerminalOutputPublisher>(relaxed = true)
                    val handler = TerminalOutputEventHandler(mockPublisher)
                    
                    // When & Then
                    handler.canHandle("TerminalOutputEvent").shouldBe(true)
                    handler.canHandle("OtherEvent").shouldBe(false)
                    handler.canHandle("SessionCreatedEvent").shouldBe(false)
                    handler.canHandle("SessionTerminatedEvent").shouldBe(false)
                }
            }
            
            then("应该处理多个输出事件") {
                runBlocking {
                    // Given
                    val mockPublisher = mockk<TerminalOutputPublisher>(relaxed = true)
                    val handler = TerminalOutputEventHandler(mockPublisher)
                    val sessionId1 = SessionId.generate()
                    val sessionId2 = SessionId.generate()
                    
                    val event1 = TerminalOutputEvent(
                        EventHelper(eventType = "TerminalOutputEvent", aggregateId = sessionId1.value, aggregateType = "TerminalSession"),
                        sessionId1, "First output", Instant.now()
                    )
                    val event2 = TerminalOutputEvent(
                        EventHelper(eventType = "TerminalOutputEvent", aggregateId = sessionId2.value, aggregateType = "TerminalSession"),
                        sessionId2, "Second output", Instant.now()
                    )
                    
                    // When
                    handler.handle(event1)
                    handler.handle(event2)
                    
                    // Then
                    coVerify { mockPublisher.publishOutput(sessionId1, "First output") }
                    coVerify { mockPublisher.publishOutput(sessionId2, "Second output") }
                }
            }
            
            then("应该处理空输出内容") {
                runBlocking {
                    // Given
                    val mockPublisher = mockk<TerminalOutputPublisher>(relaxed = true)
                    val handler = TerminalOutputEventHandler(mockPublisher)
                    val sessionId = SessionId.generate()
                    val emptyOutputEvent = TerminalOutputEvent(
                        EventHelper(eventType = "TerminalOutputEvent", aggregateId = sessionId.value, aggregateType = "TerminalSession"),
                        sessionId, "", Instant.now()
                    )
                    
                    // When
                    handler.handle(emptyOutputEvent)
                    
                    // Then
                    coVerify { mockPublisher.publishOutput(sessionId, "") }
                }
            }
            
            then("应该处理特殊字符输出") {
                runBlocking {
                    // Given
                    val mockPublisher = mockk<TerminalOutputPublisher>(relaxed = true)
                    val handler = TerminalOutputEventHandler(mockPublisher)
                    val sessionId = SessionId.generate()
                    val specialOutputEvent = TerminalOutputEvent(
                        EventHelper(eventType = "TerminalOutputEvent", aggregateId = sessionId.value, aggregateType = "TerminalSession"),
                        sessionId, "特殊字符：中文、emoji 😊、特殊符号@#$%", Instant.now()
                    )
                    
                    // When
                    handler.handle(specialOutputEvent)
                    
                    // Then
                    coVerify { mockPublisher.publishOutput(sessionId, "特殊字符：中文、emoji 😊、特殊符号@#$%") }
                }
            }
        }
    }
})
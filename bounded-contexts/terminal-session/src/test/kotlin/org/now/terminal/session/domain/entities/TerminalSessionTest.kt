package org.now.terminal.session.domain.entities

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.mockk.*
import org.now.terminal.infrastructure.configuration.ConfigurationManager
import org.now.terminal.infrastructure.eventbus.EventBus
import org.now.terminal.shared.events.Event
import org.now.terminal.shared.valueobjects.SessionId
import org.now.terminal.shared.valueobjects.UserId
import org.now.terminal.session.domain.services.Process
import org.now.terminal.session.domain.services.ProcessFactory
import org.now.terminal.session.domain.valueobjects.PtyConfiguration
import org.now.terminal.session.domain.valueobjects.TerminalCommand
import org.now.terminal.session.domain.valueobjects.TerminalSize
import org.now.terminal.session.domain.valueobjects.TerminationReason
import org.now.terminal.session.domain.events.SessionCreatedEvent
import org.now.terminal.session.domain.events.TerminalInputProcessedEvent
import org.now.terminal.session.domain.events.TerminalResizedEvent
import org.now.terminal.session.domain.events.SessionTerminatedEvent

class TerminalSessionTest : BehaviorSpec({
    
    lateinit var session: TerminalSession
    lateinit var mockEventBus: EventBus
    lateinit var mockProcessFactory: ProcessFactory
    lateinit var mockProcess: Process
    var sessionId: SessionId? = null
    var userId: UserId? = null
    lateinit var ptyConfig: PtyConfiguration
    
    beforeTest {
        // 初始化配置管理器（用于测试环境）
        ConfigurationManager.initialize(environment = "test")
        
        mockEventBus = mockk()
        mockProcessFactory = mockk()
        mockProcess = mockk()
        sessionId = SessionId.generate()
        userId = UserId.generate()
        ptyConfig = PtyConfiguration.createDefault(TerminalCommand("/bin/bash"))
        
        session = TerminalSession(sessionId!!, userId!!, ptyConfig, mockProcessFactory)
    }
    
    afterTest {
        // 清理配置管理器
        ConfigurationManager.reset()
    }
    
    given("一个终端会话") {
        
        `when`("创建会话时") {
            then("应该设置正确的属性") {
                // Given
                val expectedSessionId = sessionId!!
                val expectedUserId = userId!!
                val expectedPtyConfig = ptyConfig
                
                // When & Then
                session.sessionId shouldBe expectedSessionId
                session.userId shouldBe expectedUserId
                session.getConfiguration() shouldBe expectedPtyConfig
                session.getStatus() shouldBe SessionStatus.CREATED
                session.getTerminatedAt().shouldBeNull()
            }
        }
        
        `when`("启动会话时") {
            then("应该成功启动会话") {
                // Given
                every { mockProcessFactory.createProcess(any(), any()) } returns mockProcess
                every { mockProcess.start() } just Runs
                every { mockProcess.getOutputChannel() } returns kotlinx.coroutines.channels.Channel<String>(capacity = 10)
                
                // When
                session.start()
                
                // Then
                session.getStatus() shouldBe SessionStatus.RUNNING
                verify { mockProcessFactory.createProcess(ptyConfig, sessionId!!) }
                verify { mockProcess.start() }
                
                val events = session.getDomainEvents()
                // 检查至少包含SessionCreatedEvent，可能还有TerminalOutputEvent
                events.any { it is SessionCreatedEvent } shouldBe true
                events.forEach { event ->
                    event.shouldBeInstanceOf<Event>()
                }
            }
        }
        
        `when`("处理输入时") {
            then("应该在会话运行时处理输入") {
                // Given
                every { mockProcessFactory.createProcess(any(), any()) } returns mockProcess
                every { mockProcess.start() } just Runs
                every { mockProcess.writeInput("ls -la") } just Runs
                every { mockProcess.getOutputChannel() } returns kotlinx.coroutines.channels.Channel<String>(capacity = 10)
                
                session.start()
                session.getDomainEvents() // Clear events
                
                // When
                session.handleInput("ls -la")
                
                // Then
                verify { mockProcess.writeInput("ls -la") }
                
                val events = session.getDomainEvents()
                // 检查至少包含TerminalInputProcessedEvent，可能还有其他事件
                events.any { it is TerminalInputProcessedEvent } shouldBe true
                events.forEach { event ->
                    event.shouldBeInstanceOf<Event>()
                }
            }
            
            then("应该在非运行会话上处理输入时抛出异常") {
                // Given
                val command = "ls -la"
                
                // When & Then
                val exception = shouldThrow<IllegalArgumentException> {
                    session.handleInput(command)
                }
                
                exception.message shouldBe "Session must be in RUNNING state"
            }
        }
        
        `when`("调整终端大小时") {
            then("应该在会话运行时调整终端大小") {
                // Given
                every { mockProcessFactory.createProcess(any(), any()) } returns mockProcess
                every { mockProcess.start() } just Runs
                every { mockProcess.resize(any()) } just Runs
                every { mockProcess.getOutputChannel() } returns kotlinx.coroutines.channels.Channel<String>(capacity = 10)
                
                session.start()
                session.getDomainEvents() // Clear events
                
                val newSize = TerminalSize(120, 40)
                
                // When
                session.resize(newSize)
                
                // Then
                verify { mockProcess.resize(newSize) }
                
                val events = session.getDomainEvents()
                // 检查至少包含TerminalResizedEvent，可能还有其他事件
                events.any { it is TerminalResizedEvent } shouldBe true
                val resizeEvent = events.first { it is TerminalResizedEvent } as TerminalResizedEvent
                resizeEvent.columns shouldBe 120
                resizeEvent.rows shouldBe 40
                events.forEach { event ->
                    event.shouldBeInstanceOf<Event>()
                }
            }
        }
        
        `when`("终止会话时") {
            then("应该成功终止会话") {
                // Given
                every { mockProcessFactory.createProcess(any(), any()) } returns mockProcess
                every { mockProcess.start() } just Runs
                every { mockProcess.terminate() } just Runs
                every { mockProcess.getExitCode() } returns 0
                every { mockProcess.getOutputChannel() } returns kotlinx.coroutines.channels.Channel<String>(capacity = 10)
                
                session.start()
                session.getDomainEvents() // Clear events
                
                // When
                session.terminate(TerminationReason.USER_REQUESTED)
                
                // Then
                session.getStatus() shouldBe SessionStatus.TERMINATED
                session.getExitCode() shouldBe 0
                session.getTerminatedAt().shouldNotBeNull()
                
                verify { mockProcess.terminate() }
                
                val events = session.getDomainEvents()
                // 检查至少包含SessionTerminatedEvent，可能还有其他事件
                events.any { it is SessionTerminatedEvent } shouldBe true
                events.forEach { event ->
                    event.shouldBeInstanceOf<Event>()
                }
            }
        }
        
        `when`("检查会话可终止性时") {
            then("应该正确检查会话是否可以终止") {
                // Given
                every { mockProcessFactory.createProcess(any(), any()) } returns mockProcess
                every { mockProcess.start() } just Runs
                every { mockProcess.isAlive() } returns true
                every { mockProcess.terminate() } just Runs
                every { mockProcess.getExitCode() } returns 0
                
                session.start()
                
                // When & Then
                session.canTerminate() shouldBe true
                
                // When session is terminated
                every { mockProcess.isAlive() } returns false
                session.terminate(TerminationReason.NORMAL)
                
                // Then
                session.canTerminate() shouldBe false
            }
        }
        
        `when`("检查会话可接收输入时") {
            then("应该正确检查会话是否可以接收输入") {
                // Given
                every { mockProcessFactory.createProcess(any(), any()) } returns mockProcess
                every { mockProcess.start() } just Runs
                every { mockProcess.isAlive() } returns true
                every { mockProcess.terminate() } just Runs
                every { mockProcess.getExitCode() } returns 0
                
                // When session is created但未启动
                session.canReceiveInput() shouldBe false
                
                // When session is running
                session.start()
                session.canReceiveInput() shouldBe true
                
                // When session is terminated
                every { mockProcess.isAlive() } returns false
                session.terminate(TerminationReason.NORMAL)
                session.canReceiveInput() shouldBe false
            }
        }
        
        `when`("获取会话统计信息时") {
            then("应该获取正确的会话统计信息") {
                // Given
                every { mockProcessFactory.createProcess(any(), any()) } returns mockProcess
                every { mockProcess.start() } just Runs
                every { mockProcess.isAlive() } returns true
                
                session.start()
                
                // When
                val statistics = session.getStatistics()
                
                // Then
                statistics.sessionId shouldBe sessionId
                statistics.userId shouldBe userId
                statistics.status shouldBe SessionStatus.RUNNING
                statistics.createdAt.shouldNotBeNull()
                statistics.terminatedAt.shouldBeNull()
                statistics.exitCode.shouldBeNull()
                statistics.outputSize shouldBe 0
            }
        }
        
        `when`("计算会话持续时间时") {
            then("应该正确计算会话持续时间") {
                // Given
                every { mockProcessFactory.createProcess(any(), any()) } returns mockProcess
                every { mockProcess.start() } just Runs
                every { mockProcess.terminate() } just Runs
                every { mockProcess.getExitCode() } returns 0
                every { mockProcess.getOutputChannel() } returns kotlinx.coroutines.channels.Channel<String>(capacity = 10)
                
                session.start()
                
                // Simulate some time passing
                Thread.sleep(100)
                
                // When
                session.terminate(TerminationReason.NORMAL)
                val duration = session.getDuration()
                
                // Then
                duration.shouldNotBeNull()
                duration!! shouldBeGreaterThanOrEqualTo java.time.Duration.ZERO
            }
        }
    }
})
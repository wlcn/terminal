package org.now.terminal.session.application

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.*
import io.mockk.coVerify
import kotlinx.coroutines.runBlocking
import org.now.terminal.infrastructure.configuration.ConfigurationManager
import org.now.terminal.infrastructure.eventbus.EventBus
import org.now.terminal.session.domain.entities.SessionStatistics
import org.now.terminal.session.domain.entities.TerminalSession
import org.now.terminal.session.domain.repositories.TerminalSessionRepository
import org.now.terminal.session.domain.services.Process
import org.now.terminal.session.domain.services.ProcessFactory
import org.now.terminal.shared.valueobjects.SessionId
import org.now.terminal.shared.valueobjects.UserId
import org.now.terminal.session.domain.valueobjects.PtyConfiguration
import org.now.terminal.session.domain.valueobjects.TerminalCommand
import org.now.terminal.session.domain.valueobjects.TerminalSize
import org.now.terminal.session.domain.valueobjects.TerminationReason
import java.time.Instant

class SessionLifecycleServiceTest : BehaviorSpec({
    
    lateinit var service: SessionLifecycleService
    lateinit var mockRepository: TerminalSessionRepository
    lateinit var mockEventBus: EventBus
    lateinit var mockProcessFactory: ProcessFactory
    lateinit var mockProcess: Process
    var sessionId: SessionId? = null
    var userId: UserId? = null
    lateinit var ptyConfig: PtyConfiguration
    
    beforeTest {
        // 初始化配置管理器（用于测试环境）
        ConfigurationManager.initialize(environment = "test")
        
        mockRepository = mockk()
        mockEventBus = mockk()
        mockProcessFactory = mockk()
        mockProcess = mockk()
        sessionId = SessionId.generate()
        userId = UserId.generate()
        ptyConfig = PtyConfiguration.createDefault(TerminalCommand("/bin/bash"))
        
        service = SessionLifecycleService(mockEventBus, mockRepository, mockProcessFactory)
    }
    
    afterTest {
        // 清理配置管理器
        ConfigurationManager.reset()
    }
    
    given("SessionLifecycleService") {
        `when`("创建会话") {
            then("应该成功创建会话") {
                runBlocking {
                    // Given
                    coEvery { mockProcessFactory.createProcess(any(), any()) } returns mockProcess
                    coEvery { mockProcess.start() } returns Unit
                    coEvery { mockRepository.save(any()) } answers { firstArg() } // 返回保存的session对象
                    coEvery { mockEventBus.publish(any()) } returns Unit
                    coEvery { mockRepository.findByUserId(userId!!) } returns emptyList() // 用户没有活跃会话
                    
                    // When
                    val result = service.createSession(userId!!, ptyConfig)
                    
                    // Then
                    (result is SessionId) shouldBe true
                    coVerify { mockRepository.save(any()) }
                    coVerify { mockProcess.start() }
                    coVerify { mockEventBus.publish(any()) }
                }
            }
        }
    
        `when`("终止会话") {
            then("应该成功终止会话") {
                runBlocking {
                    // Given
                    val mockSession: TerminalSession = mockk()
                    coEvery { mockRepository.findById(sessionId!!) } returns mockSession
                    coEvery { mockSession.canTerminate() } returns true
                    coEvery { mockSession.terminate(any()) } returns Unit
                    coEvery { mockSession.getDomainEvents() } returns listOf(mockk()) // 返回一个mock事件
                    coEvery { mockRepository.save(any()) } returns mockSession
                    coEvery { mockEventBus.publish(any()) } returns Unit
                    
                    // When
                    service.terminateSession(sessionId!!, TerminationReason.USER_REQUESTED)
                    
                    // Then
                    coVerify { mockRepository.findById(sessionId!!) }
                    coVerify { mockSession.canTerminate() }
                    coVerify { mockSession.terminate(TerminationReason.USER_REQUESTED) }
                    coVerify { mockRepository.save(any()) }
                    coVerify { mockEventBus.publish(any()) }
                }
            }

            then("当终止不存在的会话时应该抛出异常") {
                runBlocking {
                    // Given
                    every { mockRepository.findById(sessionId!!) } returns null
                    
                    // When & Then
                    val exception = shouldThrow<IllegalArgumentException> {
                        service.terminateSession(sessionId!!, TerminationReason.USER_REQUESTED)
                    }
                    
                    exception.message shouldBe "Session not found: $sessionId"
                    verify(exactly = 0) { mockRepository.delete(any()) }
                }
            }
        }

        `when`("处理输入") {
            then("应该成功处理输入") {
                runBlocking {
                    // Given
                    val mockSession: TerminalSession = mockk()
                    coEvery { mockRepository.findById(sessionId!!) } returns mockSession
                    coEvery { mockSession.canReceiveInput() } returns true
                    coEvery { mockSession.handleInput(any()) } returns Unit
                    coEvery { mockSession.getDomainEvents() } returns listOf(mockk()) // 返回一个mock事件
                    coEvery { mockRepository.save(any()) } returns mockSession
                    coEvery { mockEventBus.publish(any()) } returns Unit
                    
                    // When
                    service.handleInput(sessionId!!, "test input")
                    
                    // Then
                    coVerify { mockRepository.findById(sessionId!!) }
                    coVerify { mockSession.canReceiveInput() }
                    coVerify { mockSession.handleInput("test input") }
                    coVerify { mockRepository.save(any()) }
                    coVerify { mockEventBus.publish(any()) }
                }
            }
        }

        `when`("调整终端大小") {
            then("应该成功调整终端大小") {
                runBlocking {
                    // Given
                    val mockSession: TerminalSession = mockk()
                    val newSize = TerminalSize(100, 30)
                    coEvery { mockRepository.findById(sessionId!!) } returns mockSession
                    coEvery { mockSession.resize(newSize) } returns Unit
                    coEvery { mockSession.getDomainEvents() } returns listOf(mockk()) // 返回一个mock事件
                    coEvery { mockRepository.save(any()) } returns mockSession
                    coEvery { mockEventBus.publish(any()) } returns Unit

                    // When
                    service.resizeTerminal(sessionId!!, newSize)

                    // Then
                    coVerify { mockRepository.findById(sessionId!!) }
                    coVerify { mockSession.resize(newSize) }
                    coVerify { mockRepository.save(any()) }
                    coVerify { mockEventBus.publish(any()) }
                }
            }
        }

        `when`("列出活跃会话") {
            then("应该成功列出活跃会话") {
                runBlocking {
                    // Given
                    val mockSessions = listOf<TerminalSession>(mockk(), mockk())
                    every { mockRepository.findByUserId(userId!!) } returns mockSessions
                    every { mockSessions[0].isAlive() } returns true
                    every { mockSessions[1].isAlive() } returns true
                    
                    // When
                    val result = service.listActiveSessions(userId!!)
                    
                    // Then
                    result shouldBe mockSessions
                    verify { mockRepository.findByUserId(userId!!) }
                    verify { mockSessions[0].isAlive() }
                    verify { mockSessions[1].isAlive() }
                }
            }
        }

        `when`("读取会话输出") {
            then("应该成功读取会话输出") {
                runBlocking {
                    // Given
                    val mockSession: TerminalSession = mockk()
                    val expectedOutput = "output content"
                    coEvery { mockRepository.findById(sessionId!!) } returns mockSession
                    coEvery { mockSession.readOutput() } returns expectedOutput
                    coEvery { mockSession.getDomainEvents() } returns listOf(mockk()) // 返回一个mock事件
                    coEvery { mockRepository.save(any()) } returns mockSession
                    coEvery { mockEventBus.publish(any()) } returns Unit
                    
                    // When
                    val result = service.readOutput(sessionId!!)
                    
                    // Then
                    result shouldBe expectedOutput
                    coVerify { mockRepository.findById(sessionId!!) }
                    coVerify { mockSession.readOutput() }
                    coVerify { mockRepository.save(any()) }
                    coVerify { mockEventBus.publish(any()) }
                }
            }
        }

        `when`("获取会话统计信息") {
            then("应该成功获取会话统计信息") {
                runBlocking {
                    // Given
                    val mockSession: TerminalSession = mockk()
                    val expectedStats = SessionStatistics(sessionId!!, userId!!, mockk(), Instant.now(), null, null, 0)
                    every { mockRepository.findById(sessionId!!) } returns mockSession
                    every { mockSession.getStatistics() } returns expectedStats
                    
                    // When
                    val result = service.getSessionStatistics(sessionId!!)
                    
                    // Then
                    result shouldBe expectedStats
                    verify { mockRepository.findById(sessionId!!) }
                    verify { mockSession.getStatistics() }
                }
            }
        }

        `when`("终止所有用户会话") {
            then("应该成功终止所有用户会话") {
                runBlocking {
                    // Given
                    val mockSessions = listOf<TerminalSession>(mockk(), mockk())
                    coEvery { mockRepository.findByUserId(userId!!) } returns mockSessions
                    coEvery { mockSessions[0].canTerminate() } returns true
                    coEvery { mockSessions[1].canTerminate() } returns true
                    coEvery { mockSessions[0].terminate(any()) } returns Unit
                    coEvery { mockSessions[1].terminate(any()) } returns Unit
                    coEvery { mockSessions[0].getDomainEvents() } returns listOf(mockk()) // 返回一个mock事件
                    coEvery { mockSessions[1].getDomainEvents() } returns listOf(mockk()) // 返回一个mock事件
                    coEvery { mockSessions[0].sessionId } returns SessionId.generate()
                    coEvery { mockSessions[1].sessionId } returns SessionId.generate()
                    coEvery { mockRepository.delete(any()) } returns Unit
                    coEvery { mockEventBus.publish(any()) } returns Unit
                    
                    // When
                    service.terminateAllUserSessions(userId!!, TerminationReason.USER_REQUESTED)
                    
                    // Then
                    coVerify { mockRepository.findByUserId(userId!!) }
                    coVerify { mockSessions[0].canTerminate() }
                    coVerify { mockSessions[1].canTerminate() }
                    coVerify { mockSessions[0].terminate(TerminationReason.USER_REQUESTED) }
                    coVerify { mockSessions[1].terminate(TerminationReason.USER_REQUESTED) }
                    coVerify(exactly = 2) { mockRepository.delete(any()) }
                    coVerify(exactly = 2) { mockEventBus.publish(any()) }
                }
            }
        }

        `when`("检查会话是否活跃") {
            then("应该正确检查会话是否活跃") {
                runBlocking {
                    // Given
                    val mockSession: TerminalSession = mockk()
                    every { mockRepository.findById(sessionId!!) } returns mockSession
                    every { mockSession.isAlive() } returns true
                    
                    // When
                    val result = service.isSessionActive(sessionId!!)
                    
                    // Then
                    result shouldBe true
                    verify { mockRepository.findById(sessionId!!) }
                    verify { mockSession.isAlive() }
                }
            }
        }

        `when`("获取会话配置") {
            then("应该成功获取会话配置") {
                runBlocking {
                    // Given
                    val mockSession: TerminalSession = mockk()
                    every { mockRepository.findById(sessionId!!) } returns mockSession
                    every { mockSession.getConfiguration() } returns ptyConfig
                    
                    // When
                    val result = service.getSessionConfiguration(sessionId!!)
                    
                    // Then
                    result shouldBe ptyConfig
                    verify { mockRepository.findById(sessionId!!) }
                    verify { mockSession.getConfiguration() }
                }
            }
        }
    }
})
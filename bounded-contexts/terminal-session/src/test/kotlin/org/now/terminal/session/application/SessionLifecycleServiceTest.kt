package org.now.terminal.session.application

import io.mockk.*
import io.mockk.coVerify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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

class SessionLifecycleServiceTest {
    
    private lateinit var service: SessionLifecycleService
    private lateinit var mockRepository: TerminalSessionRepository
    private lateinit var mockEventBus: EventBus
    private lateinit var mockProcessFactory: ProcessFactory
    private lateinit var mockProcess: Process
    private var sessionId: SessionId? = null
    private var userId: UserId? = null
    private lateinit var ptyConfig: PtyConfiguration
    
    @BeforeEach
    fun setUp() {
        mockRepository = mockk()
        mockEventBus = mockk()
        mockProcessFactory = mockk()
        mockProcess = mockk()
        sessionId = SessionId.generate()
        userId = UserId.generate()
        ptyConfig = PtyConfiguration.createDefault(TerminalCommand("/bin/bash"))
        
        service = SessionLifecycleService(mockEventBus, mockRepository, mockProcessFactory)
    }
    
    @Test
    fun `should create session successfully`() {
        // Given
        coEvery { mockProcessFactory.createProcess(any(), any()) } returns mockProcess
        coEvery { mockProcess.start() } returns Unit
        coEvery { mockRepository.save(any()) } answers { firstArg() } // 返回保存的session对象
        coEvery { mockEventBus.publish(any()) } returns Unit
        
        // When
        val result = service.createSession(userId!!, ptyConfig)
        
        // Then
        assertNotNull(result)
        assertTrue(result is SessionId)
        coVerify { mockRepository.save(any()) }
        coVerify { mockProcess.start() }
        runBlocking { coVerify { mockEventBus.publish(any()) } }
    }
    
    @Test
    fun `should terminate session successfully`() {
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
    
    @Test
    fun `should throw exception when terminating non-existent session`() {
        // Given
        every { mockRepository.findById(sessionId!!) } returns null
        
        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            service.terminateSession(sessionId!!, TerminationReason.USER_REQUESTED)
        }
        
        assertEquals("Session not found: $sessionId", exception.message)
        verify(exactly = 0) { mockRepository.delete(any()) }
    }
    
    @Test
    fun `should handle input successfully`() {
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
    
    @Test
    fun `should resize terminal successfully`() {
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
    
    @Test
    fun `should list active sessions`() {
        // Given
        val mockSessions = listOf<TerminalSession>(mockk(), mockk())
        every { mockRepository.findByUserId(userId!!) } returns mockSessions
        every { mockSessions[0].isAlive() } returns true
        every { mockSessions[1].isAlive() } returns true
        
        // When
        val result = service.listActiveSessions(userId!!)
        
        // Then
        assertEquals(mockSessions, result)
        verify { mockRepository.findByUserId(userId!!) }
        verify { mockSessions[0].isAlive() }
        verify { mockSessions[1].isAlive() }
    }
    
    @Test
    fun `should read output from session`() {
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
        assertEquals(expectedOutput, result)
        coVerify { mockRepository.findById(sessionId!!) }
        coVerify { mockSession.readOutput() }
        coVerify { mockRepository.save(any()) }
        coVerify { mockEventBus.publish(any()) }
    }
    
    @Test
    fun `should get session statistics`() {
        // Given
        val mockSession: TerminalSession = mockk()
        val expectedStats = SessionStatistics(sessionId!!, userId!!, mockk(), Instant.now(), null, null, 0)
        every { mockRepository.findById(sessionId!!) } returns mockSession
        every { mockSession.getStatistics() } returns expectedStats
        
        // When
        val result = service.getSessionStatistics(sessionId!!)
        
        // Then
        assertEquals(expectedStats, result)
        verify { mockRepository.findById(sessionId!!) }
        verify { mockSession.getStatistics() }
    }
    
    @Test
    fun `should terminate all user sessions`() {
        // Given
        val mockSessions = listOf<TerminalSession>(mockk(), mockk())
        coEvery { mockRepository.findByUserId(userId!!) } returns mockSessions
        coEvery { mockSessions[0].canTerminate() } returns true
        coEvery { mockSessions[1].canTerminate() } returns true
        coEvery { mockSessions[0].terminate(any()) } returns Unit
        coEvery { mockSessions[1].terminate(any()) } returns Unit
        coEvery { mockSessions[0].getDomainEvents() } returns listOf(mockk()) // 返回一个mock事件
        coEvery { mockSessions[1].getDomainEvents() } returns listOf(mockk()) // 返回一个mock事件
        coEvery { mockRepository.save(any()) } returns mockk()
        coEvery { mockEventBus.publish(any()) } returns Unit
        
        // When
        service.terminateAllUserSessions(userId!!, TerminationReason.USER_REQUESTED)
        
        // Then
        coVerify { mockRepository.findByUserId(userId!!) }
        coVerify { mockSessions[0].canTerminate() }
        coVerify { mockSessions[1].canTerminate() }
        coVerify { mockSessions[0].terminate(TerminationReason.USER_REQUESTED) }
        coVerify { mockSessions[1].terminate(TerminationReason.USER_REQUESTED) }
        coVerify(exactly = 2) { mockRepository.save(any()) }
        coVerify(exactly = 2) { mockEventBus.publish(any()) }
    }
    
    @Test
    fun `should check if session is active`() {
        // Given
        val mockSession: TerminalSession = mockk()
        every { mockRepository.findById(sessionId!!) } returns mockSession
        every { mockSession.isAlive() } returns true
        
        // When
        val result = service.isSessionActive(sessionId!!)
        
        // Then
        assertTrue(result)
        verify { mockRepository.findById(sessionId!!) }
        verify { mockSession.isAlive() }
    }
    
    @Test
    fun `should get session configuration`() {
        // Given
        val mockSession: TerminalSession = mockk()
        every { mockRepository.findById(sessionId!!) } returns mockSession
        every { mockSession.getConfiguration() } returns ptyConfig
        
        // When
        val result = service.getSessionConfiguration(sessionId!!)
        
        // Then
        assertEquals(ptyConfig, result)
        verify { mockRepository.findById(sessionId!!) }
        verify { mockSession.getConfiguration() }
    }
}
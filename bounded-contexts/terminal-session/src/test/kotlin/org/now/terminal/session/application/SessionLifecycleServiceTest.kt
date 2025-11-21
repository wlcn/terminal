package org.now.terminal.session.application

import io.mockk.*
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
import org.now.terminal.session.domain.valueobjects.TerminalSize
import org.now.terminal.session.domain.valueobjects.TerminationReason
import java.time.Instant

class SessionLifecycleServiceTest {
    
    private lateinit var service: SessionLifecycleService
    private lateinit var mockRepository: TerminalSessionRepository
    private lateinit var mockEventBus: EventBus
    private lateinit var mockProcessFactory: ProcessFactory
    private lateinit var mockProcess: Process
    private lateinit var sessionId: SessionId
    private lateinit var userId: UserId
    private lateinit var ptyConfig: PtyConfiguration
    
    @BeforeEach
    fun setUp() {
        mockRepository = mockk()
        mockEventBus = mockk()
        mockProcessFactory = mockk()
        mockProcess = mockk()
        sessionId = SessionId.generate()
        userId = UserId.generate()
        ptyConfig = PtyConfiguration.createDefault()
        
        service = SessionLifecycleService(mockRepository, mockEventBus, mockProcessFactory)
    }
    
    @Test
    fun `should create session successfully`() {
        // Given
        val mockSession: TerminalSession = mockk()
        every { mockProcessFactory.createProcess(any()) } returns mockProcess
        every { mockSession.start() } just Runs
        every { mockSession.getDomainEvents() } returns emptyList()
        every { mockRepository.save(any()) } returns mockSession
        
        // When
        val result = service.createSession(userId, ptyConfig)
        
        // Then
        assertEquals(mockSession, result)
        verify { mockRepository.save(any()) }
        verify { mockSession.start() }
        verify { mockEventBus.publishAll(any()) }
    }
    
    @Test
    fun `should terminate session successfully`() {
        // Given
        val mockSession: TerminalSession = mockk()
        every { mockRepository.findById(sessionId) } returns mockSession
        every { mockSession.terminate(any()) } just Runs
        every { mockSession.getDomainEvents() } returns emptyList()
        every { mockRepository.delete(sessionId) } just Runs
        
        // When
        service.terminateSession(sessionId)
        
        // Then
        verify { mockRepository.findById(sessionId) }
        verify { mockSession.terminate(TerminationReason.USER_REQUESTED) }
        verify { mockRepository.delete(sessionId) }
        verify { mockEventBus.publishAll(any()) }
    }
    
    @Test
    fun `should throw exception when terminating non-existent session`() {
        // Given
        every { mockRepository.findById(sessionId) } returns null
        
        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            service.terminateSession(sessionId)
        }
        
        assertEquals("Session not found: $sessionId", exception.message)
        verify(exactly = 0) { mockRepository.delete(any()) }
    }
    
    @Test
    fun `should handle input successfully`() {
        // Given
        val mockSession: TerminalSession = mockk()
        every { mockRepository.findById(sessionId) } returns mockSession
        every { mockSession.handleInput(any()) } just Runs
        every { mockSession.getDomainEvents() } returns emptyList()
        
        // When
        service.handleInput(sessionId, "ls -la")
        
        // Then
        verify { mockRepository.findById(sessionId) }
        verify { mockSession.handleInput(any()) }
        verify { mockEventBus.publishAll(any()) }
    }
    
    @Test
    fun `should resize terminal successfully`() {
        // Given
        val mockSession: TerminalSession = mockk()
        val newSize = TerminalSize(120, 40)
        every { mockRepository.findById(sessionId) } returns mockSession
        every { mockSession.resize(newSize) } just Runs
        every { mockSession.getDomainEvents() } returns emptyList()
        
        // When
        service.resizeTerminal(sessionId, newSize)
        
        // Then
        verify { mockRepository.findById(sessionId) }
        verify { mockSession.resize(newSize) }
        verify { mockEventBus.publishAll(any()) }
    }
    
    @Test
    fun `should list active sessions`() {
        // Given
        val mockSessions = listOf<TerminalSession>(mockk(), mockk())
        every { mockRepository.findAllActive() } returns mockSessions
        
        // When
        val result = service.listActiveSessions()
        
        // Then
        assertEquals(mockSessions, result)
        verify { mockRepository.findAllActive() }
    }
    
    @Test
    fun `should read output from session`() {
        // Given
        val mockSession: TerminalSession = mockk()
        val expectedOutput = "output content"
        every { mockRepository.findById(sessionId) } returns mockSession
        every { mockSession.readOutput() } returns expectedOutput
        
        // When
        val result = service.readOutput(sessionId)
        
        // Then
        assertEquals(expectedOutput, result)
        verify { mockRepository.findById(sessionId) }
        verify { mockSession.readOutput() }
    }
    
    @Test
    fun `should get session statistics`() {
        // Given
        val mockSession: TerminalSession = mockk()
        val expectedStats = SessionStatistics(sessionId, userId, mockk(), Instant.now(), null, 0, true)
        every { mockRepository.findById(sessionId) } returns mockSession
        every { mockSession.getStatistics() } returns expectedStats
        
        // When
        val result = service.getSessionStatistics(sessionId)
        
        // Then
        assertEquals(expectedStats, result)
        verify { mockRepository.findById(sessionId) }
        verify { mockSession.getStatistics() }
    }
    
    @Test
    fun `should terminate all user sessions`() {
        // Given
        val mockSessions = listOf<TerminalSession>(mockk(), mockk())
        every { mockRepository.findByUserId(userId) } returns mockSessions
        every { mockSessions[0].terminate(any()) } just Runs
        every { mockSessions[1].terminate(any()) } just Runs
        every { mockSessions[0].getDomainEvents() } returns emptyList()
        every { mockSessions[1].getDomainEvents() } returns emptyList()
        every { mockRepository.delete(any()) } just Runs
        
        // When
        service.terminateAllUserSessions(userId)
        
        // Then
        verify { mockRepository.findByUserId(userId) }
        verify { mockSessions[0].terminate(TerminationReason.USER_REQUESTED) }
        verify { mockSessions[1].terminate(TerminationReason.USER_REQUESTED) }
        verify(exactly = 2) { mockRepository.delete(any()) }
    }
    
    @Test
    fun `should check if session is active`() {
        // Given
        val mockSession: TerminalSession = mockk()
        every { mockRepository.findById(sessionId) } returns mockSession
        every { mockSession.getStatus() } returns mockk()
        every { mockSession.getStatus().isActive } returns true
        
        // When
        val result = service.isSessionActive(sessionId)
        
        // Then
        assertTrue(result)
        verify { mockRepository.findById(sessionId) }
        verify { mockSession.getStatus() }
    }
    
    @Test
    fun `should get session configuration`() {
        // Given
        val mockSession: TerminalSession = mockk()
        every { mockRepository.findById(sessionId) } returns mockSession
        every { mockSession.getConfiguration() } returns ptyConfig
        
        // When
        val result = service.getSessionConfiguration(sessionId)
        
        // Then
        assertEquals(ptyConfig, result)
        verify { mockRepository.findById(sessionId) }
        verify { mockSession.getConfiguration() }
    }
}
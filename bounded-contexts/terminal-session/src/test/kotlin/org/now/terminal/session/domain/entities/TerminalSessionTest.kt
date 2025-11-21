package org.now.terminal.session.domain.entities

import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.now.terminal.infrastructure.eventbus.EventBus
import org.now.terminal.shared.valueobjects.SessionId
import org.now.terminal.shared.valueobjects.UserId
import org.now.terminal.session.domain.services.Process
import org.now.terminal.session.domain.services.ProcessFactory
import org.now.terminal.session.domain.valueobjects.PtyConfiguration
import org.now.terminal.session.domain.valueobjects.TerminalCommand
import org.now.terminal.session.domain.valueobjects.TerminalSize
import org.now.terminal.session.domain.valueobjects.TerminationReason
import java.time.Instant

class TerminalSessionTest {
    
    private lateinit var session: TerminalSession
    private lateinit var mockEventBus: EventBus
    private lateinit var mockProcessFactory: ProcessFactory
    private lateinit var mockProcess: Process
    private lateinit var sessionId: SessionId
    private lateinit var userId: UserId
    private lateinit var ptyConfig: PtyConfiguration
    
    @BeforeEach
    fun setUp() {
        mockEventBus = mockk()
        mockProcessFactory = mockk()
        mockProcess = mockk()
        sessionId = SessionId.generate()
        userId = UserId.generate()
        ptyConfig = PtyConfiguration.createDefault()
        
        session = TerminalSession(sessionId, userId, ptyConfig, mockEventBus, mockProcessFactory)
    }
    
    @Test
    fun `should create session with correct initial state`() {
        // Then
        assertEquals(sessionId, session.sessionId)
        assertEquals(userId, session.userId)
        assertEquals(ptyConfig, session.getConfiguration())
        assertEquals(SessionStatus.CREATED, session.getStatus())
        assertNotNull(session.getCreatedAt())
        assertNull(session.getTerminatedAt())
        assertNull(session.getExitCode())
        assertTrue(session.getDomainEvents().isEmpty())
    }
    
    @Test
    fun `should start session successfully`() {
        // Given
        every { mockProcessFactory.createProcess(any()) } returns mockProcess
        every { mockProcess.start() } just Runs
        
        // When
        session.start()
        
        // Then
        assertEquals(SessionStatus.RUNNING, session.getStatus())
        verify { mockProcessFactory.createProcess(ptyConfig) }
        verify { mockProcess.start() }
        
        val events = session.getDomainEvents()
        assertEquals(1, events.size)
        assertTrue(events[0] is SessionCreatedEvent)
    }
    
    @Test
    fun `should handle input when session is running`() {
        // Given
        every { mockProcessFactory.createProcess(any()) } returns mockProcess
        every { mockProcess.start() } just Runs
        every { mockProcess.writeInput("ls -la") } just Runs
        
        session.start()
        session.getDomainEvents() // Clear events
        
        // When
        session.handleInput(TerminalCommand.fromString("ls -la"))
        
        // Then
        verify { mockProcess.writeInput("ls -la") }
        
        val events = session.getDomainEvents()
        assertEquals(1, events.size)
        assertTrue(events[0] is TerminalInputProcessedEvent)
    }
    
    @Test
    fun `should throw exception when handling input on non-running session`() {
        // Given
        val command = TerminalCommand.fromString("ls -la")
        
        // When & Then
        val exception = assertThrows<IllegalStateException> {
            session.handleInput(command)
        }
        
        assertEquals("Session is not running", exception.message)
    }
    
    @Test
    fun `should resize terminal when session is running`() {
        // Given
        every { mockProcessFactory.createProcess(any()) } returns mockProcess
        every { mockProcess.start() } just Runs
        every { mockProcess.resize(any()) } just Runs
        
        session.start()
        session.getDomainEvents() // Clear events
        
        val newSize = TerminalSize(120, 40)
        
        // When
        session.resize(newSize)
        
        // Then
        verify { mockProcess.resize(newSize) }
        assertEquals(newSize, session.getConfiguration().size)
        
        val events = session.getDomainEvents()
        assertEquals(1, events.size)
        assertTrue(events[0] is TerminalResizedEvent)
    }
    
    @Test
    fun `should terminate session successfully`() {
        // Given
        every { mockProcessFactory.createProcess(any()) } returns mockProcess
        every { mockProcess.start() } just Runs
        every { mockProcess.terminate() } just Runs
        every { mockProcess.getExitCode() } returns 0
        
        session.start()
        session.getDomainEvents() // Clear events
        
        // When
        session.terminate(TerminationReason.USER_REQUESTED)
        
        // Then
        assertEquals(SessionStatus.TERMINATED, session.getStatus())
        assertEquals(TerminationReason.USER_REQUESTED, session.getTerminationReason())
        assertEquals(0, session.getExitCode())
        assertNotNull(session.getTerminatedAt())
        
        verify { mockProcess.terminate() }
        
        val events = session.getDomainEvents()
        assertEquals(1, events.size)
        assertTrue(events[0] is SessionTerminatedEvent)
    }
    
    @Test
    fun `should check if session can terminate`() {
        // Given
        every { mockProcessFactory.createProcess(any()) } returns mockProcess
        every { mockProcess.start() } just Runs
        every { mockProcess.isAlive() } returns true
        
        session.start()
        
        // When & Then
        assertTrue(session.canTerminate())
        
        // When session is terminated
        every { mockProcess.isAlive() } returns false
        session.terminate(TerminationReason.NORMAL)
        
        // Then
        assertFalse(session.canTerminate())
    }
    
    @Test
    fun `should check if session can receive input`() {
        // Given
        every { mockProcessFactory.createProcess(any()) } returns mockProcess
        every { mockProcess.start() } just Runs
        every { mockProcess.isAlive() } returns true
        
        // When session is created but not started
        assertFalse(session.canReceiveInput())
        
        // When session is running
        session.start()
        assertTrue(session.canReceiveInput())
        
        // When session is terminated
        every { mockProcess.isAlive() } returns false
        session.terminate(TerminationReason.NORMAL)
        assertFalse(session.canReceiveInput())
    }
    
    @Test
    fun `should get correct session statistics`() {
        // Given
        every { mockProcessFactory.createProcess(any()) } returns mockProcess
        every { mockProcess.start() } just Runs
        every { mockProcess.isAlive() } returns true
        
        session.start()
        
        // When
        val statistics = session.getStatistics()
        
        // Then
        assertEquals(sessionId, statistics.sessionId)
        assertEquals(userId, statistics.userId)
        assertEquals(SessionStatus.RUNNING, statistics.status)
        assertNotNull(statistics.createdAt)
        assertNull(statistics.terminatedAt)
        assertNull(statistics.exitCode)
        assertEquals(0, statistics.outputSize)
    }
    
    @Test
    fun `should calculate duration correctly`() {
        // Given
        every { mockProcessFactory.createProcess(any()) } returns mockProcess
        every { mockProcess.start() } just Runs
        every { mockProcess.terminate() } just Runs
        every { mockProcess.getExitCode() } returns 0
        
        session.start()
        
        // Simulate some time passing
        Thread.sleep(100)
        
        // When
        session.terminate(TerminationReason.NORMAL)
        val duration = session.getDuration()
        
        // Then
        assertTrue(duration >= 0)
    }
}
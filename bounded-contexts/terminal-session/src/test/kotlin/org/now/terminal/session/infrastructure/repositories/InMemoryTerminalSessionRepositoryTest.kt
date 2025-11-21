package org.now.terminal.session.infrastructure.repositories

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.now.terminal.session.domain.entities.TerminalSession
import org.now.terminal.shared.valueobjects.SessionId
import org.now.terminal.shared.valueobjects.UserId
import org.now.terminal.session.domain.valueobjects.PtyConfiguration

class InMemoryTerminalSessionRepositoryTest {
    
    private lateinit var repository: InMemoryTerminalSessionRepository
    private var sessionId: SessionId = SessionId.generate()
    private var userId: UserId = UserId.generate()
    private lateinit var mockSession: TerminalSession
    
    @BeforeEach
    fun setUp() {
        repository = InMemoryTerminalSessionRepository()
        
        mockSession = mockk()
        every { mockSession.sessionId } returns sessionId
        every { mockSession.userId } returns userId
        every { mockSession.isAlive() } returns true
    }
    
    @Test
    fun `should save session successfully`() {
        // When
        val result = repository.save(mockSession)
        
        // Then
        assertEquals(mockSession, result)
        assertNotNull(repository.findById(sessionId))
    }
    
    @Test
    fun `should find session by id`() {
        // Given
        repository.save(mockSession)
        
        // When
        val result = repository.findById(sessionId)
        
        // Then
        assertEquals(mockSession, result)
    }
    
    @Test
    fun `should return null when session not found by id`() {
        // When
        val result = repository.findById(sessionId)
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun `should find sessions by user id`() {
        // Given
        repository.save(mockSession)
        
        // When
        val result = repository.findByUserId(userId)
        
        // Then
        assertEquals(1, result.size)
        assertEquals(mockSession, result[0])
    }
    
    @Test
    fun `should return empty list when no sessions found for user`() {
        // When
        val result = repository.findByUserId(userId)
        
        // Then
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun `should delete session successfully`() {
        // Given
        repository.save(mockSession)
        
        // When
        repository.delete(sessionId)
        
        // Then
        assertNull(repository.findById(sessionId))
    }
    
    @Test
    fun `should not throw exception when deleting non-existent session`() {
        // When & Then
        assertDoesNotThrow {
            repository.delete(sessionId)
        }
    }
    
    @Test
    fun `should find all active sessions`() {
        // Given
        val activeSession1 = mockk<TerminalSession>()
        val activeSession2 = mockk<TerminalSession>()
        val inactiveSession = mockk<TerminalSession>()
        
        every { activeSession1.sessionId } returns SessionId.generate()
        every { activeSession1.userId } returns UserId.generate()
        every { activeSession1.isAlive() } returns true
        
        every { activeSession2.sessionId } returns SessionId.generate()
        every { activeSession2.userId } returns UserId.generate()
        every { activeSession2.isAlive() } returns true
        
        every { inactiveSession.sessionId } returns SessionId.generate()
        every { inactiveSession.userId } returns UserId.generate()
        every { inactiveSession.isAlive() } returns false
        
        repository.save(activeSession1)
        repository.save(activeSession2)
        repository.save(inactiveSession)
        
        // When
        val result = repository.findAllActive()
        
        // Then
        assertEquals(2, result.size)
        assertTrue(result.contains(activeSession1))
        assertTrue(result.contains(activeSession2))
        assertFalse(result.contains(inactiveSession))
    }
    
    @Test
    fun `should return empty list when no active sessions`() {
        // Given
        val inactiveSession = mockk<TerminalSession>()
        every { inactiveSession.sessionId } returns SessionId.generate()
        every { inactiveSession.userId } returns UserId.generate()
        every { inactiveSession.isAlive() } returns false
        
        repository.save(inactiveSession)
        
        // When
        val result = repository.findAllActive()
        
        // Then
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun `should handle multiple operations correctly`() {
        // Given
        val session1 = mockk<TerminalSession>()
        val session2 = mockk<TerminalSession>()
        
        every { session1.sessionId } returns SessionId.generate()
        every { session1.userId } returns UserId.generate()
        every { session1.isAlive() } returns true
        
        every { session2.sessionId } returns SessionId.generate()
        every { session2.userId } returns UserId.generate()
        every { session2.isAlive() } returns true
        
        // When
        repository.save(session1)
        repository.save(session2)
        
        // Then
        assertEquals(2, repository.findAllActive().size)
        
        // When
        repository.delete(session1.sessionId)
        
        // Then
        assertEquals(1, repository.findAllActive().size)
        assertEquals(session2, repository.findAllActive()[0])
    }
}
package org.now.terminal.session.infrastructure.repositories

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.mockk.every
import io.mockk.mockk
import org.now.terminal.session.domain.entities.TerminalSession
import org.now.terminal.shared.valueobjects.SessionId
import org.now.terminal.shared.valueobjects.UserId

class InMemoryTerminalSessionRepositoryTest : BehaviorSpec({
    
    given("InMemoryTerminalSessionRepository") {
        
        `when`("保存会话") {
            val repository = InMemoryTerminalSessionRepository()
            val sessionId = SessionId.generate()
            val userId = UserId.generate()
            val mockSession = mockk<TerminalSession>()
            
            every { mockSession.sessionId } returns sessionId
            every { mockSession.userId } returns userId
            every { mockSession.isAlive() } returns true
            
            val result = repository.save(mockSession)
            
            then("应该成功保存并可以找到") {
                result shouldBe mockSession
                repository.findById(sessionId).shouldNotBeNull()
            }
        }
        
        `when`("通过ID查找会话") {
            val repository = InMemoryTerminalSessionRepository()
            val sessionId = SessionId.generate()
            val userId = UserId.generate()
            val mockSession = mockk<TerminalSession>()
            
            every { mockSession.sessionId } returns sessionId
            every { mockSession.userId } returns userId
            every { mockSession.isAlive() } returns true
            
            repository.save(mockSession)
            val result = repository.findById(sessionId)
            
            then("应该返回正确的会话") {
                result shouldBe mockSession
            }
        }
        
        `when`("查找不存在的会话") {
            val repository = InMemoryTerminalSessionRepository()
            val sessionId = SessionId.generate()
            
            val result = repository.findById(sessionId)
            
            then("应该返回null") {
                result.shouldBeNull()
            }
        }
        
        `when`("通过用户ID查找会话") {
            val repository = InMemoryTerminalSessionRepository()
            val sessionId = SessionId.generate()
            val userId = UserId.generate()
            val mockSession = mockk<TerminalSession>()
            
            every { mockSession.sessionId } returns sessionId
            every { mockSession.userId } returns userId
            every { mockSession.isAlive() } returns true
            
            repository.save(mockSession)
            val result = repository.findByUserId(userId)
            
            then("应该返回用户的会话列表") {
                result.size shouldBe 1
                result[0] shouldBe mockSession
            }
        }
        
        `when`("查找用户没有的会话") {
            val repository = InMemoryTerminalSessionRepository()
            val userId = UserId.generate()
            
            val result = repository.findByUserId(userId)
            
            then("应该返回空列表") {
                result.isEmpty().shouldBeTrue()
            }
        }
        
        `when`("删除会话") {
            val repository = InMemoryTerminalSessionRepository()
            val sessionId = SessionId.generate()
            val userId = UserId.generate()
            val mockSession = mockk<TerminalSession>()
            
            every { mockSession.sessionId } returns sessionId
            every { mockSession.userId } returns userId
            every { mockSession.isAlive() } returns true
            
            repository.save(mockSession)
            repository.delete(sessionId)
            
            then("应该成功删除会话") {
                repository.findById(sessionId).shouldBeNull()
            }
        }
        
        `when`("删除不存在的会话") {
            val repository = InMemoryTerminalSessionRepository()
            val sessionId = SessionId.generate()
            
            then("不应该抛出异常") {
                shouldNotThrowAny {
                    repository.delete(sessionId)
                }
            }
        }
    
        `when`("查找所有活跃会话") {
            val repository = InMemoryTerminalSessionRepository()
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
            
            val result = repository.findAllActive()
            
            then("应该只返回活跃会话") {
                result.size shouldBe 2
                result shouldContain activeSession1
                result shouldContain activeSession2
                result shouldNotContain inactiveSession
            }
        }
        
        `when`("查找没有活跃会话的情况") {
            val repository = InMemoryTerminalSessionRepository()
            val inactiveSession = mockk<TerminalSession>()
            every { inactiveSession.sessionId } returns SessionId.generate()
            every { inactiveSession.userId } returns UserId.generate()
            every { inactiveSession.isAlive() } returns false
            
            repository.save(inactiveSession)
            
            val result = repository.findAllActive()
            
            then("应该返回空列表") {
                result.isEmpty().shouldBeTrue()
            }
        }
        
        `when`("处理多个操作") {
            val repository = InMemoryTerminalSessionRepository()
            val session1 = mockk<TerminalSession>()
            val session2 = mockk<TerminalSession>()
            
            every { session1.sessionId } returns SessionId.generate()
            every { session1.userId } returns UserId.generate()
            every { session1.isAlive() } returns true
            
            every { session2.sessionId } returns SessionId.generate()
            every { session2.userId } returns UserId.generate()
            every { session2.isAlive() } returns true
            
            repository.save(session1)
            repository.save(session2)
            
            then("应该包含两个活跃会话") {
                repository.findAllActive().size shouldBe 2
            }
        }
        
        `when`("删除一个会话后") {
            val repository = InMemoryTerminalSessionRepository()
            val session1 = mockk<TerminalSession>()
            val session2 = mockk<TerminalSession>()
            
            every { session1.sessionId } returns SessionId.generate()
            every { session1.userId } returns UserId.generate()
            every { session1.isAlive() } returns true
            
            every { session2.sessionId } returns SessionId.generate()
            every { session2.userId } returns UserId.generate()
            every { session2.isAlive() } returns true
            
            repository.save(session1)
            repository.save(session2)
            repository.delete(session1.sessionId)
            
            then("应该只剩下一个会话") {
                repository.findAllActive().size shouldBe 1
                repository.findAllActive()[0] shouldBe session2
            }
        }
    }
})
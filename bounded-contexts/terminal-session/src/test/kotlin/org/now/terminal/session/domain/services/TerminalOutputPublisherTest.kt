package org.now.terminal.session.domain.services

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import kotlinx.coroutines.runBlocking
import org.now.terminal.shared.valueobjects.SessionId

class TerminalOutputPublisherTest : BehaviorSpec({
    
    given("TerminalOutputPublisher接口测试") {
        
        `when`("使用测试实现") {
            val testPublisher = object : TerminalOutputPublisher {
                private val connectedSessions = mutableSetOf<SessionId>()
                
                override suspend fun publishOutput(sessionId: SessionId, output: String) {
                    // 模拟发布输出
                    println("Publishing output to session $sessionId: $output")
                }
                
                override suspend fun getActiveSessionCount(): Int {
                    return connectedSessions.size
                }
                
                override suspend fun isSessionConnected(sessionId: SessionId): Boolean {
                    return connectedSessions.contains(sessionId)
                }
                
                override suspend fun closeAllSessions() {
                    connectedSessions.clear()
                }
                
                // 测试辅助方法
                fun registerSession(sessionId: SessionId) {
                    connectedSessions.add(sessionId)
                }
                
                fun unregisterSession(sessionId: SessionId) {
                    connectedSessions.remove(sessionId)
                }
            }
            
            then("应该正确发布输出") {
                runBlocking {
                    val sessionId = SessionId.generate()
                    
                    // 注册会话
                    testPublisher.registerSession(sessionId)
                    
                    // 发布输出
                    testPublisher.publishOutput(sessionId, "Hello World")
                    
                    // 验证会话状态
                    testPublisher.isSessionConnected(sessionId).shouldBeTrue()
                    testPublisher.getActiveSessionCount() shouldBe 1
                }
            }
            
            then("应该正确管理会话连接状态") {
                runBlocking {
                    val sessionId1 = SessionId.generate()
                    val sessionId2 = SessionId.generate()
                    
                    // 注册两个会话
                    testPublisher.registerSession(sessionId1)
                    testPublisher.registerSession(sessionId2)
                    
                    // 验证活跃会话数量
                    testPublisher.getActiveSessionCount() shouldBe 2
                    testPublisher.isSessionConnected(sessionId1).shouldBeTrue()
                    testPublisher.isSessionConnected(sessionId2).shouldBeTrue()
                    
                    // 注销一个会话
                    testPublisher.unregisterSession(sessionId1)
                    
                    // 验证会话状态
                    testPublisher.getActiveSessionCount() shouldBe 1
                    testPublisher.isSessionConnected(sessionId1).shouldBeFalse()
                    testPublisher.isSessionConnected(sessionId2).shouldBeTrue()
                }
            }
            
            then("应该正确关闭所有会话") {
                runBlocking {
                    val sessionId1 = SessionId.generate()
                    val sessionId2 = SessionId.generate()
                    
                    // 注册两个会话
                    testPublisher.registerSession(sessionId1)
                    testPublisher.registerSession(sessionId2)
                    
                    // 验证活跃会话数量
                    testPublisher.getActiveSessionCount() shouldBe 2
                    
                    // 关闭所有会话
                    testPublisher.closeAllSessions()
                    
                    // 验证所有会话已关闭
                    testPublisher.getActiveSessionCount() shouldBe 0
                    testPublisher.isSessionConnected(sessionId1).shouldBeFalse()
                    testPublisher.isSessionConnected(sessionId2).shouldBeFalse()
                }
            }
            
            then("应该正确处理未注册的会话") {
                runBlocking {
                    val unregisteredSessionId = SessionId.generate()
                    
                    // 验证未注册的会话状态
                    testPublisher.isSessionConnected(unregisteredSessionId).shouldBeFalse()
                    
                    // 发布输出到未注册的会话（不应该抛出异常）
                    testPublisher.publishOutput(unregisteredSessionId, "Test output")
                    
                    // 验证活跃会话数量不变
                    testPublisher.getActiveSessionCount() shouldBe 0
                }
            }
        }
    }
})
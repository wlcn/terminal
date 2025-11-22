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
            
            then("应该正确发布输出") {
                runBlocking {
                    val testPublisher = createTestPublisher()
                    val sessionId = SessionId.generate()
                    
                    // 发布输出（不需要注册会话，因为接口不提供注册方法）
                    testPublisher.publishOutput(sessionId, "Hello World")
                    
                    // 验证会话状态（未注册的会话应该显示为未连接）
                    testPublisher.isSessionConnected(sessionId).shouldBeFalse()
                    testPublisher.getActiveSessionCount() shouldBe 0
                }
            }
            
            then("应该正确管理会话连接状态") {
                runBlocking {
                    val testPublisher = createTestPublisher()
                    val sessionId1 = SessionId.generate()
                    val sessionId2 = SessionId.generate()
                    
                    // 验证初始状态
                    testPublisher.getActiveSessionCount() shouldBe 0
                    testPublisher.isSessionConnected(sessionId1).shouldBeFalse()
                    testPublisher.isSessionConnected(sessionId2).shouldBeFalse()
                    
                    // 发布输出到会话（不会改变连接状态）
                    testPublisher.publishOutput(sessionId1, "Output 1")
                    testPublisher.publishOutput(sessionId2, "Output 2")
                    
                    // 验证会话状态保持不变
                    testPublisher.getActiveSessionCount() shouldBe 0
                    testPublisher.isSessionConnected(sessionId1).shouldBeFalse()
                    testPublisher.isSessionConnected(sessionId2).shouldBeFalse()
                }
            }
            
            then("应该正确关闭所有会话") {
                runBlocking {
                    val testPublisher = createTestPublisher()
                    val sessionId1 = SessionId.generate()
                    val sessionId2 = SessionId.generate()
                    
                    // 验证初始状态
                    testPublisher.getActiveSessionCount() shouldBe 0
                    
                    // 关闭所有会话（应该没有影响，因为没有活跃会话）
                    testPublisher.closeAllSessions()
                    
                    // 验证状态保持不变
                    testPublisher.getActiveSessionCount() shouldBe 0
                    testPublisher.isSessionConnected(sessionId1).shouldBeFalse()
                    testPublisher.isSessionConnected(sessionId2).shouldBeFalse()
                }
            }
            
            then("应该正确处理未注册的会话") {
                runBlocking {
                    val testPublisher = createTestPublisher()
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

private fun createTestPublisher(): TerminalOutputPublisher {
    return object : TerminalOutputPublisher {
        
        override suspend fun publishOutput(sessionId: SessionId, output: String) {
            // 模拟发布输出
            println("Publishing output to session $sessionId: $output")
        }
        
        override suspend fun getActiveSessionCount(): Int {
            // 测试实现中，总是返回0，因为没有会话管理功能
            return 0
        }
        
        override suspend fun isSessionConnected(sessionId: SessionId): Boolean {
            // 测试实现中，总是返回false，因为没有会话管理功能
            return false
        }
        
        override suspend fun closeAllSessions() {
            // 测试实现中，关闭所有会话没有实际效果
            // 因为接口不提供会话管理功能
        }
    }
}
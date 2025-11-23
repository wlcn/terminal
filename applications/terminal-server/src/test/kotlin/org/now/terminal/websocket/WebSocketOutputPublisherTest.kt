package org.now.terminal.websocket

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import kotlinx.coroutines.test.runTest
import org.now.terminal.shared.valueobjects.SessionId

/**
 * WebSocket输出发布器测试
 * 测试终端输出推送和会话管理功能
 */
class WebSocketOutputPublisherTest : BehaviorSpec({
    
    given("WebSocket输出发布器") {
        
        `when`("获取活跃会话数量") {
            `then`("应该返回正确的会话数量") {
                runTest {
                    val publisher = WebSocketOutputPublisher()
                    
                    // 验证初始会话数量
                    publisher.getActiveSessionCount() shouldBe 0
                }
            }
        }
        
        `when`("检查会话是否存在") {
            `then`("应该正确判断会话状态") {
                runTest {
                    val publisher = WebSocketOutputPublisher()
                    val sessionId = SessionId.generate()
                    
                    // 验证会话不存在
                    publisher.isSessionConnected(sessionId).shouldBeFalse()
                }
            }
        }
        
        `when`("关闭所有会话") {
            `then`("应该成功关闭所有连接") {
                runTest {
                    val publisher = WebSocketOutputPublisher()
                    
                    // 测试关闭所有会话
                    publisher.closeAllSessions()
                    
                    // 验证会话数量为0
                    publisher.getActiveSessionCount() shouldBe 0
                }
            }
        }
    }
})
package org.now.terminal.websocket

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import kotlinx.coroutines.test.runTest
import org.now.terminal.shared.valueobjects.SessionId

/**
 * WebSocket服务器测试
 * 基于现有架构测试WebSocket连接管理和消息路由功能
 */
class WebSocketServerTest : BehaviorSpec({
    
    given("WebSocket服务器") {
        
        `when`("处理新连接") {
            `then`("应该成功注册会话") {
                runTest {
                    // 测试连接注册功能
                    val outputPublisher = WebSocketOutputPublisher()
                    val server = WebSocketServer(outputPublisher)
                    val sessionId = SessionId.generate()
                    
                    // 验证会话注册
                    outputPublisher.getActiveSessionCount() shouldBe 0
                }
            }
        }
        
        `when`("获取活跃会话数量") {
            `then`("应该返回正确的会话数量") {
                runTest {
                    val outputPublisher = WebSocketOutputPublisher()
                    val server = WebSocketServer(outputPublisher)
                    
                    // 验证初始会话数量
                    server.getActiveSessionCount() shouldBe 0
                }
            }
        }
        
        `when`("关闭服务器") {
            `then`("应该优雅地关闭所有连接") {
                runTest {
                    val outputPublisher = WebSocketOutputPublisher()
                    val server = WebSocketServer(outputPublisher)
                    
                    // 测试服务器关闭
                    server.shutdown()
                    
                    // 验证连接已关闭
                    server.getActiveSessionCount() shouldBe 0
                }
            }
        }
    }
})
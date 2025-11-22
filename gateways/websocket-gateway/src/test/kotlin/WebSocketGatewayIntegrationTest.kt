package gateways.websocket

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.AfterAll
import java.util.concurrent.atomic.AtomicInteger

/**
 * WebSocket网关集成测试
 * 测试网关与真实客户端的交互场景，模拟完整的终端会话生命周期
 */
class WebSocketGatewayIntegrationTest {
    
    companion object {
        private lateinit var testApplication: TestApplication
        private val connectionCounter = AtomicInteger(0)
        
        @BeforeAll
        @JvmStatic
        fun setup() {
            testApplication = TestApplication {
                // 配置测试应用
                // 这里需要配置实际的WebSocket路由和处理程序
            }
        }
        
        @AfterAll
        @JvmStatic
        fun tearDown() {
            testApplication.stop()
        }
    }
    
    @Test
    fun `should establish WebSocket connection and exchange messages`() = runTest {
        val client = HttpClient(CIO) {
            install(WebSockets)
        }
        
        testApplication.application {
            client.webSocket("/ws") {
                // 测试连接建立
                val connectionId = "test_conn_${connectionCounter.incrementAndGet()}"
                
                // 发送认证消息
                val authMessage = buildJsonObject {
                    put("type", "authenticate")
                    put("token", "test_token_123")
                    put("connectionId", connectionId)
                }.toString()
                
                send(authMessage)
                
                // 接收认证响应
                val authResponse = incoming.receive() as? Frame.Text
                assertNotNull(authResponse)
                
                val authJson = Json.parseToJsonElement(authResponse.readText())
                assertTrue(authJson.jsonObject["authenticated"]?.jsonPrimitive?.booleanOrNull ?: false)
                
                // 发送会话创建请求
                val sessionRequest = buildJsonObject {
                    put("type", "create_session")
                    put("userId", "test_user")
                    put("terminalType", "bash")
                    put("initialSize", buildJsonObject {
                        put("rows", 24)
                        put("cols", 80)
                    })
                }.toString()
                
                send(sessionRequest)
                
                // 接收会话创建响应
                val sessionResponse = incoming.receive() as? Frame.Text
                assertNotNull(sessionResponse)
                
                val sessionJson = Json.parseToJsonElement(sessionResponse.readText())
                val sessionId = sessionJson.jsonObject["sessionId"]?.jsonPrimitive?.content
                assertNotNull(sessionId)
                
                // 发送命令执行请求
                val commandRequest = buildJsonObject {
                    put("type", "execute_command")
                    put("sessionId", sessionId)
                    put("command", "echo 'Hello, WebSocket Gateway!'")
                }.toString()
                
                send(commandRequest)
                
                // 接收命令执行结果
                val commandResponse = incoming.receive() as? Frame.Text
                assertNotNull(commandResponse)
                
                val commandJson = Json.parseToJsonElement(commandResponse.readText())
                assertTrue(commandJson.jsonObject["output"]?.jsonPrimitive?.content?.contains("Hello") ?: false)
                
                // 发送终端尺寸调整请求
                val resizeRequest = buildJsonObject {
                    put("type", "resize_terminal")
                    put("sessionId", sessionId)
                    put("size", buildJsonObject {
                        put("rows", 30)
                        put("cols", 120)
                    })
                }.toString()
                
                send(resizeRequest)
                
                // 接收尺寸调整确认
                val resizeResponse = incoming.receive() as? Frame.Text
                assertNotNull(resizeResponse)
                
                val resizeJson = Json.parseToJsonElement(resizeResponse.readText())
                assertTrue(resizeJson.jsonObject["success"]?.jsonPrimitive?.booleanOrNull ?: false)
                
                // 发送会话终止请求
                val terminateRequest = buildJsonObject {
                    put("type", "terminate_session")
                    put("sessionId", sessionId)
                    put("reason", "test_complete")
                }.toString()
                
                send(terminateRequest)
                
                // 接收会话终止确认
                val terminateResponse = incoming.receive() as? Frame.Text
                assertNotNull(terminateResponse)
                
                val terminateJson = Json.parseToJsonElement(terminateResponse.readText())
                assertTrue(terminateJson.jsonObject["success"]?.jsonPrimitive?.booleanOrNull ?: false)
            }
        }
        
        client.close()
    }
    
    @Test
    fun `should handle multiple concurrent client connections`() = runTest {
        val clientCount = 5
        val clients = List(clientCount) { HttpClient(CIO) { install(WebSockets) } }
        
        val results = mutableListOf<Deferred<Boolean>>()
        
        testApplication.application {
            results.addAll(clients.mapIndexed { index, client ->
                async {
                    client.webSocket("/ws") {
                        val connectionId = "concurrent_conn_$index"
                        
                        // 认证
                        val authMessage = buildJsonObject {
                            put("type", "authenticate")
                            put("token", "token_$index")
                            put("connectionId", connectionId)
                        }.toString()
                        
                        send(authMessage)
                        val authResponse = incoming.receive() as? Frame.Text
                        assertNotNull(authResponse)
                        
                        // 创建会话
                        val sessionRequest = buildJsonObject {
                            put("type", "create_session")
                            put("userId", "user_$index")
                            put("terminalType", "bash")
                        }.toString()
                        
                        send(sessionRequest)
                        val sessionResponse = incoming.receive() as? Frame.Text
                        assertNotNull(sessionResponse)
                        
                        // 执行简单命令
                        val commandRequest = buildJsonObject {
                            put("type", "execute_command")
                            put("sessionId", "session_$index")
                            put("command", "whoami")
                        }.toString()
                        
                        send(commandRequest)
                        val commandResponse = incoming.receive() as? Frame.Text
                        assertNotNull(commandResponse)
                        
                        true
                    }
                }
            })
        }
        
        // 等待所有客户端完成
        results.awaitAll()
        
        clients.forEach { it.close() }
        
        // 验证所有连接都成功处理
        assertEquals(clientCount, results.count { it.getCompleted() })
    }
    
    @Test
    fun `should handle real-time terminal output streaming`() = runTest {
        val client = HttpClient(CIO) {
            install(WebSockets)
        }
        
        val outputMessages = mutableListOf<String>()
        
        testApplication.application {
            client.webSocket("/ws/terminal") {
                // 认证
                val authMessage = buildJsonObject {
                    put("type", "authenticate")
                    put("token", "stream_test_token")
                }.toString()
                
                send(authMessage)
                incoming.receive() // 忽略认证响应
                
                // 创建支持流式输出的会话
                val sessionRequest = buildJsonObject {
                    put("type", "create_session")
                    put("userId", "stream_user")
                    put("terminalType", "bash")
                    put("streamOutput", true)
                }.toString()
                
                send(sessionRequest)
                val sessionResponse = incoming.receive() as? Frame.Text
                val sessionId = Json.parseToJsonElement(sessionResponse!!.readText())
                    .jsonObject["sessionId"]?.jsonPrimitive?.content
                
                // 执行产生持续输出的命令
                val commandRequest = buildJsonObject {
                    put("type", "execute_command")
                    put("sessionId", sessionId)
                    put("command", "for i in {1..5}; do echo 'Output line '\$i; sleep 0.1; done")
                }.toString()
                
                send(commandRequest)
                
                // 收集流式输出
                for (i in 1..5) {
                    val outputFrame = incoming.receive() as? Frame.Text
                    if (outputFrame != null) {
                        outputMessages.add(outputFrame.readText())
                    }
                }
                
                // 验证输出流
                assertEquals(5, outputMessages.size)
                outputMessages.forEachIndexed { index, message ->
                    assertTrue(message.contains("Output line ${index + 1}"))
                }
            }
        }
        
        client.close()
    }
    
    @Test
    fun `should handle authentication failures and reconnection`() = runTest {
        val client = HttpClient(CIO) {
            install(WebSockets)
        }
        
        testApplication.application {
            client.webSocket("/ws") {
                // 发送无效认证
                val invalidAuth = buildJsonObject {
                    put("type", "authenticate")
                    put("token", "invalid_token")
                }.toString()
                
                send(invalidAuth)
                
                // 接收认证失败响应
                val authResponse = incoming.receive() as? Frame.Text
                assertNotNull(authResponse)
                
                val authJson = Json.parseToJsonElement(authResponse.readText())
                assertFalse(authJson.jsonObject["authenticated"]?.jsonPrimitive?.booleanOrNull ?: true)
                assertTrue(authJson.jsonObject["error"]?.jsonPrimitive?.content?.contains("invalid") ?: false)
                
                // 连接应该被关闭
                val closeFrame = incoming.receive() as? Frame.Close
                assertNotNull(closeFrame)
            }
        }
        
        client.close()
    }
    
    @Test
    fun `should handle malformed messages gracefully`() = runTest {
        val client = HttpClient(CIO) {
            install(WebSockets)
        }
        
        testApplication.application {
            client.webSocket("/ws") {
                // 认证
                val authMessage = buildJsonObject {
                    put("type", "authenticate")
                    put("token", "test_token")
                }.toString()
                
                send(authMessage)
                incoming.receive() // 忽略认证响应
                
                // 发送格式错误的消息
                send("invalid json message")
                
                // 接收错误响应
                val errorResponse = incoming.receive() as? Frame.Text
                assertNotNull(errorResponse)
                
                val errorJson = Json.parseToJsonElement(errorResponse.readText())
                assertTrue(errorJson.jsonObject["error"]?.jsonPrimitive?.content?.contains("malformed") ?: false)
                
                // 连接应该保持活跃
                val sessionRequest = buildJsonObject {
                    put("type", "create_session")
                    put("userId", "test_user")
                }.toString()
                
                send(sessionRequest)
                val sessionResponse = incoming.receive() as? Frame.Text
                assertNotNull(sessionResponse)
            }
        }
        
        client.close()
    }
    
    @Test
    fun `should handle session timeout and cleanup`() = runTest {
        val client = HttpClient(CIO) {
            install(WebSockets)
        }
        
        testApplication.application {
            client.webSocket("/ws") {
                // 认证
                val authMessage = buildJsonObject {
                    put("type", "authenticate")
                    put("token", "timeout_test")
                }.toString()
                
                send(authMessage)
                incoming.receive()
                
                // 创建带短超时的会话
                val sessionRequest = buildJsonObject {
                    put("type", "create_session")
                    put("userId", "timeout_user")
                    put("timeoutSeconds", 1) // 1秒超时
                }.toString()
                
                send(sessionRequest)
                val sessionResponse = incoming.receive() as? Frame.Text
                val sessionId = Json.parseToJsonElement(sessionResponse!!.readText())
                    .jsonObject["sessionId"]?.jsonPrimitive?.content
                
                // 等待超时
                delay(1500)
                
                // 尝试使用已超时的会话
                val commandRequest = buildJsonObject {
                    put("type", "execute_command")
                    put("sessionId", sessionId)
                    put("command", "echo test")
                }.toString()
                
                send(commandRequest)
                
                // 接收会话超时错误
                val timeoutResponse = incoming.receive() as? Frame.Text
                assertNotNull(timeoutResponse)
                
                val timeoutJson = Json.parseToJsonElement(timeoutResponse.readText())
                assertTrue(timeoutJson.jsonObject["error"]?.jsonPrimitive?.content?.contains("timeout") ?: false)
            }
        }
        
        client.close()
    }
    
    @Test
    fun `should handle large message payloads correctly`() = runTest {
        val client = HttpClient(CIO) {
            install(WebSockets)
        }
        
        testApplication.application {
            client.webSocket("/ws") {
                // 认证
                val authMessage = buildJsonObject {
                    put("type", "authenticate")
                    put("token", "large_payload_test")
                }.toString()
                
                send(authMessage)
                incoming.receive()
                
                // 发送大负载消息
                val largePayload = "A".repeat(1024 * 10) // 10KB负载
                val largeMessage = buildJsonObject {
                    put("type", "large_data")
                    put("payload", largePayload)
                }.toString()
                
                send(largeMessage)
                
                // 接收处理响应
                val response = incoming.receive() as? Frame.Text
                assertNotNull(response)
                
                val responseJson = Json.parseToJsonElement(response.readText())
                assertTrue(responseJson.jsonObject["processed"]?.jsonPrimitive?.booleanOrNull ?: false)
            }
        }
        
        client.close()
    }
}
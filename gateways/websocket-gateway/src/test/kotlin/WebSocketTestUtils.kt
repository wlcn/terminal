package gateways.websocket

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.json.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.string.shouldContain

/**
 * WebSocket测试工具类
 * 提供测试辅助函数、模拟客户端和测试数据生成器
 */
object WebSocketTestUtils {
    
    /**
     * 测试配置
     */
    data class TestConfig(
        val baseUrl: String = "/ws",
        val timeout: Duration = 30.seconds,
        val maxConnections: Int = 1000,
        val messageBufferSize: Int = 100
    )
    
    /**
     * 模拟WebSocket客户端
     */
    class MockWebSocketClient(
        private val config: TestConfig = TestConfig()
    ) {
        private val client = HttpClient(CIO) {
            install(WebSockets)
        }
        
        private var session: WebSocketSession? = null
        private val messageQueue = Channel<String>(config.messageBufferSize)
        private val receivedMessages = mutableListOf<String>()
        private val connectionId = "mock_client_${System.currentTimeMillis()}"
        
        /**
         * 连接到WebSocket服务器
         */
        suspend fun connect(
            host: String = "localhost",
            port: Int = 8080,
            path: String = "/ws"
        ): Boolean = withTimeout(config.timeout) {
            try {
                client.webSocket(
                    method = HttpMethod.Get,
                    host = host,
                    port = port,
                    path = path
                ) {
                    session = this
                }
                true
            } catch (e: Exception) {
                false
            }
        }
        
        /**
         * 认证客户端
         */
        suspend fun authenticate(token: String = "test_token"): Boolean = withTimeout(config.timeout) {
            val currentSession = session ?: return@withTimeout false
            
            val authMessage = buildJsonObject {
                put("type", "authenticate")
                put("token", token)
                put("connectionId", connectionId)
                put("clientType", "mock")
            }.toString()
            
            currentSession.send(authMessage)
            
            val response = currentSession.incoming.receive() as? Frame.Text
            response?.let { frame ->
                val json = Json.parseToJsonElement(frame.readText())
                json.jsonObject["authenticated"]?.jsonPrimitive?.booleanOrNull ?: false
            } ?: false
        }
        
        /**
         * 创建终端会话
         */
        suspend fun createSession(
            userId: String = "test_user",
            terminalType: String = "bash",
            initialSize: JsonObject = buildJsonObject {
                put("rows", 24)
                put("cols", 80)
            }
        ): String? = withTimeout(config.timeout) {
            val currentSession = session ?: return@withTimeout null
            
            val sessionRequest = buildJsonObject {
                put("type", "create_session")
                put("userId", userId)
                put("terminalType", terminalType)
                put("initialSize", initialSize)
            }.toString()
            
            currentSession.send(Frame.Text(sessionRequest))
            
            val response = currentSession.incoming.receive() as? Frame.Text
            response?.let { frame ->
                val json = Json.parseToJsonElement(frame.readText())
                json.jsonObject["sessionId"]?.jsonPrimitive?.content
            }
        }
        
        /**
         * 执行命令
         */
        suspend fun executeCommand(
            sessionId: String,
            command: String,
            args: List<String> = emptyList()
        ): JsonObject? = withTimeout(config.timeout) {
            val currentSession = session ?: return@withTimeout null
            
            val commandRequest = buildJsonObject {
                put("type", "execute_command")
                put("sessionId", sessionId)
                put("command", command)
                put("args", JsonArray(args.map { JsonPrimitive(it) }))
            }.toString()
            
            currentSession.send(Frame.Text(commandRequest))
            
            val response = currentSession.incoming.receive() as? Frame.Text
            response?.let { frame ->
                Json.parseToJsonElement(frame.readText()).jsonObject
            }
        }
        
        /**
         * 调整终端尺寸
         */
        suspend fun resizeTerminal(
            sessionId: String,
            rows: Int,
            cols: Int
        ): Boolean = withTimeout(config.timeout) {
            val currentSession = session ?: return@withTimeout false
            
            val resizeRequest = buildJsonObject {
                put("type", "resize_terminal")
                put("sessionId", sessionId)
                put("size", buildJsonObject {
                    put("rows", rows)
                    put("cols", cols)
                })
            }.toString()
            
            currentSession.send(Frame.Text(resizeRequest))
            
            val response = currentSession.incoming.receive() as? Frame.Text
            response?.let { frame ->
                val json = Json.parseToJsonElement(frame.readText())
                json.jsonObject["success"]?.jsonPrimitive?.booleanOrNull ?: false
            } ?: false
        }
        
        /**
         * 发送自定义消息
         */
        suspend fun sendMessage(message: JsonObject): Boolean = withTimeout(config.timeout) {
            val currentSession = session ?: return@withTimeout false
            
            try {
                currentSession.send(Frame.Text(message.toString()))
                true
            } catch (e: Exception) {
                false
            }
        }
        
        /**
         * 接收消息
         */
        suspend fun receiveMessage(): String? = withTimeout(config.timeout) {
            val currentSession = session ?: return@withTimeout null
            
            try {
                val frame = currentSession.incoming.receive()
                when (frame) {
                    is Frame.Text -> frame.readText()
                    is Frame.Binary -> "binary_data"
                    else -> null
                }
            } catch (e: Exception) {
                null
            }
        }
        
        /**
         * 启动消息监听器
         */
        fun startMessageListener() = GlobalScope.launch {
            val currentSession = session ?: return@launch
            
            while (isActive) {
                try {
                    val message = receiveMessage()
                    message?.let { 
                        receivedMessages.add(it)
                        messageQueue.send(it)
                    }
                } catch (e: Exception) {
                    break
                }
            }
        }
        
        /**
         * 获取接收到的消息
         */
        fun getReceivedMessages(): List<String> = receivedMessages.toList()
        
        /**
         * 等待特定消息
         */
        suspend fun waitForMessage(
            predicate: (String) -> Boolean,
            timeout: Duration = config.timeout
        ): String? = withTimeout(timeout) {
            while (isActive) {
                val message = messageQueue.receive()
                if (predicate(message)) {
                    return@withTimeout message
                }
            }
            null
        }
        
        /**
         * 关闭连接
         */
        suspend fun close(reason: String = "test_complete") {
            try {
                session?.close()
                client.close()
            } catch (e: Exception) {
                // 忽略关闭异常
            }
        }
        
        /**
         * 获取连接状态
         */
        fun isConnected(): Boolean = session?.isActive == true
    }
    
    /**
     * 测试数据生成器
     */
    object TestDataGenerator {
        private val userCounter = AtomicInteger(0)
        private val sessionCounter = AtomicInteger(0)
        private val commandCounter = AtomicInteger(0)
        
        /**
         * 生成测试用户ID
         */
        fun generateUserId(prefix: String = "test_user"): String {
            return "${prefix}_${userCounter.incrementAndGet()}"
        }
        
        /**
         * 生成测试会话ID
         */
        fun generateSessionId(prefix: String = "test_session"): String {
            return "${prefix}_${sessionCounter.incrementAndGet()}"
        }
        
        /**
         * 生成测试命令
         */
        fun generateCommand(prefix: String = "test_command"): String {
            return "${prefix}_${commandCounter.incrementAndGet()}"
        }
        
        /**
         * 生成认证消息
         */
        fun createAuthMessage(
            token: String = "test_token",
            connectionId: String = generateSessionId("conn")
        ): JsonObject {
            return buildJsonObject {
                put("type", "authenticate")
                put("token", token)
                put("connectionId", connectionId)
                put("timestamp", System.currentTimeMillis())
            }
        }
        
        /**
         * 生成会话创建消息
         */
        fun createSessionMessage(
            userId: String = generateUserId(),
            terminalType: String = "bash",
            initialSize: JsonObject = buildJsonObject {
                put("rows", 24)
                put("cols", 80)
            }
        ): JsonObject {
            return buildJsonObject {
                put("type", "create_session")
                put("userId", userId)
                put("terminalType", terminalType)
                put("initialSize", initialSize)
                put("timestamp", System.currentTimeMillis())
            }
        }
        
        /**
         * 生成命令执行消息
         */
        fun createCommandMessage(
            sessionId: String = generateSessionId(),
            command: String = generateCommand(),
            args: List<String> = emptyList(),
            workingDir: String = "/tmp"
        ): JsonObject {
            return buildJsonObject {
                put("type", "execute_command")
                put("sessionId", sessionId)
                put("command", command)
                put("args", Json.encodeToJsonElement(args))
                put("workingDir", workingDir)
                put("timestamp", System.currentTimeMillis())
            }
        }
        
        /**
         * 生成终端调整消息
         */
        fun createResizeMessage(
            sessionId: String = generateSessionId(),
            rows: Int = 30,
            cols: Int = 120
        ): JsonObject {
            return buildJsonObject {
                put("type", "resize_terminal")
                put("sessionId", sessionId)
                put("size", buildJsonObject {
                    put("rows", rows)
                    put("cols", cols)
                })
                put("timestamp", System.currentTimeMillis())
            }
        }
        
        /**
         * 生成大负载测试数据
         */
        fun createLargePayload(sizeKB: Int): String {
            val payload = "A".repeat(sizeKB * 1024)
            return buildJsonObject {
                put("type", "large_payload")
                put("size", sizeKB)
                put("payload", payload)
                put("timestamp", System.currentTimeMillis())
            }.toString()
        }
        
        /**
         * 生成性能测试消息序列
         */
        fun generateMessageSequence(count: Int): List<JsonObject> {
            return List(count) { index ->
                buildJsonObject {
                    put("type", "performance_test")
                    put("sequence", index)
                    put("data", "test_data_${index}".repeat(10))
                    put("timestamp", System.currentTimeMillis())
                }
            }
        }
    }
    
    /**
     * 断言工具
     */
    object AssertionUtils {
        
        /**
         * 验证消息格式
         */
        fun assertValidMessageFormat(message: String) {
            val json = Json.parseToJsonElement(message)
            (json is JsonObject) shouldBe true
            
            val obj = json.jsonObject
            obj.containsKey("type") shouldBe true
            (obj["type"] is JsonPrimitive) shouldBe true
        }
        
        /**
         * 验证认证响应
         */
        fun assertAuthenticationResponse(message: String, expected: Boolean = true) {
            val json = Json.parseToJsonElement(message)
            val obj = json.jsonObject
            
            obj["type"]?.jsonPrimitive?.content shouldBe "authentication_response"
            obj["authenticated"]?.jsonPrimitive?.booleanOrNull shouldBe expected
            
            if (expected) {
                obj.containsKey("connectionId") shouldBe true
            } else {
                obj.containsKey("error") shouldBe true
            }
        }
        
        /**
         * 验证会话创建响应
         */
        fun assertSessionCreationResponse(message: String) {
            val json = Json.parseToJsonElement(message)
            val obj = json.jsonObject
            
            obj["type"]?.jsonPrimitive?.content shouldBe "session_created"
            obj.containsKey("sessionId") shouldBe true
            obj.containsKey("status") shouldBe true
            obj["status"]?.jsonPrimitive?.content shouldBe "active"
        }
        
        /**
         * 验证命令执行响应
         */
        fun assertCommandExecutionResponse(message: String) {
            val json = Json.parseToJsonElement(message)
            val obj = json.jsonObject
            
            obj["type"]?.jsonPrimitive?.content shouldBe "command_executed"
            obj.containsKey("exitCode") shouldBe true
            obj.containsKey("output") shouldBe true
        }
        
        /**
         * 验证错误响应
         */
        fun assertErrorResponse(message: String, expectedErrorType: String? = null) {
            val json = Json.parseToJsonElement(message)
            val obj = json.jsonObject
            
            obj["type"]?.jsonPrimitive?.content shouldBe "error"
            obj.containsKey("error") shouldBe true
            
            expectedErrorType?.let { errorType ->
                (obj["error"]?.jsonPrimitive?.content?.contains(errorType) == true) shouldBe true
            }
        }
        
        /**
         * 验证消息顺序
         */
        fun assertMessageOrder(messages: List<String>, sequenceField: String = "sequence") {
            val sequences = messages.mapNotNull { message ->
                val json = Json.parseToJsonElement(message)
                json.jsonObject[sequenceField]?.jsonPrimitive?.intOrNull
            }
            
            sequences shouldBe sequences.sorted()
        }
        
        /**
         * 验证响应时间在阈值内
         */
        fun <T> assertResponseTime(
            operation: suspend () -> T,
            maxTimeMs: Long,
            message: String = "操作响应时间超过阈值"
        ): T {
            val startTime = System.currentTimeMillis()
            val result = runBlocking { operation() }
            val endTime = System.currentTimeMillis()
            
            (endTime - startTime <= maxTimeMs) shouldBe true
            return result
        }
    }
    
    /**
     * 性能监控工具
     */
    object PerformanceMonitor {
        private val metrics = mutableMapOf<String, MutableList<Long>>()
        
        /**
         * 记录性能指标
         */
        fun recordMetric(name: String, value: Long) {
            metrics.getOrPut(name) { mutableListOf() }.add(value)
        }
        
        /**
         * 获取性能统计
         */
        fun getStats(name: String): PerformanceStats? {
            val values = metrics[name] ?: return null
            return PerformanceStats(
                count = values.size,
                average = values.average(),
                min = values.minOrNull() ?: 0L,
                max = values.maxOrNull() ?: 0L,
                p50 = values.sorted()[values.size / 2],
                p90 = values.sorted()[values.size * 90 / 100],
                p95 = values.sorted()[values.size * 95 / 100],
                p99 = values.sorted()[values.size * 99 / 100]
            )
        }
        
        /**
         * 清除所有指标
         */
        fun clear() {
            metrics.clear()
        }
        
        /**
         * 输出性能报告
         */
        fun printReport() {
            println("\n=== 性能监控报告 ===")
            metrics.keys.sorted().forEach { metricName ->
                getStats(metricName)?.let { stats ->
                    println("$metricName:")
                    println("  样本数: ${stats.count}")
                    println("  平均: ${stats.average}ms")
                    println("  最小: ${stats.min}ms")
                    println("  最大: ${stats.max}ms")
                    println("  50分位: ${stats.p50}ms")
                    println("  90分位: ${stats.p90}ms")
                    println("  95分位: ${stats.p95}ms")
                    println("  99分位: ${stats.p99}ms")
                    println()
                }
            }
        }
        
        data class PerformanceStats(
            val count: Int,
            val average: Double,
            val min: Long,
            val max: Long,
            val p50: Long,
            val p90: Long,
            val p95: Long,
            val p99: Long
        )
    }
}
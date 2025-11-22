package gateways.websocket

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.AfterAll
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

/**
 * WebSocket网关性能测试
 * 测试网关在高并发、高负载场景下的性能表现
 */
class WebSocketGatewayPerformanceTest {
    
    companion object {
        private lateinit var testApplication: TestApplication
        private val performanceStats = mutableMapOf<String, MutableList<Long>>()
        
        @BeforeAll
        @JvmStatic
        fun setup() {
            testApplication = TestApplication {
                // 配置测试应用
            }
        }
        
        @AfterAll
        @JvmStatic
        fun tearDown() {
            testApplication.stop()
            
            // 输出性能统计
            println("\n=== 性能测试统计 ===")
            performanceStats.forEach { (testName, times) ->
                val avg = times.average()
                val min = times.minOrNull() ?: 0L
                val max = times.maxOrNull() ?: 0L
                val p95 = times.sorted()[times.size * 95 / 100]
                
                println("$testName:")
                println("  样本数: ${times.size}")
                println("  平均时间: ${avg}ms")
                println("  最小时间: ${min}ms")
                println("  最大时间: ${max}ms")
                println("  95分位: ${p95}ms")
                println()
            }
        }
        
        private fun recordPerformance(testName: String, timeMs: Long) {
            performanceStats.getOrPut(testName) { mutableListOf() }.add(timeMs)
        }
    }
    
    @Test
    fun `should handle 100 concurrent connections with low latency`() = runTest {
        val connectionCount = 100
        val clients = List(connectionCount) { HttpClient(CIO) { install(WebSockets) } }
        val results = mutableListOf<Deferred<Long>>()
        
        val executionTime = measureTimeMillis {
            testApplication.application {
                results.addAll(clients.mapIndexed { index, client ->
                    async {
                        measureTimeMillis {
                            client.webSocket("/ws") {
                                // 认证
                                val authMessage = buildJsonObject {
                                    put("type", "authenticate")
                                    put("token", "perf_token_$index")
                                }.toString()
                                
                                send(authMessage)
                                incoming.receive()
                                
                                // 创建会话
                                val sessionRequest = buildJsonObject {
                                    put("type", "create_session")
                                    put("userId", "perf_user_$index")
                                }.toString()
                                
                                send(sessionRequest)
                                incoming.receive()
                                
                                // 执行简单命令
                                val commandRequest = buildJsonObject {
                                    put("type", "execute_command")
                                    put("command", "echo 'performance test'")
                                }.toString()
                                
                                send(commandRequest)
                                incoming.receive()
                            }
                        }
                    }
                })
                
                results.awaitAll()
            }
        }
        
        clients.forEach { it.close() }
        
        // 记录性能数据
        recordPerformance("100_concurrent_connections", executionTime)
        
        // 验证所有连接成功
        assertEquals(connectionCount, results.count { it.getCompleted() > 0 })
        
        // 验证平均响应时间在合理范围内
        val avgResponseTime = results.map { it.getCompleted() }.average()
        assertTrue(avgResponseTime < 5000, "平均响应时间 ${avgResponseTime}ms 超过5秒阈值")
        
        println("100并发连接测试完成，总耗时: ${executionTime}ms，平均响应时间: ${avgResponseTime}ms")
    }
    
    @Test
    fun `should handle 1000 messages per second throughput`() = runTest {
        val messageCount = 1000
        val client = HttpClient(CIO) { install(WebSockets) }
        
        val throughputTime = measureTimeMillis {
            testApplication.application {
                client.webSocket("/ws") {
                    // 认证
                    val authMessage = buildJsonObject {
                        put("type", "authenticate")
                        put("token", "throughput_test")
                    }.toString()
                    
                    send(authMessage)
                    incoming.receive()
                    
                    // 创建会话
                    val sessionRequest = buildJsonObject {
                        put("type", "create_session")
                        put("userId", "throughput_user")
                    }.toString()
                    
                    send(sessionRequest)
                    val sessionResponse = incoming.receive() as? Frame.Text
                    val sessionId = Json.parseToJsonElement(sessionResponse!!.readText())
                        .jsonObject["sessionId"]?.jsonPrimitive?.content
                    
                    // 发送大量消息
                    repeat(messageCount) { index ->
                        val message = buildJsonObject {
                            put("type", "test_message")
                            put("sessionId", sessionId)
                            put("sequence", index)
                            put("timestamp", System.currentTimeMillis())
                        }.toString()
                        
                        send(message)
                        
                        // 接收响应（非阻塞方式）
                        if (incoming.isEmpty.not()) {
                            incoming.receive()
                        }
                    }
                }
            }
        }
        
        client.close()
        
        // 计算吞吐量
        val throughput = messageCount.toDouble() / (throughputTime / 1000.0)
        recordPerformance("1000_messages_throughput", throughputTime)
        
        // 验证吞吐量达到要求
        assertTrue(throughput > 800, "吞吐量 ${throughput} msg/s 低于800 msg/s要求")
        
        println("1000消息吞吐量测试完成，耗时: ${throughputTime}ms，吞吐量: ${throughput} msg/s")
    }
    
    @Test
    fun `should maintain low memory usage under sustained load`() = runTest {
        val durationSeconds = 30
        val client = HttpClient(CIO) { install(WebSockets) }
        val messageChannel = Channel<String>(Channel.UNLIMITED)
        
        val memoryUsage = measureTimeMillis {
            testApplication.application {
                client.webSocket("/ws") {
                    // 认证
                    val authMessage = buildJsonObject {
                        put("type", "authenticate")
                        put("token", "memory_test")
                    }.toString()
                    
                    send(authMessage)
                    incoming.receive()
                    
                    // 创建会话
                    val sessionRequest = buildJsonObject {
                        put("type", "create_session")
                        put("userId", "memory_user")
                    }.toString()
                    
                    send(sessionRequest)
                    val sessionResponse = incoming.receive() as? Frame.Text
                    val sessionId = Json.parseToJsonElement(sessionResponse!!.readText())
                        .jsonObject["sessionId"]?.jsonPrimitive?.content
                    
                    // 启动消息生产者
                    val producer = launch {
                        var sequence = 0
                        while (isActive) {
                            val message = buildJsonObject {
                                put("type", "sustained_load")
                                put("sessionId", sessionId)
                                put("sequence", sequence++)
                                put("data", "A".repeat(1024)) // 1KB数据
                            }.toString()
                            
                            messageChannel.send(message)
                            delay(100) // 10 msg/s
                        }
                    }
                    
                    // 持续发送和接收消息
                    val startTime = System.currentTimeMillis()
                    var messageCount = 0
                    
                    while (System.currentTimeMillis() - startTime < durationSeconds * 1000) {
                        // 发送消息
                        if (messageChannel.isEmpty.not()) {
                            val message = messageChannel.receive()
                            send(message)
                            messageCount++
                        }
                        
                        // 接收响应
                        if (incoming.isEmpty.not()) {
                            incoming.receive()
                        }
                        
                        delay(10)
                    }
                    
                    producer.cancel()
                    
                    println("持续负载测试完成，发送消息数: $messageCount")
                }
            }
        }
        
        client.close()
        messageChannel.close()
        
        recordPerformance("sustained_load_memory", memoryUsage)
        
        // 验证在持续负载下没有内存泄漏迹象
        assertTrue(memoryUsage < durationSeconds * 1000 * 2, "内存使用异常增长")
        
        println("持续负载内存测试完成，总耗时: ${memoryUsage}ms")
    }
    
    @Test
    fun `should handle connection burst scenarios`() = runTest {
        val burstSizes = listOf(10, 50, 100)
        val results = mutableMapOf<Int, Pair<Long, Double>>()
        
        burstSizes.forEach { burstSize ->
            val clients = List(burstSize) { HttpClient(CIO) { install(WebSockets) } }
            val connectionTasks = mutableListOf<Deferred<Long>>()
            
            val burstTime = measureTimeMillis {
                testApplication.application {
                    connectionTasks.addAll(clients.mapIndexed { index, client ->
                        async {
                            measureTimeMillis {
                                client.webSocket("/ws") {
                                    // 快速认证和会话创建
                                    val authMessage = buildJsonObject {
                                        put("type", "authenticate")
                                        put("token", "burst_${burstSize}_$index")
                                    }.toString()
                                    
                                    send(authMessage)
                                    incoming.receive()
                                    
                                    // 立即关闭连接（模拟突发连接）
                                    close(CloseReason(CloseReason.Codes.NORMAL, "burst_test"))
                                }
                            }
                        }
                    })
                    
                    connectionTasks.awaitAll()
                }
            }
            
            clients.forEach { it.close() }
            
            val avgConnectionTime = connectionTasks.map { it.getCompleted() }.average()
            results[burstSize] = Pair(burstTime, avgConnectionTime)
            
            recordPerformance("burst_${burstSize}_connections", burstTime)
            
            println("突发连接测试(${burstSize}连接)完成，总耗时: ${burstTime}ms，平均连接时间: ${avgConnectionTime}ms")
        }
        
        // 验证突发连接处理能力
        results.forEach { (size, (totalTime, avgTime)) ->
            assertTrue(avgTime < 1000, "${size}连接突发场景平均连接时间 ${avgTime}ms 超过1秒")
            assertTrue(totalTime < size * 200, "${size}连接突发场景总耗时 ${totalTime}ms 异常")
        }
    }
    
    @Test
    fun `should maintain message ordering under high concurrency`() = runTest {
        val messageCount = 500
        val client = HttpClient(CIO) { install(WebSockets) }
        val receivedMessages = mutableListOf<Int>()
        
        val orderingTime = measureTimeMillis {
            testApplication.application {
                client.webSocket("/ws/ordered") {
                    // 认证
                    val authMessage = buildJsonObject {
                        put("type", "authenticate")
                        put("token", "ordering_test")
                    }.toString()
                    
                    send(authMessage)
                    incoming.receive()
                    
                    // 发送有序消息
                    val sendJobs = List(messageCount) { index ->
                        async {
                            val message = buildJsonObject {
                                put("type", "ordered_message")
                                put("sequence", index)
                                put("timestamp", System.currentTimeMillis())
                            }.toString()
                            
                            send(message)
                        }
                    }
                    
                    sendJobs.awaitAll()
                    
                    // 接收有序响应
                    repeat(messageCount) {
                        val response = incoming.receive() as? Frame.Text
                        if (response != null) {
                            val json = Json.parseToJsonElement(response.readText())
                            val sequence = json.jsonObject["sequence"]?.jsonPrimitive?.intOrNull
                            sequence?.let { receivedMessages.add(it) }
                        }
                    }
                }
            }
        }
        
        client.close()
        
        recordPerformance("message_ordering", orderingTime)
        
        // 验证消息顺序
        assertEquals(messageCount, receivedMessages.size)
        assertTrue(receivedMessages == receivedMessages.sorted(), "消息顺序被打乱")
        
        println("消息顺序测试完成，总耗时: ${orderingTime}ms，消息数: ${receivedMessages.size}")
    }
    
    @Test
    fun `should handle graceful degradation under extreme load`() = runTest {
        val extremeLoadSize = 500
        val clients = List(extremeLoadSize) { HttpClient(CIO) { install(WebSockets) } }
        val results = mutableListOf<Deferred<Boolean>>()
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        
        val degradationTime = measureTimeMillis {
            testApplication.application {
                results.addAll(clients.mapIndexed { index, client ->
                    async {
                        try {
                            client.webSocket("/ws") {
                                // 尝试认证（可能失败）
                                val authMessage = buildJsonObject {
                                    put("type", "authenticate")
                                    put("token", "extreme_$index")
                                }.toString()
                                
                                send(authMessage)
                                
                                val response = incoming.receive() as? Frame.Text
                                if (response != null) {
                                    val json = Json.parseToJsonElement(response.readText())
                                    if (json.jsonObject["authenticated"]?.jsonPrimitive?.booleanOrNull == true) {
                                        successCount.incrementAndGet()
                                        // 成功连接后立即关闭
                                        close(CloseReason(CloseReason.Codes.NORMAL, "test_complete"))
                                        true
                                    } else {
                                        failureCount.incrementAndGet()
                                        false
                                    }
                                } else {
                                    failureCount.incrementAndGet()
                                    false
                                }
                            }
                        } catch (e: Exception) {
                            failureCount.incrementAndGet()
                            false
                        }
                    }
                })
                
                results.awaitAll()
            }
        }
        
        clients.forEach { it.close() }
        
        recordPerformance("graceful_degradation", degradationTime)
        
        // 验证优雅降级：即使在高负载下，系统仍能处理部分请求
        val successRate = successCount.get() / extremeLoadSize.toDouble()
        assertTrue(successRate > 0.1, "极端负载下成功率为 ${successRate * 100}%，低于10%阈值")
        
        println("优雅降级测试完成，总耗时: ${degradationTime}ms，成功率: ${successRate * 100}%")
        println("成功连接: ${successCount.get()}，失败连接: ${failureCount.get()}")
    }
    
    @Test
    fun `should measure message processing latency distribution`() = runTest {
        val sampleSize = 100
        val client = HttpClient(CIO) { install(WebSockets) }
        val latencies = mutableListOf<Long>()
        
        val distributionTime = measureTimeMillis {
            testApplication.application {
                client.webSocket("/ws/latency") {
                    // 认证
                    val authMessage = buildJsonObject {
                        put("type", "authenticate")
                        put("token", "latency_test")
                    }.toString()
                    
                    send(authMessage)
                    incoming.receive()
                    
                    // 测量消息处理延迟
                    repeat(sampleSize) { index ->
                        val startTime = System.nanoTime()
                        
                        val message = buildJsonObject {
                            put("type", "latency_measure")
                            put("sequence", index)
                            put("sendTime", startTime)
                        }.toString()
                        
                        send(message)
                        
                        val response = incoming.receive() as? Frame.Text
                        val endTime = System.nanoTime()
                        
                        if (response != null) {
                            val latency = (endTime - startTime) / 1_000_000 // 转换为毫秒
                            latencies.add(latency)
                        }
                        
                        delay(10) // 避免过载
                    }
                }
            }
        }
        
        client.close()
        
        recordPerformance("latency_distribution", distributionTime)
        
        // 计算延迟分布统计
        val avgLatency = latencies.average()
        val p50 = latencies.sorted()[latencies.size / 2]
        val p90 = latencies.sorted()[latencies.size * 90 / 100]
        val p99 = latencies.sorted()[latencies.size * 99 / 100]
        
        // 验证延迟在合理范围内
        assertTrue(p99 < 100, "99分位延迟 ${p99}ms 超过100ms阈值")
        
        println("延迟分布测试完成:")
        println("  平均延迟: ${avgLatency}ms")
        println("  50分位: ${p50}ms")
        println("  90分位: ${p90}ms")
        println("  99分位: ${p99}ms")
        println("  样本数: ${latencies.size}")
    }
}
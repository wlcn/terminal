package org.now.terminal.session.domain.services

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldBeEmpty
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.now.terminal.session.domain.entities.SessionStatistics
import org.now.terminal.session.domain.entities.TerminalSession
import org.now.terminal.session.domain.entities.SessionStatus
import org.now.terminal.session.domain.valueobjects.PtyConfiguration
import org.now.terminal.session.domain.valueobjects.TerminalCommand
import org.now.terminal.session.domain.valueobjects.TerminalSize
import org.now.terminal.session.domain.valueobjects.TerminationReason
import org.now.terminal.shared.valueobjects.SessionId
import org.now.terminal.shared.valueobjects.UserId
import java.time.Instant

class TerminalSessionServiceTest : BehaviorSpec({
    
    given("TerminalSessionService接口测试") {
        
        `when`("使用测试实现") {
            val testService = object : TerminalSessionService {
                private val activeSessions = mutableMapOf<SessionId, PtyConfiguration>()
                private val userSessions = mutableMapOf<UserId, MutableSet<SessionId>>()
                
                var lastInput: String? = null
                var lastSessionId: SessionId? = null
                var lastResizeSize: TerminalSize? = null
                var lastResizeSessionId: SessionId? = null
                
                override suspend fun createSession(userId: UserId, ptyConfig: PtyConfiguration): SessionId {
                    val sessionId = SessionId.generate()
                    activeSessions[sessionId] = ptyConfig
                    userSessions.getOrPut(userId) { mutableSetOf() }.add(sessionId)
                    return sessionId
                }
                
                override suspend fun terminateSession(sessionId: SessionId, reason: TerminationReason) {
                    activeSessions.remove(sessionId)
                    userSessions.values.forEach { it.remove(sessionId) }
                }
                
                override suspend fun handleInput(sessionId: SessionId, input: String) {
                    lastInput = input
                    lastSessionId = sessionId
                }
                
                override suspend fun resizeTerminal(sessionId: SessionId, size: TerminalSize) {
                    lastResizeSize = size
                    lastResizeSessionId = sessionId
                }
                
                override suspend fun listActiveSessions(userId: UserId): List<TerminalSession> {
                    return userSessions[userId]?.map { sessionId ->
                        // 创建模拟的TerminalSession对象用于测试
                        val mockProcessFactory = mockk<ProcessFactory>()
                        val session = TerminalSession(sessionId, userId, activeSessions[sessionId]!!, mockProcessFactory)
                        session
                    } ?: emptyList()
                }
                
                override suspend fun readOutput(sessionId: SessionId): String {
                    return if (activeSessions.containsKey(sessionId)) {
                        "Mock terminal output for session: $sessionId"
                    } else ""
                }
                
                override suspend fun getSessionStatistics(sessionId: SessionId): SessionStatistics {
                    return SessionStatistics(
                        sessionId = sessionId,
                        userId = UserId.generate(), // 模拟用户ID
                        status = SessionStatus.RUNNING,
                        createdAt = Instant.now(),
                        terminatedAt = null,
                        exitCode = null,
                        outputSize = 1024
                    )
                }
                
                override suspend fun terminateAllUserSessions(userId: UserId, reason: TerminationReason) {
                    userSessions[userId]?.forEach { sessionId ->
                        activeSessions.remove(sessionId)
                    }
                    userSessions.remove(userId)
                }
                
                override suspend fun isSessionActive(sessionId: SessionId): Boolean {
                    return activeSessions.containsKey(sessionId)
                }
                
                override suspend fun getSessionConfiguration(sessionId: SessionId): PtyConfiguration {
                    return activeSessions[sessionId] ?: PtyConfiguration.createDefault(TerminalCommand("bash"))
                }
            }
            
            then("应该正确创建会话") {
                runBlocking {
                    // Given
                    val userId = UserId.generate()
                    val ptyConfig = PtyConfiguration.createDefault(TerminalCommand("bash"))
                    
                    // When
                    val sessionId = testService.createSession(userId, ptyConfig)
                    
                    // Then
                    testService.isSessionActive(sessionId).shouldBe(true)
                }
            }
            
            then("应该正确终止会话") {
                runBlocking {
                    // Given
                    val userId = UserId.generate()
                    val ptyConfig = PtyConfiguration.createDefault(TerminalCommand("bash"))
                    val sessionId = testService.createSession(userId, ptyConfig)
                    
                    // When
                    testService.terminateSession(sessionId, TerminationReason.USER_REQUESTED)

                    // Then
                    testService.isSessionActive(sessionId).shouldBe(false)
                }
            }
            
            then("应该正确处理终端输入") {
                runBlocking {
                    // Given
                    val userId = UserId.generate()
                    val ptyConfig = PtyConfiguration.createDefault(TerminalCommand("bash"))
                    val sessionId = testService.createSession(userId, ptyConfig)
                    
                    // When
                    testService.handleInput(sessionId, "ls -la")
                    
                    // Then
                    testService.lastInput shouldBe "ls -la"
                    testService.lastSessionId shouldBe sessionId
                }
            }
            
            then("应该正确调整终端尺寸") {
                runBlocking {
                    // Given
                    val userId = UserId.generate()
                    val ptyConfig = PtyConfiguration.createDefault(TerminalCommand("bash"))
                    val sessionId = testService.createSession(userId, ptyConfig)
                    val newSize = TerminalSize(120, 40)
                    
                    // When
                    testService.resizeTerminal(sessionId, newSize)
                    
                    // Then
                    testService.lastResizeSize shouldBe newSize
                    testService.lastResizeSessionId shouldBe sessionId
                }
            }
            
            then("应该正确列出活跃会话") {
                runBlocking {
                    // Given
                    val userId = UserId.generate()
                    val ptyConfig = PtyConfiguration.createDefault(TerminalCommand("bash"))
                    
                    // 创建多个会话
                    val session1 = testService.createSession(userId, ptyConfig)
                    val session2 = testService.createSession(userId, ptyConfig)
                    
                    // When
                    val activeSessions = testService.listActiveSessions(userId)
                    
                    // Then
                    activeSessions.size shouldBe 2
                    activeSessions.map { it.sessionId } shouldContainExactly listOf(session1, session2)
                }
            }
            
            then("应该正确读取会话输出") {
                runBlocking {
                    // Given
                    val userId = UserId.generate()
                    val ptyConfig = PtyConfiguration.createDefault(TerminalCommand("bash"))
                    val sessionId = testService.createSession(userId, ptyConfig)
                    
                    // When
                    val output = testService.readOutput(sessionId)
                    
                    // Then
                    output shouldBe "Mock terminal output for session: $sessionId"
                }
            }
            
            then("应该正确获取会话统计信息") {
                runBlocking {
                    // Given
                    val userId = UserId.generate()
                    val ptyConfig = PtyConfiguration.createDefault(TerminalCommand("bash"))
                    val sessionId = testService.createSession(userId, ptyConfig)
                    
                    // When
                    val statistics = testService.getSessionStatistics(sessionId)
                    
                    // Then
                    statistics shouldBe SessionStatistics(
                        sessionId = sessionId,
                        userId = userId,
                        status = SessionStatus.RUNNING,
                        createdAt = statistics.createdAt, // 使用实际创建的时间
                        terminatedAt = null,
                        exitCode = null,
                        outputSize = 1024
                    )
                }
            }
            
            then("应该正确强制终止所有用户会话") {
                runBlocking {
                    // Given
                    val userId = UserId.generate()
                    val ptyConfig = PtyConfiguration.createDefault(TerminalCommand("bash"))
                    
                    // 创建多个会话
                    val session1 = testService.createSession(userId, ptyConfig)
                    val session2 = testService.createSession(userId, ptyConfig)
                    
                    // 验证会话活跃
                    testService.isSessionActive(session1).shouldBe(true)
                    testService.isSessionActive(session2).shouldBe(true)
                    
                    // When
                    testService.terminateAllUserSessions(userId, TerminationReason.SYSTEM_ERROR)

                    // Then
                    testService.isSessionActive(session1).shouldBe(false)
                    testService.isSessionActive(session2).shouldBe(false)
                    testService.listActiveSessions(userId).shouldBeEmpty()
                }
            }
            
            then("应该正确获取会话配置") {
                runBlocking {
                    // Given
                    val userId = UserId.generate()
                    val originalConfig = PtyConfiguration.createDefault(TerminalCommand("bash"))
                    val sessionId = testService.createSession(userId, originalConfig)
                    
                    // When
                    val retrievedConfig = testService.getSessionConfiguration(sessionId)
                    
                    // Then
                    retrievedConfig shouldBe originalConfig
                }
            }
            
            then("应该正确处理不存在的会话") {
                runBlocking {
                    // Given
                    val nonExistentSessionId = SessionId.generate()
                    
                    // When & Then - 检查不存在的会话状态
                    testService.isSessionActive(nonExistentSessionId).shouldBe(false)
                    
                    // 尝试读取不存在的会话输出（应该返回空字符串）
                    val output = testService.readOutput(nonExistentSessionId)
                    output shouldBe ""
                }
            }
        }
    }
})
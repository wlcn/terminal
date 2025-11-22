package org.now.terminal.session.application.usecases

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.now.terminal.infrastructure.configuration.ConfigurationManager
import org.now.terminal.session.domain.services.TerminalSessionService
import org.now.terminal.shared.valueobjects.SessionId
import org.now.terminal.shared.valueobjects.UserId
import org.now.terminal.session.domain.entities.TerminalSession
import org.now.terminal.session.domain.valueobjects.PtyConfiguration
import org.now.terminal.session.domain.valueobjects.TerminalSize
import org.now.terminal.session.domain.valueobjects.TerminalCommand

class UseCasesTest : BehaviorSpec({
    
    val mockTerminalSessionService = mockk<TerminalSessionService>()
    
    beforeTest {
        // 初始化配置管理器（用于测试环境）
        ConfigurationManager.initialize(environment = "test")
    }
    
    afterTest {
        // 清理配置管理器
        ConfigurationManager.reset()
    }
    
    given("CreateSessionUseCase") {
        val createSessionUseCase = CreateSessionUseCase(mockTerminalSessionService)
        val userId = UserId.create("usr_123e4567-e89b-12d3-a456-426614174000")
        val ptyConfig = PtyConfiguration(
            command = TerminalCommand("/bin/bash"),
            environment = mapOf("TERM" to "xterm-256color"),
            size = TerminalSize(80, 24),
            workingDirectory = "/home/user"
        )
        val sessionId = SessionId.generate()
        
        `when`("执行创建会话用例") {
            coEvery { mockTerminalSessionService.createSession(userId, ptyConfig) } returns sessionId
            val result = createSessionUseCase.execute(userId, ptyConfig)
            
            then("应该调用服务层并返回会话ID") {
                coVerify { mockTerminalSessionService.createSession(userId, ptyConfig) }
                result shouldBe sessionId
            }
        }
    }
    
    given("HandleInputUseCase") {
        val handleInputUseCase = HandleInputUseCase(mockTerminalSessionService)
        val sessionId = SessionId.generate()
        val input = "ls -la"
        
        `when`("执行处理输入用例") {
            coEvery { mockTerminalSessionService.handleInput(sessionId, input) } returns Unit
            handleInputUseCase.execute(sessionId, input)
            
            then("应该调用服务层处理输入") {
                coVerify { mockTerminalSessionService.handleInput(sessionId, input) }
            }
        }
    }
    
    given("ResizeTerminalUseCase") {
        val resizeTerminalUseCase = ResizeTerminalUseCase(mockTerminalSessionService)
        val sessionId = SessionId.generate()
        val newSize = TerminalSize(120, 40)
        
        `when`("执行调整终端尺寸用例") {
            coEvery { mockTerminalSessionService.resizeTerminal(sessionId, newSize) } returns Unit
            resizeTerminalUseCase.execute(sessionId, newSize)
            
            then("应该调用服务层调整终端尺寸") {
                coVerify { mockTerminalSessionService.resizeTerminal(sessionId, newSize) }
            }
        }
    }
    
    given("TerminateSessionUseCase") {
        val terminateSessionUseCase = TerminateSessionUseCase(mockTerminalSessionService)
        val sessionId = SessionId.generate()
        val reason = org.now.terminal.session.domain.valueobjects.TerminationReason.USER_REQUESTED
        
        `when`("执行终止会话用例") {
            coEvery { mockTerminalSessionService.terminateSession(sessionId, reason) } returns Unit
            terminateSessionUseCase.execute(sessionId, reason)
            
            then("应该调用服务层终止会话") {
                coVerify { mockTerminalSessionService.terminateSession(sessionId, reason) }
            }
        }
    }
    
    given("ListActiveSessionsUseCase") {
        val listActiveSessionsUseCase = ListActiveSessionsUseCase(mockTerminalSessionService)
        val userId = UserId.create("usr_123e4567-e89b-12d3-a456-426614174000")
        val mockSessions = listOf<TerminalSession>(mockk(), mockk())
        
        `when`("执行列出活跃会话用例") {
            coEvery { mockTerminalSessionService.listActiveSessions(userId) } returns mockSessions
            val result = listActiveSessionsUseCase.execute(userId)
            
            then("应该调用服务层并返回活跃会话列表") {
                coVerify { mockTerminalSessionService.listActiveSessions(userId) }
                result shouldBe mockSessions
            }
        }
    }
})
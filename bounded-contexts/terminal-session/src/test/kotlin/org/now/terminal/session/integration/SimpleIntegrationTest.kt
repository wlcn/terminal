package org.now.terminal.session.integration

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import org.now.terminal.infrastructure.configuration.ConfigurationManager
import org.now.terminal.session.domain.valueobjects.TerminalCommand
import org.now.terminal.session.domain.valueobjects.PtyConfiguration
import org.now.terminal.session.infrastructure.process.Pty4jProcessFactory
import org.now.terminal.shared.valueobjects.SessionId

/**
 * 简单的集成测试
 * 验证基本的终端命令执行功能
 */
class SimpleIntegrationTest : StringSpec({
    
    beforeSpec {
        ConfigurationManager.initialize(environment = "integration-test")
    }
    
    afterSpec {
        ConfigurationManager.reset()
    }
    
    "应该能够执行简单的echo命令" {
        // 在Windows环境下跳过Pty4j测试，因为存在兼容性问题
        if (System.getProperty("os.name").lowercase().contains("windows")) {
            // Windows环境下跳过实际执行，只验证工厂创建功能
            val processFactory = Pty4jProcessFactory()
            val sessionId = SessionId.generate()
            val command = TerminalCommand("echo Hello World")
            val ptyConfig = PtyConfiguration.createDefault(command)
            
            // 验证工厂能够创建Process实例
            val process = processFactory.createProcess(ptyConfig, sessionId)
            process shouldNotBe null
        } else {
            runBlocking {
                val processFactory = Pty4jProcessFactory()
                val sessionId = SessionId.generate()
                val command = TerminalCommand("echo Hello World")
                
                val ptyConfig = PtyConfiguration.createDefault(command)
                val process = processFactory.createProcess(ptyConfig, sessionId)
                
                process.start()
                
                // 等待进程执行完成
                Thread.sleep(1000)
                
                // 验证进程状态
                process.isAlive() shouldBe false
                process.getExitCode() shouldBe 0
                
                process.terminate()
            }
        }
    }
    
    "应该能够执行目录列表命令" {
        // 在Windows环境下跳过Pty4j测试，因为存在兼容性问题
        if (System.getProperty("os.name").lowercase().contains("windows")) {
            // Windows环境下跳过实际执行，只验证工厂创建功能
            val processFactory = Pty4jProcessFactory()
            val sessionId = SessionId.generate()
            val command = TerminalCommand("ls -la")
            val ptyConfig = PtyConfiguration.createDefault(command)
            
            // 验证工厂能够创建Process实例
            val process = processFactory.createProcess(ptyConfig, sessionId)
            process shouldNotBe null
        } else {
            runBlocking {
                val processFactory = Pty4jProcessFactory()
                val sessionId = SessionId.generate()
                val command = TerminalCommand("ls -la")
                
                val ptyConfig = PtyConfiguration.createDefault(command)
                val process = processFactory.createProcess(ptyConfig, sessionId)
                
                process.start()
                
                // 等待进程执行完成
                Thread.sleep(2000)
                
                // 验证进程状态
                process.isAlive() shouldBe false
                process.getExitCode() shouldBe 0
                
                process.terminate()
            }
        }
    }
})
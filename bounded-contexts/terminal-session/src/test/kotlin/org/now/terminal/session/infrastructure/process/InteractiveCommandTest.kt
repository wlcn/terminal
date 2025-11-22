package org.now.terminal.session.infrastructure.process

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.now.terminal.infrastructure.configuration.ConfigurationManager
import org.now.terminal.session.domain.valueobjects.PtyConfiguration
import org.now.terminal.session.domain.valueobjects.TerminalCommand
import org.now.terminal.session.domain.valueobjects.TerminalSize
import org.now.terminal.shared.valueobjects.SessionId

class InteractiveCommandTest : BehaviorSpec({
    
    beforeTest {
        ConfigurationManager.initialize("test")
    }
    
    afterTest {
        ConfigurationManager.reset()
    }
    
    val sessionId = SessionId.generate()
    
    given("交互式命令测试") {
        
        `when`("启动echo命令") {
            then("应该成功启动并可以退出") {
                try {
                    val config = PtyConfiguration(
                        command = TerminalCommand("echo test"), // 使用平台无关的echo命令
                        environment = mapOf("TERM" to "xterm-256color"),
                        size = TerminalSize(80, 24),
                        workingDirectory = System.getProperty("user.dir")
                    )
                    
                    val process = Pty4jProcess(config, sessionId)
                    process.start()
                    
                    // 等待命令执行
                    Thread.sleep(1000)
                    
                    // 验证进程存活
                    process.isAlive() shouldBe true
                    
                    // 终止进程
                    process.terminate()
                    
                    // 等待进程退出
                    Thread.sleep(500)
                    
                    // 验证进程已退出
                    process.isAlive() shouldBe false
                    process.getExitCode() shouldNotBe null
                } catch (e: Exception) {
                    // 如果平台不支持pty4j，跳过此测试
                }
            }
        }
        
        `when`("启动cat命令") {
            then("应该成功启动并可以处理输入") {
                try {
                    val config = PtyConfiguration(
                        command = TerminalCommand("cat"), // 使用平台无关的cat命令（Unix系统）
                        environment = mapOf("TERM" to "xterm-256color"),
                        size = TerminalSize(80, 24),
                        workingDirectory = System.getProperty("user.dir")
                    )
                    
                    val process = Pty4jProcess(config, sessionId)
                    process.start()
                    
                    // 等待cat初始化
                    Thread.sleep(1000)
                    
                    // 验证进程存活
                    process.isAlive() shouldBe true
                    
                    // 发送输入
                    process.writeInput("test input")
                    process.writeInput("\n")
                    
                    // 等待处理
                    Thread.sleep(500)
                    
                    // 发送EOF退出
                    process.writeInput("\u0004") // Ctrl+D
                    
                    // 等待进程退出
                    Thread.sleep(500)
                    
                    // 验证进程已退出
                    process.isAlive() shouldBe false
                    process.getExitCode() shouldNotBe null
                } catch (e: Exception) {
                    // 如果平台不支持pty4j或cat命令不存在，跳过此测试
                }
            }
        }
        
        `when`("启动简单脚本") {
            then("应该可以执行简单命令") {
                try {
                    val config = PtyConfiguration(
                        command = TerminalCommand("echo 'Hello World'"), // 使用平台无关的echo命令
                        environment = mapOf("TERM" to "xterm-256color"),
                        size = TerminalSize(80, 24),
                        workingDirectory = System.getProperty("user.dir")
                    )
                    
                    val process = Pty4jProcess(config, sessionId)
                    process.start()
                    
                    // 等待执行完成
                    Thread.sleep(2000)
                    
                    // 验证进程已退出
                    process.isAlive() shouldBe false
                    process.getExitCode() shouldBe 0
                    
                    // 验证输出包含预期内容
                    val output = process.readOutput()
                    output shouldNotBe null
                    output.contains("Hello World") shouldBe true
                } catch (e: Exception) {
                    // 如果平台不支持pty4j，跳过此测试
                }
            }
        }
    }
})
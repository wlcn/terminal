package org.now.terminal.session.infrastructure.process

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.now.terminal.session.domain.valueobjects.PtyConfiguration
import org.now.terminal.session.domain.valueobjects.TerminalSize
import org.now.terminal.shared.valueobjects.SessionId

class InteractiveCommandTest : BehaviorSpec({
    
    val sessionId = SessionId("test-session")
    
    given("交互式命令测试") {
        
        `when`("启动top命令") {
            then("应该成功启动并可以退出") {
                val config = PtyConfiguration(
                    command = "top -n 1", // 只运行一次
                    size = TerminalSize(80, 24),
                    workingDirectory = System.getProperty("user.dir")
                )
                
                val process = Pty4jProcess(config, sessionId)
                process.start()
                
                // 等待top初始化
                Thread.sleep(2000)
                
                // 验证进程存活
                process.isAlive() shouldBe true
                
                // 发送退出命令
                process.writeInput("q")
                
                // 等待进程退出
                Thread.sleep(1000)
                
                // 验证进程已退出
                process.isAlive() shouldBe false
                process.getExitCode() shouldNotBe null
            }
        }
        
        `when`("启动vim命令") {
            then("应该成功启动并可以强制退出") {
                val config = PtyConfiguration(
                    command = "vim --version", // 显示版本信息后退出
                    size = TerminalSize(80, 24),
                    workingDirectory = System.getProperty("user.dir")
                )
                
                val process = Pty4jProcess(config, sessionId)
                process.start()
                
                // 等待vim初始化
                Thread.sleep(1000)
                
                // 验证进程存活
                process.isAlive() shouldBe true
                
                // 强制退出vim
                process.writeInput(":q!\n")
                
                // 等待进程退出
                Thread.sleep(1000)
                
                // 验证进程已退出
                process.isAlive() shouldBe false
                process.getExitCode() shouldNotBe null
            }
        }
        
        `when`("启动交互式Python") {
            then("应该可以执行Python命令") {
                val config = PtyConfiguration(
                    command = "python -c \"print('Hello World'); exit()\"",
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
            }
        }
    }
})
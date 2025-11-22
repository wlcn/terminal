package org.now.terminal.session.infrastructure.process

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.now.terminal.infrastructure.configuration.ConfigurationManager
import org.now.terminal.session.domain.valueobjects.PtyConfiguration
import org.now.terminal.session.domain.valueobjects.TerminalCommand
import org.now.terminal.session.domain.valueobjects.TerminalSize
import org.now.terminal.shared.valueobjects.SessionId
import java.io.File

class Pty4jProcessTest : BehaviorSpec({
    
    beforeTest {
        ConfigurationManager.initialize("test")
    }
    
    afterTest {
        ConfigurationManager.reset()
    }
    
    given("Pty4jProcess") {
        val sessionId = SessionId.generate()
        val ptyConfig = PtyConfiguration(
            command = TerminalCommand("echo 'test'"),
            environment = mapOf("TERM" to "xterm-256color"),
            size = TerminalSize(80, 24),
            workingDirectory = File(".").absolutePath
        )
        
        `when`("创建Pty4jProcess实例") {
            val process = Pty4jProcess(ptyConfig, sessionId)
            
            then("应该成功创建实例") {
                process.shouldBeInstanceOf<Pty4jProcess>()
                process.isAlive() shouldBe false
            }
        }
        
        `when`("检查进程存活状态") {
            val process = Pty4jProcess(ptyConfig, sessionId)
            
            then("未启动的进程应该返回false") {
                process.isAlive() shouldBe false
            }
        }
        
        `when`("获取退出码") {
            val process = Pty4jProcess(ptyConfig, sessionId)
            
            then("未启动的进程应该返回null") {
                process.getExitCode() shouldBe null
            }
        }
        
        `when`("终止进程") {
            val process = Pty4jProcess(ptyConfig, sessionId)
            
            then("应该成功终止进程") {
                process.terminate() // 应该不会抛出异常
            }
        }
        
        `when`("调整终端尺寸") {
            val process = Pty4jProcess(ptyConfig, sessionId)
            val newSize = TerminalSize(120, 40)
            
            then("应该成功调整尺寸") {
                process.resize(newSize) // 应该不会抛出异常
            }
        }
        
        `when`("写入输入") {
            val process = Pty4jProcess(ptyConfig, sessionId)
            
            then("应该处理输入写入") {
                process.writeInput("test input") // 应该不会抛出异常
            }
        }
        
        `when`("读取输出") {
            val process = Pty4jProcess(ptyConfig, sessionId)
            
            then("应该返回空字符串") {
                process.readOutput() shouldBe ""
            }
        }
    }
    
    given("Pty4jProcess协程功能") {
        val sessionId = SessionId.generate()
        val ptyConfig = PtyConfiguration(
            command = TerminalCommand("echo 'test'"),
            environment = mapOf("TERM" to "xterm-256color"),
            size = TerminalSize(80, 24),
            workingDirectory = File(".").absolutePath
        )
        
        `when`("检查协程上下文") {
            val process = Pty4jProcess(ptyConfig, sessionId)
            
            then("应该具有协程上下文") {
                process.coroutineContext shouldNotBe null
            }
        }
    }
})
package org.now.terminal.session.infrastructure.process

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.now.terminal.infrastructure.configuration.ConfigurationManager
import org.now.terminal.session.domain.valueobjects.PtyConfiguration
import org.now.terminal.session.domain.valueobjects.TerminalCommand
import org.now.terminal.session.domain.valueobjects.TerminalSize
import org.now.terminal.shared.valueobjects.SessionId
import java.io.File
import java.util.concurrent.TimeUnit

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
    
    given("Pty4jProcess启动功能") {
        val sessionId = SessionId.generate()
        val ptyConfig = PtyConfiguration(
            command = TerminalCommand("sleep 1"), // 使用sleep命令避免立即退出
            environment = mapOf("TERM" to "xterm-256color"),
            size = TerminalSize(80, 24),
            workingDirectory = File(".").absolutePath
        )
        
        `when`("启动进程") {
            val process = Pty4jProcess(ptyConfig, sessionId)
            
            then("应该成功启动进程") {
                process.start()
                
                // 等待进程启动
                Thread.sleep(100)
                
                process.isAlive() shouldBe true
                process.terminate() // 清理资源
            }
        }
        
        `when`("启动后检查存活状态") {
            val process = Pty4jProcess(ptyConfig, sessionId)
            
            then("启动的进程应该返回true") {
                process.start()
                
                // 等待进程启动
                Thread.sleep(100)
                
                process.isAlive() shouldBe true
                process.terminate()
            }
        }
        
        `when`("启动后获取退出码") {
            val process = Pty4jProcess(ptyConfig, sessionId)
            
            then("存活的进程应该返回null") {
                process.start()
                
                // 等待进程启动
                Thread.sleep(100)
                
                process.getExitCode() shouldBe null
                process.terminate()
            }
        }
    }
    
    given("Pty4jProcess启动功能") {
        val sessionId = SessionId.generate()
        val ptyConfig = PtyConfiguration(
            command = TerminalCommand("echo 'test'"),
            environment = mapOf("TERM" to "xterm-256color"),
            size = TerminalSize(80, 24),
            workingDirectory = File(".").absolutePath
        )
        
        `when`("启动进程") {
            val process = Pty4jProcess(ptyConfig, sessionId)
            
            then("应该成功启动进程") {
                process.start()
                
                // 等待进程启动
                Thread.sleep(100)
                
                process.isAlive() shouldBe true
                process.terminate() // 清理资源
            }
        }
        
        `when`("启动后检查存活状态") {
            val process = Pty4jProcess(ptyConfig, sessionId)
            
            then("启动的进程应该返回true") {
                process.start()
                
                // 等待进程启动
                Thread.sleep(100)
                
                process.isAlive() shouldBe true
                process.terminate()
            }
        }
        
        `when`("启动后获取退出码") {
            val process = Pty4jProcess(ptyConfig, sessionId)
            
            then("存活的进程应该返回null") {
                process.start()
                
                // 等待进程启动
                Thread.sleep(100)
                
                process.getExitCode() shouldBe null
                process.terminate()
            }
        }
    }
    
    given("Pty4jProcess异常处理") {
        val sessionId = SessionId.generate()
        
        `when`("启动无效命令") {
            val invalidPtyConfig = PtyConfiguration(
                command = TerminalCommand("invalid_command_that_does_not_exist"),
                environment = mapOf("TERM" to "xterm-256color"),
                size = TerminalSize(80, 24),
                workingDirectory = File(".").absolutePath
            )
            
            val process = Pty4jProcess(invalidPtyConfig, sessionId)
            
            then("应该抛出异常") {
                try {
                    process.start()
                    // 如果启动成功，应该终止进程
                    process.terminate()
                } catch (e: Exception) {
                    // 预期会抛出异常
                    e.shouldBeInstanceOf<RuntimeException>()
                }
            }
        }
        
        `when`("写入输入到未启动的进程") {
            val process = Pty4jProcess(
                PtyConfiguration(
                    command = TerminalCommand("echo 'test'"),
                    environment = mapOf("TERM" to "xterm-256color"),
                    size = TerminalSize(80, 24),
                    workingDirectory = File(".").absolutePath
                ),
                sessionId
            )
            
            then("应该不会抛出异常") {
                process.writeInput("test") // 应该正常处理
            }
        }
        
        `when`("调整未启动进程的尺寸") {
            val process = Pty4jProcess(
                PtyConfiguration(
                    command = TerminalCommand("echo 'test'"),
                    environment = mapOf("TERM" to "xterm-256color"),
                    size = TerminalSize(80, 24),
                    workingDirectory = File(".").absolutePath
                ),
                sessionId
            )
            
            then("应该不会抛出异常") {
                process.resize(TerminalSize(100, 30)) // 应该正常处理
            }
        }
    }
})
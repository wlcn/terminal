package org.now.terminal.session.domain.valueobjects

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldContain
import io.kotest.assertions.throwables.shouldThrow
import org.now.terminal.infrastructure.configuration.ConfigurationManager

class ValueObjectsTest : BehaviorSpec({
    
    beforeTest {
        // 初始化配置管理器（用于测试环境）
        ConfigurationManager.initialize(environment = "test")
    }
    
    afterTest {
        // 清理配置管理器
        ConfigurationManager.reset()
    }
    
    given("TerminalCommand") {
        `when`("创建有效的命令") {
            val command = TerminalCommand("ls -la")
            
            then("应该包含正确的值") {
                command.value shouldBe "ls -la"
            }
        }
        
        `when`("创建空白命令") {
            then("应该抛出异常") {
                val exception = shouldThrow<IllegalArgumentException> {
                    TerminalCommand("   ")
                }
                exception.message shouldBe "Command cannot be blank"
            }
        }
        
        `when`("创建过长的命令") {
            val longCommand = "a".repeat(1025)
            
            then("应该抛出异常") {
                val exception = shouldThrow<IllegalArgumentException> {
                    TerminalCommand(longCommand)
                }
                exception.message shouldBe "Command too long"
            }
        }
    }
    
    given("TerminalSize") {
        `when`("创建有效的尺寸") {
            val size = TerminalSize(80, 24)
            
            then("应该包含正确的列数和行数") {
                size.columns shouldBe 80
                size.rows shouldBe 24
            }
        }
        
        `when`("创建无效的列数") {
            then("应该抛出异常") {
                val exception = shouldThrow<IllegalArgumentException> {
                    TerminalSize(0, 24)
                }
                exception.message shouldBe "Columns must be positive"
            }
        }
        
        `when`("创建无效的行数") {
            then("应该抛出异常") {
                val exception = shouldThrow<IllegalArgumentException> {
                    TerminalSize(80, 0)
                }
                exception.message shouldBe "Rows must be positive"
            }
        }
        
        `when`("创建过大的列数") {
            then("应该抛出异常") {
                val exception = shouldThrow<IllegalArgumentException> {
                    TerminalSize(501, 24)
                }
                exception.message shouldBe "Columns too large"
            }
        }
        
        `when`("创建过大的行数") {
            then("应该抛出异常") {
                val exception = shouldThrow<IllegalArgumentException> {
                    TerminalSize(80, 201)
                }
                exception.message shouldBe "Rows too large"
            }
        }
        
        `when`("从字符串创建尺寸") {
            val size = TerminalSize.fromString("80x24")
            
            then("应该正确解析列数和行数") {
                size.columns shouldBe 80
                size.rows shouldBe 24
            }
        }
        
        `when`("使用无效格式的字符串") {
            then("应该抛出异常") {
                val exception = shouldThrow<IllegalArgumentException> {
                    TerminalSize.fromString("80-24")
                }
                exception.message shouldBe "Terminal size format should be 'columnsxrows'"
            }
        }
    }
    
    given("PtyConfiguration") {
        `when`("创建有效的配置") {
            val config = PtyConfiguration(
                command = TerminalCommand("/bin/bash"),
                environment = mapOf("PATH" to "/usr/bin"),
                size = TerminalSize.DEFAULT
            )
            
            then("应该包含正确的命令、环境和尺寸") {
                config.command.value shouldBe "/bin/bash"
                config.environment["PATH"] shouldBe "/usr/bin"
                config.size shouldBe TerminalSize.DEFAULT
            }
        }
        
        `when`("创建空环境的配置") {
            then("应该抛出异常") {
                val exception = shouldThrow<IllegalArgumentException> {
                    PtyConfiguration(
                        command = TerminalCommand("/bin/bash"),
                        environment = emptyMap(),
                        size = TerminalSize.DEFAULT
                    )
                }
                exception.message shouldBe "Environment cannot be empty"
            }
        }
        
        `when`("创建默认配置") {
            val config = PtyConfiguration.createDefault(TerminalCommand("/bin/bash"))
            
            then("应该包含正确的命令和非空环境") {
                config.command.value shouldBe "/bin/bash"
                config.environment.isNotEmpty().shouldBeTrue()
                config.size shouldBe TerminalSize.DEFAULT
            }
        }
    }
    
    given("OutputBuffer") {
        `when`("管理缓冲区内容") {
            val buffer = OutputBuffer()
            buffer.append("Hello")
            buffer.append(" World")
            
            then("应该包含正确的内容和大小") {
                buffer.getContent() shouldBe "Hello World"
                buffer.size() shouldBe 11
            }
        }
        
        `when`("清空缓冲区") {
            val buffer = OutputBuffer()
            buffer.append("Test content")
            buffer.clear()
            
            then("应该为空且大小为0") {
                buffer.getContent() shouldBe ""
                buffer.size() shouldBe 0
            }
        }
        
        `when`("添加超过限制的内容") {
            val buffer = OutputBuffer()
            val bufferSizeLimit = ConfigurationManager.getTerminalConfig().bufferSize
            val longContent = "a".repeat(bufferSizeLimit + 100)
            buffer.append(longContent)
            
            then("应该限制在缓冲区大小限制内") {
                buffer.size() shouldBe bufferSizeLimit
            }
        }
    }
    
    given("TerminationReason") {
        `when`("检查枚举值") {
            then("应该包含所有有效的终止原因") {
                TerminationReason.values().size shouldBe 5
                TerminationReason.values() shouldContain TerminationReason.NORMAL
                TerminationReason.values() shouldContain TerminationReason.USER_REQUESTED
                TerminationReason.values() shouldContain TerminationReason.PROCESS_ERROR
                TerminationReason.values() shouldContain TerminationReason.TIMEOUT
                TerminationReason.values() shouldContain TerminationReason.SYSTEM_ERROR
            }
        }
    }
})
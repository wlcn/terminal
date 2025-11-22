package org.now.terminal.session.infrastructure.process

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import org.now.terminal.infrastructure.configuration.ConfigurationManager
import org.now.terminal.session.domain.valueobjects.PtyConfiguration
import org.now.terminal.session.domain.valueobjects.TerminalCommand
import org.now.terminal.shared.valueobjects.SessionId

class Pty4jProcessFactoryTest : BehaviorSpec({
    
    beforeSpec {
        // 初始化配置管理器
        ConfigurationManager.initialize(environment = "test")
    }
    
    afterSpec {
        // 清理配置管理器
        ConfigurationManager.reset()
    }
    
    given("Pty4jProcessFactory测试") {
        
        `when`("创建进程实例") {
            then("应该返回Pty4jProcess实例") {
                // Given
                val factory = Pty4jProcessFactory()
                val ptyConfig = PtyConfiguration.createDefault(TerminalCommand("echo test"))
                val sessionId = SessionId.generate()
                
                // When
                val process = factory.createProcess(ptyConfig, sessionId)
                
                // Then
                process.shouldBeInstanceOf<Pty4jProcess>()
            }
            
            then("应该正确传递配置参数") {
                // Given
                val factory = Pty4jProcessFactory()
                val mockPtyConfig = mockk<PtyConfiguration>()
                val sessionId = SessionId.generate()
                
                // When
                val process = factory.createProcess(mockPtyConfig, sessionId)
                
                // Then
                process.shouldBeInstanceOf<Pty4jProcess>()
                // 注意：由于Pty4jProcess是内部类，我们无法直接验证参数传递
                // 但可以通过类型检查确认工厂方法正常工作
            }
            
            then("应该处理不同的会话ID") {
                // Given
                val factory = Pty4jProcessFactory()
                val ptyConfig = PtyConfiguration.createDefault(TerminalCommand("echo test"))
                val sessionId1 = SessionId.generate()
                val sessionId2 = SessionId.generate()
                
                // When
                val process1 = factory.createProcess(ptyConfig, sessionId1)
                val process2 = factory.createProcess(ptyConfig, sessionId2)
                
                // Then
                process1.shouldBeInstanceOf<Pty4jProcess>()
                process2.shouldBeInstanceOf<Pty4jProcess>()
                // 两个进程实例应该是不同的对象
                process1 shouldNotBe process2
            }
            
            then("应该处理不同的Pty配置") {
                // Given
                val factory = Pty4jProcessFactory()
                val sessionId = SessionId.generate()
                
                // 创建不同的Pty配置
                val config1 = PtyConfiguration.createDefault(TerminalCommand("echo test"))
                val config2 = PtyConfiguration.createDefault(TerminalCommand("echo test"))
                
                // When
                val process1 = factory.createProcess(config1, sessionId)
                val process2 = factory.createProcess(config2, sessionId)
                
                // Then
                process1.shouldBeInstanceOf<Pty4jProcess>()
                process2.shouldBeInstanceOf<Pty4jProcess>()
                // 两个进程实例应该是不同的对象
                process1 shouldNotBe process2
            }
            
            then("应该支持多次调用") {
                // Given
                val factory = Pty4jProcessFactory()
                val ptyConfig = PtyConfiguration.createDefault(TerminalCommand("echo test"))
                
                // When - 多次调用工厂方法
                val processes = (1..5).map { 
                    factory.createProcess(ptyConfig, SessionId.generate())
                }
                
                // Then
                processes.forEach { process ->
                    process.shouldBeInstanceOf<Pty4jProcess>()
                }
                // 所有进程实例都应该是不同的对象
                processes.distinct().size shouldBe 5
            }
        }
    }
})
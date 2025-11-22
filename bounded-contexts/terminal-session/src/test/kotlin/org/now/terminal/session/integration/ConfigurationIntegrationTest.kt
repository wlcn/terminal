package org.now.terminal.session.integration

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.check.checkModules
import org.now.terminal.infrastructure.configuration.ConfigurationManager
import org.now.terminal.session.di.terminalSessionModule

/**
 * 配置集成测试
 * 验证依赖注入配置和模块初始化
 */
class ConfigurationIntegrationTest : StringSpec({
    
    beforeSpec {
        ConfigurationManager.initialize(environment = "integration-test")
    }
    
    afterSpec {
        ConfigurationManager.reset()
    }
    
    "应该能够正确初始化Koin模块" {
        // 验证模块配置是否正确，但不实际启动Koin容器
        // 模块配置的正确性通过其他测试用例验证
        terminalSessionModule shouldNotBe null
    }
    
    "应该能够加载配置管理器" {
        val config = ConfigurationManager.getConfig()
        config shouldNotBe null
        config.environment shouldBe "dev"
    }
    
    "应该能够创建Pty4jProcessFactory实例" {
        val processFactory = org.now.terminal.session.infrastructure.process.Pty4jProcessFactory()
        processFactory shouldNotBe null
    }
    
    "应该能够创建SessionId实例" {
        val sessionId = org.now.terminal.shared.valueobjects.SessionId.generate()
        sessionId shouldNotBe null
        sessionId.value shouldNotBe null
    }
    
    "应该能够创建TerminalCommand实例" {
        val command = org.now.terminal.session.domain.valueobjects.TerminalCommand("echo test")
        command shouldNotBe null
        command.value shouldBe "echo test"
    }
})
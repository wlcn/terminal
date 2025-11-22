package org.now.terminal.session.integration

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.now.terminal.infrastructure.configuration.ConfigurationManager

/**
 * 基础集成测试
 * 验证最基本的配置功能
 */
class BasicIntegrationTest : StringSpec({
    
    beforeSpec {
        ConfigurationManager.initialize(environment = "integration-test")
    }
    
    afterSpec {
        ConfigurationManager.reset()
    }
    
    "配置管理器应该能够初始化" {
        ConfigurationManager.isInitialized() shouldBe true
    }
    
    "应该能够获取应用程序名称" {
        val appName = ConfigurationManager.getAppName()
        appName shouldBe "kt-terminal"
    }
    
    "应该能够获取应用程序版本" {
        val appVersion = ConfigurationManager.getAppVersion()
        appVersion shouldBe "1.0.0"
    }
    
    "应该能够获取当前环境" {
        val environment = ConfigurationManager.getEnvironment()
        environment shouldBe "dev"
    }
})
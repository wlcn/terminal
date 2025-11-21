package org.now.terminal.infrastructure.configuration

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.File

class ConfigLoaderTest : BehaviorSpec({
    
    given("配置加载器") {
        
        `when`("加载默认配置") {
            then("应该成功加载默认配置") {
                val config = ConfigLoader.loadAppConfig()
                
                config shouldNotBe null
                config.name shouldBe "kt-terminal"
                config.version shouldBe "1.0.0"
                config.environment shouldBe "dev"
                config.server.port shouldBe 8080
                config.database.url shouldBe "jdbc:h2:mem:testdb"
            }
        }
        
        `when`("加载测试环境配置") {
            then("应该正确合并环境特定配置") {
                val config = ConfigLoader.loadAppConfig(environment = "test")
                
                config shouldNotBe null
                config.environment shouldBe "test"
                config.server.port shouldBe 8081  // 测试环境的端口
                config.database.url shouldBe "jdbc:h2:mem:testdb_test"
                config.logging.level shouldBe "DEBUG"
            }
        }
        
        `when`("加载不存在的环境配置") {
            then("应该回退到默认配置") {
                val config = ConfigLoader.loadAppConfig(environment = "nonexistent")
                
                config shouldNotBe null
                config.environment shouldBe "dev"  // 应该使用默认配置
                config.server.port shouldBe 8080
            }
        }
        
        `when`("验证配置完整性") {
            then("应该通过有效的配置验证") {
                val config = ConfigLoader.loadAppConfig()
                val isValid = ConfigLoader.validateConfig(config)
                
                isValid shouldBe true
            }
        }
        
        `when`("验证无效配置") {
            then("应该抛出配置异常") {
                val invalidConfig = AppConfig(
                    name = "",  // 空名称
                    version = "1.0.0",
                    environment = "dev",
                    server = ServerConfig(port = -1),  // 无效端口
                    database = DatabaseConfig(),
                    eventBus = EventBusConfig(),
                    logging = LoggingConfig(),
                    monitoring = MonitoringConfig()
                )
                
                val exception = kotlin.runCatching {
                    ConfigLoader.validateConfig(invalidConfig)
                }.exceptionOrNull()
                
                exception shouldNotBe null
                exception!!::class shouldBe ConfigurationException::class
            }
        }
    }
})
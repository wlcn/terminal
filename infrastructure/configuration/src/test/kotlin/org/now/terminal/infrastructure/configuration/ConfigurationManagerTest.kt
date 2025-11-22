package org.now.terminal.infrastructure.configuration

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ConfigurationManagerTest : BehaviorSpec({
    
    afterEach {
        // 在每个测试后重置配置管理器
        ConfigurationManager.reset()
    }
    
    given("配置管理器") {
        
        `when`("初始化配置管理器") {
            then("应该成功初始化并获取配置") {
                ConfigurationManager.initialize()
                
                val config = ConfigurationManager.getConfig()
                config shouldNotBe null
                config.name shouldBe "kt-terminal"
                ConfigurationManager.isInitialized() shouldBe true
            }
        }
        
        `when`("获取服务器配置") {
            then("应该返回正确的服务器配置") {
                ConfigurationManager.initialize()
                
                val serverConfig = ConfigurationManager.getServerConfig()
                serverConfig shouldNotBe null
                serverConfig.port shouldBe 8080
                serverConfig.host shouldBe "localhost"
            }
        }
        
        `when`("获取数据库配置") {
            then("应该返回正确的数据库配置") {
                ConfigurationManager.initialize()
                
                val dbConfig = ConfigurationManager.getDatabaseConfig()
                dbConfig shouldNotBe null
                dbConfig.url shouldBe "jdbc:h2:mem:testdb"
                dbConfig.username shouldBe "sa"
            }
        }
        
        `when`("获取事件总线配置") {
            then("应该返回正确的事件总线配置") {
                ConfigurationManager.initialize()
                
                val eventBusConfig = ConfigurationManager.getEventBusConfig()
                eventBusConfig shouldNotBe null
                eventBusConfig.bufferSize shouldBe 1000
                eventBusConfig.enableMetrics shouldBe true
            }
        }
        
        `when`("重新加载配置") {
            then("应该成功重新加载配置") {
                ConfigurationManager.initialize()
                
                val originalConfig = ConfigurationManager.getConfig()
                ConfigurationManager.reload()
                
                val newConfig = ConfigurationManager.getConfig()
                newConfig shouldNotBe null
                newConfig.name shouldBe originalConfig.name
            }
        }
        
        `when`("获取日志配置") {
            then("应该返回正确的日志配置") {
                ConfigurationManager.initialize()
                
                val loggingConfig = ConfigurationManager.getLoggingConfig()
                loggingConfig shouldNotBe null
                loggingConfig.level shouldBe "INFO"
                loggingConfig.file.enabled shouldBe false
            }
        }
        
        `when`("获取监控配置") {
            then("应该返回正确的监控配置") {
                ConfigurationManager.initialize()
                
                val monitoringConfig = ConfigurationManager.getMonitoringConfig()
                monitoringConfig shouldNotBe null
                monitoringConfig.enabled shouldBe true
                monitoringConfig.metrics.exportInterval shouldBe 60000
            }
        }
        
        `when`("重置配置管理器") {
            then("应该清除已初始化的配置") {
                ConfigurationManager.initialize()
                
                ConfigurationManager.reset()
                
                ConfigurationManager.isInitialized() shouldBe false
            }
        }
    }
})
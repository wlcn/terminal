# 配置模块 (Configuration Module)

配置模块为kt-terminal项目提供统一的配置管理功能，支持多种配置源和配置验证。

## 功能特性

- ✅ 多配置源支持（默认值、环境变量、配置文件）
- ✅ 配置验证和完整性检查
- ✅ 环境特定配置
- ✅ 热重载支持
- ✅ 类型安全的配置访问

## 核心类说明

### ConfigurationManager

全局配置管理器，提供配置的初始化和访问接口。

```kotlin
object ConfigurationManager {
    // 初始化配置管理器
    fun initialize(configPath: String? = null, environment: String? = null)
    
    // 获取当前配置
    fun getConfig(): AppConfig
    
    // 重新加载配置
    fun reload(configPath: String? = null, environment: String? = null)
    
    // 便捷配置访问方法
    fun getServerPort(): Int
    fun getDatabaseUrl(): String
    fun getLogLevel(): String
    fun isDevelopment(): Boolean
    // ... 更多便捷方法
}
```

### ConfigLoader

配置加载器，负责从不同源加载和合并配置。

```kotlin
object ConfigLoader {
    // 加载应用程序配置
    fun loadAppConfig(configPath: String? = null, environment: String? = null): AppConfig
    
    // 验证配置完整性
    fun validateConfig(config: AppConfig)
    
    // 合并配置（环境特定配置覆盖默认配置）
    private fun mergeConfigs(baseConfig: AppConfig, overrideConfig: AppConfig): AppConfig
}
```

### AppConfig

应用程序主配置类，包含所有配置节。

```kotlin
@Serializable
data class AppConfig(
    val name: String = "kt-terminal",
    val version: String = "1.0.0", 
    val environment: String = "dev",
    val server: ServerConfig = ServerConfig(),
    val database: DatabaseConfig = DatabaseConfig(),
    val eventBus: EventBusConfig = EventBusConfig(),
    val logging: LoggingConfig = LoggingConfig(),
    val monitoring: MonitoringConfig = MonitoringConfig()
)
```

## 使用示例

### 基本使用

```kotlin
import org.now.terminal.infrastructure.configuration.ConfigurationManager

// 1. 初始化配置管理器
ConfigurationManager.initialize()

// 2. 获取配置
val config = ConfigurationManager.getConfig()

// 3. 使用配置
println("应用名称: ${config.name}")
println("服务器端口: ${config.server.port}")
println("数据库URL: ${config.database.url}")
```

### 使用便捷方法

```kotlin
// 使用便捷方法获取配置
val serverPort = ConfigurationManager.getServerPort()
val dbUrl = ConfigurationManager.getDatabaseUrl() 
val logLevel = ConfigurationManager.getLogLevel()

// 环境检查
if (ConfigurationManager.isDevelopment()) {
    println("运行在开发环境")
}

if (ConfigurationManager.isProduction()) {
    println("运行在生产环境")
}
```

### 环境特定配置

```kotlin
// 加载测试环境配置
ConfigurationManager.initialize(environment = "test")

// 加载生产环境配置
ConfigurationManager.initialize(environment = "prod")

// 使用自定义配置文件
ConfigurationManager.initialize(
    configPath = "/path/to/config.yaml", 
    environment = "prod"
)
```

### 配置热重载

```kotlin
// 重新加载配置（适用于配置更新）
ConfigurationManager.reload()

// 重新加载特定环境的配置
ConfigurationManager.reload(environment = "prod")
```

## 配置结构详解

### ServerConfig (服务器配置)

```kotlin
@Serializable
data class ServerConfig(
    val port: Int = 8080,
    val host: String = "localhost", 
    val contextPath: String = "/api",
    val cors: CorsConfig = CorsConfig()
)

@Serializable
data class CorsConfig(
    val allowedOrigins: List<String> = listOf("*"),
    val allowedMethods: List<String> = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS"),
    val allowedHeaders: List<String> = listOf("*"),
    val allowCredentials: Boolean = true
)
```

### DatabaseConfig (数据库配置)

```kotlin
@Serializable
data class DatabaseConfig(
    val url: String = "jdbc:h2:mem:testdb",
    val driver: String = "org.h2.Driver",
    val username: String = "sa",
    val password: String = "",
    val poolSize: Int = 10,
    val connectionTimeout: Long = 30000,
    val maxLifetime: Long = 1800000
)
```

### EventBusConfig (事件总线配置)

```kotlin
@Serializable
data class EventBusConfig(
    val bufferSize: Int = 1000,
    val enableMetrics: Boolean = true,
    val enableDeadLetterQueue: Boolean = true,
    val deadLetterQueueCapacity: Int = 100,
    val maxRetries: Int = 3
)
```

### LoggingConfig (日志配置)

```kotlin
@Serializable
data class LoggingConfig(
    val level: String = "INFO",
    val file: LogFileConfig = LogFileConfig(),
    val format: String = "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
)

@Serializable
data class LogFileConfig(
    val enabled: Boolean = false,
    val path: String = "logs/application.log",
    val maxFileSize: String = "10MB",
    val maxHistory: Int = 30
)
```

### MonitoringConfig (监控配置)

```kotlin
@Serializable
data class MonitoringConfig(
    val enabled: Boolean = true,
    val metrics: MetricsConfig = MetricsConfig(),
    val health: HealthConfig = HealthConfig()
)

@Serializable
data class MetricsConfig(
    val enabled: Boolean = true,
    val exportInterval: Long = 60000,
    val endpoints: List<String> = listOf("jvm", "system", "process")
)

@Serializable
data class HealthConfig(
    val enabled: Boolean = true,
    val checkInterval: Long = 30000,
    val endpoints: List<String> = listOf("disk", "database", "memory")
)
```

## 配置源优先级

配置模块按照以下优先级加载配置：

1. **环境变量** (最高优先级)
2. **配置文件** 
3. **默认值** (最低优先级)

### 环境变量映射

环境变量名称遵循以下命名约定：

```bash
# 服务器配置
KT_SERVER_PORT=8080
KT_SERVER_HOST=localhost

# 数据库配置  
KT_DATABASE_URL=jdbc:postgresql://localhost:5432/mydb
KT_DATABASE_USERNAME=admin
KT_DATABASE_PASSWORD=secret

# 日志配置
KT_LOGGING_LEVEL=DEBUG
KT_LOGGING_FILE_ENABLED=true
```

### 配置文件支持

支持以下配置文件格式：

**YAML格式** (`config.yaml`):

```yaml
name: "kt-terminal"
version: "1.0.0"
environment: "prod"

server:
  port: 8080
  host: "0.0.0.0"
  cors:
    allowedOrigins: ["https://example.com"]

database:
  url: "jdbc:postgresql://localhost:5432/production"
  username: "prod_user"
  poolSize: 20
```

**JSON格式** (`config.json`):

```json
{
  "name": "kt-terminal",
  "version": "1.0.0",
  "environment": "prod",
  "server": {
    "port": 8080,
    "host": "0.0.0.0"
  }
}
```

## 最佳实践

### 1. 配置初始化

```kotlin
// 在应用程序启动时尽早初始化配置
fun main() {
    // 初始化配置
    ConfigurationManager.initialize()
    
    // 初始化其他基础设施
    TerminalLogger.initialize()
    
    // 启动应用程序
    startApplication()
}
```

### 2. 配置访问

```kotlin
// 推荐：使用便捷方法
val port = ConfigurationManager.getServerPort()

// 不推荐：直接访问配置对象（除非需要完整配置）
val config = ConfigurationManager.getConfig()
val port = config.server.port
```

### 3. 环境特定配置

```kotlin
// 根据运行环境加载不同配置
val environment = System.getenv("KT_ENVIRONMENT") ?: "dev"
ConfigurationManager.initialize(environment = environment)
```

### 4. 配置验证

配置模块会自动验证配置的完整性，但您也可以手动验证：

```kotlin
try {
    ConfigurationManager.initialize()
} catch (e: ConfigurationException) {
    logger.error("配置验证失败", e)
    // 处理配置错误
}
```

### 5. 安全配置

敏感信息（如密码）应该通过环境变量设置：

```kotlin
// 从环境变量读取敏感配置
val dbPassword = System.getenv("KT_DATABASE_PASSWORD") ?: ""
```

## 故障排除

### 常见问题

1. **配置加载失败**
   - 检查配置文件格式是否正确
   - 验证文件路径和权限
   - 检查环境变量名称

2. **配置验证失败**
   - 检查必填字段是否提供
   - 验证数据类型是否正确
   - 检查配置值的有效性

3. **配置不生效**
   - 确认配置初始化成功
   - 检查配置源优先级
   - 验证环境变量是否被正确读取

### 调试技巧

```kotlin
// 启用调试日志查看配置加载过程
ConfigurationManager.initialize()

// 检查当前生效的配置
val config = ConfigurationManager.getConfig()
println("当前环境: ${config.environment}")
println("服务器端口: ${config.server.port}")

// 重新加载配置（适用于配置更新）
ConfigurationManager.reload()
```

## 扩展配置

### 添加新的配置节

1. 在 `AppConfig.kt` 中添加新的配置类
2. 在 `AppConfig` 数据类中添加新字段
3. 在 `ConfigurationManager` 中添加便捷访问方法
4. 更新配置验证逻辑

### 自定义配置加载器

如果需要支持自定义配置源，可以扩展 `ConfigLoader`：

```kotlin
class CustomConfigLoader : ConfigLoader() {
    override fun loadAppConfig(configPath: String?, environment: String?): AppConfig {
        // 自定义配置加载逻辑
        val baseConfig = super.loadAppConfig(configPath, environment)
        // 应用自定义逻辑
        return baseConfig
    }
}
```

## 相关链接

- [基础设施层文档](../README.md)
- [日志模块文档](../logging/README.md)
- [事件总线模块文档](../event-bus/README.md)
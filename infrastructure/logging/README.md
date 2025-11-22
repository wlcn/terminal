# 日志模块 (Logging Module)

日志模块为kt-terminal项目提供统一的日志记录功能，基于SLF4J和Logback实现。

## 功能特性

- ✅ 统一的日志配置和输出格式
- ✅ 支持控制台和文件日志输出
- ✅ 日志级别动态配置
- ✅ 日志文件轮转和归档
- ✅ 全局日志工具类

## 核心类说明

### TerminalLogger

全局日志工具类，提供便捷的日志记录器获取方式。

```kotlin
object TerminalLogger {
    // 获取指定类的日志记录器
    fun getLogger(clazz: Class<*>): Logger
    
    // 获取指定名称的日志记录器
    fun getLogger(name: String): Logger
    
    // 初始化日志系统
    fun initialize()
    
    // 重新配置日志系统
    fun reconfigure()
}

// 扩展函数：为任意类提供日志记录器
inline fun <reified T> T.logger(): Logger

// 顶层日志函数：全局日志访问
val logger: Logger
```

### LoggingConfigurator

Logback日志配置器，负责配置日志系统和输出器。

```kotlin
object LoggingConfigurator {
    // 配置日志系统
    fun configure()
    
    // 重新配置日志系统
    fun reconfigure()
    
    // 创建控制台输出器
    private fun createConsoleAppender(): ConsoleAppender<ILoggingEvent>
    
    // 创建文件输出器
    private fun createFileAppender(): RollingFileAppender<ILoggingEvent>
}
```

## 使用示例

### 基本使用

```kotlin
import org.now.terminal.infrastructure.logging.TerminalLogger

// 1. 初始化日志系统
TerminalLogger.initialize()

// 2. 获取日志记录器
val logger = TerminalLogger.getLogger(MyClass::class.java)

// 3. 记录日志
logger.info("应用程序启动")
logger.debug("调试信息: {}", someValue)
logger.warn("警告信息")
logger.error("错误信息", exception)
```

### 使用扩展函数

```kotlin
import org.now.terminal.infrastructure.logging.logger

class MyService {
    // 使用扩展函数获取日志记录器
    private val logger = logger()
    
    fun processData(data: String) {
        logger.debug("开始处理数据: {}", data)
        
        try {
            // 业务逻辑
            logger.info("数据处理完成")
        } catch (e: Exception) {
            logger.error("数据处理失败", e)
            throw e
        }
    }
}
```

### 使用顶层日志函数

```kotlin
import org.now.terminal.infrastructure.logging.logger

// 在顶层函数或脚本中使用全局日志
fun main() {
    logger.info("应用程序启动")
    
    // 执行应用程序逻辑
    
    logger.info("应用程序正常退出")
}
```

### 动态日志级别调整

```kotlin
// 重新配置日志系统（例如：动态调整日志级别）
TerminalLogger.reconfigure()

// 在运行时可以修改日志配置，然后调用reconfigure()生效
```

## 日志配置详解

### 默认配置

日志模块使用以下默认配置：

- **日志级别**: INFO
- **输出格式**: `%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n`
- **控制台输出**: 启用
- **文件输出**: 禁用（可通过配置启用）

### 配置结构

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

### 日志级别

支持以下日志级别（从低到高）：

- `TRACE` - 最详细的日志信息
- `DEBUG` - 调试信息
- `INFO` - 一般信息
- `WARN` - 警告信息
- `ERROR` - 错误信息

### 输出格式说明

默认格式：`%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n`

- `%d{yyyy-MM-dd HH:mm:ss}` - 时间戳
- `[%thread]` - 线程名称
- `%-5level` - 日志级别（左对齐，宽度5）
- `%logger{36}` - 日志记录器名称（最大长度36）
- `%msg` - 日志消息
- `%n` - 换行符

## 配置方式

### 通过配置管理器

```kotlin
import org.now.terminal.infrastructure.configuration.ConfigurationManager

// 使用配置管理器设置日志配置
ConfigurationManager.initialize()

// 日志系统会自动使用配置管理器中的日志配置
TerminalLogger.initialize()
```

### 环境变量配置

```bash
# 设置日志级别
KT_LOGGING_LEVEL=DEBUG

# 启用文件日志
KT_LOGGING_FILE_ENABLED=true

# 设置日志文件路径
KT_LOGGING_FILE_PATH=/var/log/kt-terminal/app.log

# 设置日志文件最大大小
KT_LOGGING_FILE_MAX_FILE_SIZE=50MB

# 设置日志文件保留天数
KT_LOGGING_FILE_MAX_HISTORY=60
```

### 配置文件配置

**YAML格式** (`config.yaml`):

```yaml
logging:
  level: DEBUG
  format: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    enabled: true
    path: "logs/application.log"
    maxFileSize: "50MB"
    maxHistory: 60
```

**JSON格式** (`config.json`):

```json
{
  "logging": {
    "level": "DEBUG",
    "file": {
      "enabled": true,
      "path": "logs/application.log",
      "maxFileSize": "50MB",
      "maxHistory": 60
    }
  }
}
```

## 最佳实践

### 1. 日志初始化

```kotlin
// 在应用程序启动时尽早初始化日志系统
fun main() {
    // 先初始化配置
    ConfigurationManager.initialize()
    
    // 再初始化日志系统
    TerminalLogger.initialize()
    
    // 然后记录启动日志
    logger.info("应用程序启动成功")
    
    // 启动应用程序逻辑
    startApplication()
}
```

### 2. 日志级别选择

```kotlin
class UserService {
    private val logger = logger()
    
    fun createUser(user: User) {
        // TRACE: 非常详细的调试信息
        logger.trace("开始创建用户: {}", user)
        
        // DEBUG: 调试信息
        logger.debug("用户数据验证通过")
        
        // INFO: 一般业务信息
        logger.info("用户创建成功，ID: {}", user.id)
        
        // WARN: 警告信息（不影响业务）
        logger.warn("用户邮箱未验证")
        
        // ERROR: 错误信息（业务异常）
        logger.error("用户创建失败", exception)
    }
}
```

### 3. 日志消息格式

```kotlin
// 推荐：使用参数化日志
logger.info("用户 {} 登录成功", username)
logger.debug("处理请求 {} 耗时 {}ms", requestId, duration)

// 不推荐：字符串拼接
logger.info("用户 " + username + " 登录成功")

// 异常日志
try {
    riskyOperation()
} catch (e: Exception) {
    // 包含异常堆栈
    logger.error("操作失败", e)
    
    // 仅记录异常消息
    logger.error("操作失败: {}", e.message)
}
```

### 4. 性能考虑

```kotlin
// 使用延迟计算避免不必要的字符串构建
if (logger.isDebugEnabled()) {
    logger.debug("复杂计算: {}", expensiveCalculation())
}

// 或者使用lambda表达式（如果日志框架支持）
logger.debug { "复杂计算: ${expensiveCalculation()}" }
```

## 高级功能

### 自定义日志配置

如果需要自定义日志配置，可以扩展 `LoggingConfigurator`：

```kotlin
object CustomLoggingConfigurator {
    fun configureCustomAppender() {
        // 获取LoggerContext
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        
        // 创建自定义输出器
        val customAppender = createCustomAppender(loggerContext)
        
        // 配置根日志记录器
        val rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME)
        rootLogger.addAppender(customAppender)
        
        // 设置日志级别
        rootLogger.level = Level.toLevel("DEBUG")
    }
    
    private fun createCustomAppender(context: LoggerContext): Appender<ILoggingEvent> {
        // 实现自定义输出器逻辑
        // ...
    }
}
```

### 日志上下文（MDC）

使用Mapped Diagnostic Context (MDC) 添加上下文信息：

```kotlin
import org.slf4j.MDC

class RequestProcessor {
    private val logger = logger()
    
    fun processRequest(request: Request) {
        // 添加上下文信息
        MDC.put("requestId", request.id)
        MDC.put("userId", request.userId)
        
        try {
            logger.info("开始处理请求")
            // 处理逻辑
            logger.info("请求处理完成")
        } finally {
            // 清理上下文
            MDC.clear()
        }
    }
}
```

在日志格式中使用MDC信息：

```
%d{yyyy-MM-dd HH:mm:ss} [%thread] [%X{requestId}] %-5level %logger{36} - %msg%n
```

## 故障排除

### 常见问题

1. **日志不输出**
   - 检查日志级别设置
   - 验证日志配置是否正确加载
   - 检查文件路径权限

2. **日志文件不创建**
   - 确认文件日志已启用
   - 检查文件路径是否存在且可写
   - 验证磁盘空间

3. **日志格式不正确**
   - 检查日志格式字符串
   - 验证特殊字符转义

### 调试技巧

```kotlin
// 检查日志系统状态
val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger
logger.debug("根日志级别: ${rootLogger.level}")

// 检查所有输出器
rootLogger.iteratorForAppenders().forEach { appender ->
    logger.debug("输出器: ${appender.name}, 类: ${appender.javaClass.simpleName}")
}

// 临时启用调试日志
System.setProperty("logback.debug", "true")
TerminalLogger.reconfigure()
```

## 相关链接

- [基础设施层文档](../README.md)
- [配置模块文档](../configuration/README.md)
- [SLF4J官方文档](http://www.slf4j.org/)
- [Logback官方文档](http://logback.qos.ch/)
# 基础设施层 (Infrastructure Layer)

基础设施层为kt-terminal项目提供技术基础设施支持，包括配置管理、日志记录、事件总线、监控等功能。

## 模块概览

### 1. Configuration (配置管理)
- **位置**: `infrastructure/configuration/`
- **功能**: 统一的配置加载、验证和管理
- **核心类**: 
  - `ConfigurationManager` - 全局配置管理器
  - `ConfigLoader` - 配置加载器
  - `AppConfig` - 应用程序主配置类

### 2. Logging (日志记录)
- **位置**: `infrastructure/logging/`
- **功能**: 统一的日志配置和输出
- **核心类**:
  - `LoggingConfigurator` - Logback日志配置器
  - `TerminalLogger` - 全局日志工具类

### 3. Event Bus (事件总线)
- **位置**: `infrastructure/event-bus/`
- **功能**: 异步事件处理机制
- **核心类**:
  - `InMemoryEventBus` - 内存事件总线实现
  - `EventBusFactory` - 事件总线工厂
  - `EventBusConfig` - 事件总线配置

### 4. Monitoring (监控)
- **位置**: `infrastructure/monitoring/`
- **功能**: 应用程序监控和指标收集
- **核心类**: 待实现

## 快速开始

### 1. 初始化配置管理器

```kotlin
import org.now.terminal.infrastructure.configuration.ConfigurationManager

// 初始化配置管理器
ConfigurationManager.initialize()

// 获取配置
val config = ConfigurationManager.getConfig()
val serverPort = ConfigurationManager.getServerPort()
val logLevel = ConfigurationManager.getLogLevel()
```

### 2. 使用全局日志

```kotlin
import org.now.terminal.infrastructure.logging.TerminalLogger

// 初始化日志系统
TerminalLogger.initialize()

// 获取日志记录器
val logger = TerminalLogger.getLogger(MyClass::class.java)
logger.info("应用程序启动成功")

// 使用扩展函数
class MyService {
    private val logger = logger()
    
    fun doSomething() {
        logger.debug("执行操作")
    }
}
```

### 3. 使用事件总线

```kotlin
import org.now.terminal.infrastructure.eventbus.EventBusFactory
import org.now.terminal.shared.events.EventHandler

// 创建事件总线
val eventBus = EventBusFactory.createInMemoryEventBus()

// 启动事件总线
eventBus.start()

// 订阅事件
eventBus.subscribe(MyEvent::class.java, object : EventHandler<MyEvent> {
    override suspend fun handle(event: MyEvent) {
        // 处理事件
    }
    
    override fun canHandle(eventType: String): Boolean = eventType == "MyEvent"
})

// 发布事件
val event = MyEvent.create()
eventBus.publish(event)
```

## 配置说明

### 配置文件结构

应用程序支持多种配置方式：

1. **默认配置**: 使用代码中的默认值
2. **环境变量**: 通过环境变量覆盖配置
3. **配置文件**: 支持YAML、JSON格式的配置文件

### 配置优先级

1. 环境变量 (最高优先级)
2. 配置文件
3. 默认配置 (最低优先级)

### 环境特定配置

支持不同环境的配置：
- `dev` - 开发环境
- `test` - 测试环境  
- `prod` - 生产环境

## 最佳实践

### 1. 配置管理
- 在应用程序启动时尽早调用 `ConfigurationManager.initialize()`
- 使用便捷方法获取配置，避免直接访问配置对象
- 在生产环境中使用环境变量进行敏感配置

### 2. 日志记录
- 使用 `TerminalLogger` 获取日志记录器，确保统一的日志配置
- 在关键业务逻辑中添加适当的日志级别
- 避免在生产环境中使用 `DEBUG` 级别日志

### 3. 事件处理
- 事件处理器应该是无状态的
- 使用适当的重试机制处理失败的事件
- 监控事件总线的性能和错误率

## 开发指南

### 添加新的基础设施模块

1. 在 `infrastructure/` 目录下创建新的模块目录
2. 创建对应的 `build.gradle.kts` 文件
3. 实现模块的核心功能
4. 添加单元测试
5. 更新 `settings.gradle.kts` 包含新模块
6. 更新此文档说明新模块的使用方法

### 模块间依赖

基础设施模块可以相互依赖，但应避免循环依赖：
- Configuration 模块是基础模块，其他模块可以依赖它
- Logging 模块可以依赖 Configuration 模块
- Event Bus 模块可以依赖 Configuration 和 Logging 模块
- Monitoring 模块可以依赖所有其他基础设施模块

## 故障排除

### 常见问题

1. **配置加载失败**: 检查配置文件格式和环境变量设置
2. **日志不输出**: 检查日志级别配置和文件权限
3. **事件处理失败**: 检查事件处理器实现和重试配置

### 调试技巧

- 启用 `DEBUG` 级别日志查看详细执行过程
- 使用配置管理器的 `reload()` 方法重新加载配置
- 检查事件总线的死信队列处理失败的事件

## 相关文档

- [配置模块使用文档](./configuration/README.md)
- [日志模块使用文档](./logging/README.md) 
- [事件总线模块使用文档](./event-bus/README.md)
- [监控模块使用文档](./monitoring/README.md)
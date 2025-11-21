# 设计模式文档

## Event系统设计模式选择

### 当前实现：组合模式（优于密封类）

#### 为什么选择组合而非密封类？

**组合模式优势：**

1. **扩展性**：可以轻松添加新的事件类型，无需修改现有代码
2. **灵活性**：每个事件可以有不同的属性和行为，不受密封类限制
3. **职责分离**：EventHelper负责通用功能，具体事件负责业务逻辑
4. **接口契约**：通过Event接口确保所有事件的一致性
5. **测试友好**：可以独立测试EventHelper和具体事件

**密封类局限性：**

1. **扩展困难**：添加新事件类型需要修改密封类定义
2. **耦合度高**：所有事件类型必须在同一文件中定义
3. **业务逻辑混合**：不同类型的事件可能有完全不同的业务语义

### 具体实现对比

#### 组合模式实现
```kotlin
// 通用事件助手（组合）
data class EventHelper(
    val eventId: EventId = EventId.generate(),
    val occurredAt: Instant = Instant.now(),
    val eventType: String,
    val aggregateId: String? = null
)

// 事件接口
interface Event {
    val eventHelper: EventHelper
}

// 具体事件实现
class UserCreatedEvent(
    override val eventHelper: EventHelper,
    val userId: String,
    val userName: String
) : Event

class SystemHeartbeatEvent(
    override val eventHelper: EventHelper,
    val systemId: String,
    val status: SystemStatus
) : Event
```

#### 密封类实现（不推荐）
```kotlin
sealed class Event {
    abstract val eventId: EventId
    abstract val occurredAt: Instant
    
    data class UserCreatedEvent(
        override val eventId: EventId,
        override val occurredAt: Instant,
        val userId: String,
        val userName: String
    ) : Event()
    
    data class SystemHeartbeatEvent(
        override val eventId: EventId,
        override val occurredAt: Instant,
        val systemId: String,
        val status: SystemStatus
    ) : Event()
}
```

### CommandResult设计选择：密封类（优于组合）

#### 为什么选择密封类而非组合？

**密封类优势：**

1. **有限状态**：命令执行结果只有三种明确状态（成功/失败/超时）
2. **模式匹配**：编译时安全的when表达式
3. **状态完整性**：确保所有状态都被处理
4. **业务语义清晰**：每个状态都有明确含义

#### 具体实现
```kotlin
@Serializable
sealed class CommandResult {
    @Serializable
    data class Success(val output: String, val exitCode: Int) : CommandResult()
    
    @Serializable
    data class Failure(val error: String, val exitCode: Int) : CommandResult()
    
    @Serializable
    data class Timeout(val timeoutMs: Long) : CommandResult()
}
```

### 设计原则总结

| 设计模式 | 适用场景 | 当前项目应用 | 优势 |
|---------|---------|-------------|------|
| **组合模式** | 事件系统、可扩展的领域对象 | Event接口 + EventHelper | 扩展性强、职责分离 |
| **密封类** | 有限状态机、命令结果 | CommandResult | 模式匹配、状态完整性 |

### 类型安全最佳实践

#### 避免使用Any类型

**问题**：`Any`类型缺乏明确的业务语义，违反类型安全原则

**错误示例**：
```kotlin
// ❌ 不推荐 - 类型不明确
val customMetrics: Map<String, Any> = emptyMap()
```

**正确示例**：
```kotlin
// ✅ 推荐 - 明确的类型定义
val customMetrics: Map<String, String> = emptyMap()

// ✅ 或者使用密封类定义可能的类型
@Serializable
sealed class MetricValue {
    @Serializable data class StringValue(val value: String) : MetricValue()
    @Serializable data class NumberValue(val value: Double) : MetricValue()
    @Serializable data class BooleanValue(val value: Boolean) : MetricValue()
}

val customMetrics: Map<String, MetricValue> = emptyMap()
```

#### 类型安全的好处

1. **编译时检查**：编译器可以验证类型正确性
2. **业务语义清晰**：明确的类型表达业务意图
3. **序列化友好**：明确的类型更容易序列化
4. **重构安全**：类型变更时编译器会报错

### 最佳实践指南

1. **使用组合模式**：当需要支持多种不同类型且有不同业务语义的对象时
2. **使用密封类**：当对象状态有限且需要编译时安全的状态处理时
3. **避免继承**：优先选择组合而非继承，除非有明确的is-a关系
4. **接口契约**：通过接口定义行为契约，实现类关注具体实现
5. **类型安全**：避免使用`Any`类型，使用明确的类型定义

### 测试验证

- ✅ Event组合模式：支持不同类型事件的扩展
- ✅ CommandResult密封类：确保所有状态都被正确处理
- ✅ 编译时安全：两种设计都提供类型安全保证
- ✅ 业务逻辑正确：符合DDD和Kotlin最佳实践
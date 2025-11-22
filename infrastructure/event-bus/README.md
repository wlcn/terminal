# 事件总线模块 (Event Bus Module)

事件总线模块为kt-terminal项目提供基于内存的事件发布-订阅机制，支持异步事件处理和重试机制。

## 功能特性

- ✅ 内存事件总线实现
- ✅ 异步事件处理
- ✅ 事件重试机制
- ✅ 死信队列支持
- ✅ 事件监控和指标收集
- ✅ 配置驱动的事件总线创建

## 核心类说明

### EventBus

事件总线接口，定义事件发布和订阅的基本操作。

```kotlin
interface EventBus {
    // 发布事件
    suspend fun <T : Event> publish(event: T)
    
    // 订阅事件
    fun <T : Event> subscribe(
        eventType: KClass<T>,
        handler: suspend (T) -> Unit
    )
    
    // 取消订阅
    fun <T : Event> unsubscribe(eventType: KClass<T>, handler: suspend (T) -> Unit)
    
    // 关闭事件总线
    suspend fun close()
}
```

### InMemoryEventBus

内存事件总线实现，支持异步事件处理和重试机制。

```kotlin
class InMemoryEventBus(
    private val config: EventBusConfig = EventBusConfig(),
    private val retryHandler: EventRetryHandler = EventRetryHandler(),
    private val deadLetterQueue: DeadLetterQueue = DeadLetterQueue()
) : EventBus
```

### MonitoredEventBus

监控事件总线包装器，提供事件处理指标收集功能。

```kotlin
class MonitoredEventBus(
    private val delegate: EventBus,
    private val metrics: EventBusMetrics = EventBusMetrics()
) : EventBus by delegate
```

### EventBusFactory

事件总线工厂，提供便捷的事件总线创建方法。

```kotlin
object EventBusFactory {
    // 创建默认事件总线
    fun createDefault(): EventBus
    
    // 创建带配置的事件总线
    fun createWithConfig(config: EventBusConfig): EventBus
    
    // 创建监控事件总线
    fun createMonitored(delegate: EventBus): EventBus
}
```

### ConfiguredEventBusFactory

配置驱动的事件总线工厂，根据配置创建相应类型的事件总线。

```kotlin
object ConfiguredEventBusFactory {
    // 根据配置创建事件总线
    fun createEventBus(): EventBus
}
```

## 使用示例

### 基本使用

```kotlin
import org.now.terminal.infrastructure.eventbus.EventBus
import org.now.terminal.infrastructure.eventbus.EventBusFactory

// 1. 创建事件总线
val eventBus = EventBusFactory.createDefault()

// 2. 定义事件
class UserCreatedEvent(val userId: String, val username: String) : Event
class OrderPlacedEvent(val orderId: String, val amount: Double) : Event

// 3. 订阅事件
class UserEventHandler {
    init {
        eventBus.subscribe(UserCreatedEvent::class) { event ->
            logger.info("用户创建事件处理: ${event.userId}, ${event.username}")
            // 处理用户创建逻辑
        }
    }
}

class OrderEventHandler {
    init {
        eventBus.subscribe(OrderPlacedEvent::class) { event ->
            logger.info("订单创建事件处理: ${event.orderId}, ${event.amount}")
            // 处理订单逻辑
        }
    }
}

// 4. 发布事件
suspend fun main() {
    // 发布用户创建事件
    eventBus.publish(UserCreatedEvent("123", "john.doe"))
    
    // 发布订单创建事件
    eventBus.publish(OrderPlacedEvent("ORD-001", 99.99))
    
    // 等待事件处理完成
    delay(1000)
    
    // 关闭事件总线
    eventBus.close()
}
```

### 使用配置管理器

```kotlin
import org.now.terminal.infrastructure.configuration.ConfigurationManager
import org.now.terminal.infrastructure.eventbus.ConfiguredEventBusFactory

// 1. 初始化配置管理器
ConfigurationManager.initialize()

// 2. 创建配置驱动的事件总线
val eventBus = ConfiguredEventBusFactory.createEventBus()

// 3. 使用事件总线
class NotificationService {
    init {
        eventBus.subscribe(UserCreatedEvent::class) { event ->
            // 发送欢迎邮件
            sendWelcomeEmail(event.userId, event.username)
        }
    }
    
    private fun sendWelcomeEmail(userId: String, username: String) {
        // 发送邮件逻辑
        logger.info("发送欢迎邮件给用户: $username")
    }
}

// 4. 发布事件
suspend fun createUser() {
    // 创建用户逻辑...
    
    // 发布用户创建事件
    eventBus.publish(UserCreatedEvent("456", "jane.smith"))
}
```

### 事件重试机制

```kotlin
import org.now.terminal.infrastructure.eventbus.EventBusFactory
import org.now.terminal.infrastructure.eventbus.EventBusConfig

// 创建带重试配置的事件总线
val config = EventBusConfig(
    maxRetries = 3,
    retryDelay = 1000L // 1秒重试间隔
)

val eventBus = EventBusFactory.createWithConfig(config)

// 订阅可能失败的事件处理
class PaymentEventHandler {
    init {
        eventBus.subscribe(PaymentProcessedEvent::class) { event ->
            try {
                // 可能失败的外部API调用
                processPayment(event.paymentId)
                logger.info("支付处理成功: ${event.paymentId}")
            } catch (e: Exception) {
                logger.warn("支付处理失败，将进行重试: ${e.message}")
                throw e // 抛出异常触发重试机制
            }
        }
    }
    
    private fun processPayment(paymentId: String) {
        // 模拟可能失败的外部调用
        if (Random.nextBoolean()) {
            throw RuntimeException("外部支付服务暂时不可用")
        }
        // 支付处理逻辑
    }
}
```

### 监控事件总线

```kotlin
import org.now.terminal.infrastructure.eventbus.EventBusFactory

// 创建监控事件总线
val baseEventBus = EventBusFactory.createDefault()
val monitoredEventBus = EventBusFactory.createMonitored(baseEventBus)

// 使用监控事件总线
class AnalyticsService {
    init {
        monitoredEventBus.subscribe(UserActivityEvent::class) { event ->
            // 记录用户活动指标
            recordUserActivity(event)
        }
    }
}

// 获取事件总线指标
suspend fun printEventBusMetrics() {
    val metrics = // 从监控系统获取指标
    logger.info("事件发布总数: ${metrics.totalEventsPublished}")
    logger.info("事件处理总数: ${metrics.totalEventsProcessed}")
    logger.info("失败事件数: ${metrics.failedEvents}")
    logger.info("平均处理时间: ${metrics.averageProcessingTime}ms")
}
```

## 配置详解

### EventBusConfig

事件总线配置类：

```kotlin
@Serializable
data class EventBusConfig(
    val maxRetries: Int = 3,
    val retryDelay: Long = 1000L,
    val enableDeadLetterQueue: Boolean = true,
    val maxConcurrentHandlers: Int = 10
)
```

### 配置属性说明

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| maxRetries | Int | 3 | 事件处理失败时的最大重试次数 |
| retryDelay | Long | 1000 | 重试间隔时间（毫秒） |
| enableDeadLetterQueue | Boolean | true | 是否启用死信队列 |
| maxConcurrentHandlers | Int | 10 | 最大并发事件处理器数量 |

### 配置方式

**YAML格式** (`config.yaml`):

```yaml
eventBus:
  maxRetries: 5
  retryDelay: 2000
  enableDeadLetterQueue: true
  maxConcurrentHandlers: 20
```

**JSON格式** (`config.json`):

```json
{
  "eventBus": {
    "maxRetries": 5,
    "retryDelay": 2000,
    "enableDeadLetterQueue": true,
    "maxConcurrentHandlers": 20
  }
}
```

**环境变量**:

```bash
KT_EVENTBUS_MAXRETRIES=5
KT_EVENTBUS_RETRYDELAY=2000
KT_EVENTBUS_ENABLEDEADLETTERQUEUE=true
KT_EVENTBUS_MAXCONCURRENTHANDLERS=20
```

## 高级功能

### 死信队列 (Dead Letter Queue)

当事件处理失败且达到最大重试次数后，事件会被发送到死信队列：

```kotlin
import org.now.terminal.infrastructure.eventbus.DeadLetterQueue

class DeadLetterHandler {
    private val deadLetterQueue = DeadLetterQueue()
    
    init {
        // 处理死信队列中的事件
        deadLetterQueue.observe().collect { deadLetter ->
            logger.warn("处理死信事件: ${deadLetter.event}, 失败原因: ${deadLetter.failureReason}")
            
            // 记录到错误日志或发送警报
            logDeadLetterEvent(deadLetter)
        }
    }
    
    private fun logDeadLetterEvent(deadLetter: DeadLetter) {
        // 死信事件处理逻辑
    }
}
```

### 事件重试处理器 (EventRetryHandler)

自定义重试策略：

```kotlin
import org.now.terminal.infrastructure.eventbus.EventRetryHandler

class CustomRetryHandler : EventRetryHandler() {
    override suspend fun <T : Event> retry(
        event: T,
        handler: suspend (T) -> Unit,
        maxRetries: Int,
        retryDelay: Long
    ) {
        var attempt = 0
        while (attempt < maxRetries) {
            try {
                handler(event)
                return // 成功处理，退出重试循环
            } catch (e: Exception) {
                attempt++
                if (attempt >= maxRetries) {
                    throw e // 达到最大重试次数，抛出异常
                }
                
                // 自定义重试延迟策略
                val delay = calculateRetryDelay(attempt, retryDelay)
                delay(delay)
            }
        }
    }
    
    private fun calculateRetryDelay(attempt: Int, baseDelay: Long): Long {
        // 指数退避策略
        return baseDelay * (1L shl attempt)
    }
}
```

### 事件总线指标 (EventBusMetrics)

收集和监控事件总线性能指标：

```kotlin
import org.now.terminal.infrastructure.eventbus.EventBusMetrics

class EventBusMonitor {
    private val metrics = EventBusMetrics()
    
    fun printMetrics() {
        logger.info("=== 事件总线指标 ===")
        logger.info("总发布事件数: ${metrics.totalEventsPublished}")
        logger.info("总处理事件数: ${metrics.totalEventsProcessed}")
        logger.info("失败事件数: ${metrics.failedEvents}")
        logger.info("当前活跃处理器: ${metrics.activeHandlers}")
        logger.info("平均处理时间: ${metrics.averageProcessingTime}ms")
        logger.info("最大处理时间: ${metrics.maxProcessingTime}ms")
    }
    
    fun resetMetrics() {
        metrics.reset()
    }
}
```

## 最佳实践

### 1. 事件设计原则

```kotlin
// 好的事件设计：不可变、包含足够上下文
class OrderShippedEvent(
    val orderId: String,
    val shippingDate: Instant,
    val trackingNumber: String,
    val shippingAddress: Address
) : Event

// 不好的事件设计：可变、缺少上下文
class BadOrderEvent(
    var orderId: String,
    var status: String
) : Event
```

### 2. 事件处理器设计

```kotlin
class OrderEventHandler {
    private val logger = logger()
    
    init {
        eventBus.subscribe(OrderCreatedEvent::class) { event ->
            handleOrderCreated(event)
        }
    }
    
    private suspend fun handleOrderCreated(event: OrderCreatedEvent) {
        logger.info("处理订单创建事件: {}", event.orderId)
        
        try {
            // 每个处理器只负责一个职责
            updateInventory(event.orderItems)
            sendConfirmationEmail(event.customerEmail)
            notifyShippingDepartment(event)
            
            logger.info("订单创建事件处理完成: {}", event.orderId)
        } catch (e: Exception) {
            logger.error("订单创建事件处理失败: {}", event.orderId, e)
            throw e // 让重试机制处理
        }
    }
    
    private suspend fun updateInventory(orderItems: List<OrderItem>) {
        // 库存更新逻辑
    }
    
    private suspend fun sendConfirmationEmail(email: String) {
        // 邮件发送逻辑
    }
    
    private suspend fun notifyShippingDepartment(event: OrderCreatedEvent) {
        // 通知发货部门逻辑
    }
}
```

### 3. 错误处理和重试策略

```kotlin
class PaymentEventHandler {
    init {
        eventBus.subscribe(PaymentFailedEvent::class) { event ->
            handlePaymentFailed(event)
        }
    }
    
    private suspend fun handlePaymentFailed(event: PaymentFailedEvent) {
        // 检查是否应该重试
        if (shouldRetryPayment(event)) {
            // 重试支付逻辑
            retryPayment(event)
        } else {
            // 标记为最终失败
            markPaymentAsFailed(event)
            
            // 发送失败通知
            sendFailureNotification(event)
        }
    }
    
    private fun shouldRetryPayment(event: PaymentFailedEvent): Boolean {
        // 基于错误类型和重试次数决定是否重试
        return event.retryCount < 3 && 
               event.errorType != ErrorType.INVALID_CARD
    }
}
```

### 4. 性能优化

```kotlin
// 使用协程优化并发处理
class OptimizedEventHandler {
    init {
        eventBus.subscribe(DataProcessingEvent::class) { event ->
            // 使用协程上下文优化
            withContext(Dispatchers.IO) {
                processLargeData(event.data)
            }
        }
    }
    
    private suspend fun processLargeData(data: ByteArray) {
        // 分批处理大数据
        data.chunked(1024 * 1024) { chunk ->
            // 并行处理每个块
            processChunk(chunk)
        }
    }
    
    private suspend fun processChunk(chunk: ByteArray) {
        // 处理数据块逻辑
    }
}
```

## 故障排除

### 常见问题

1. **事件不处理**
   - 检查事件订阅是否正确注册
   - 验证事件类型匹配
   - 检查事件处理器是否抛出异常

2. **内存泄漏**
   - 确保及时取消订阅
   - 检查事件处理器生命周期
   - 使用弱引用或适当的清理机制

3. **性能问题**
   - 监控事件处理时间
   - 优化事件处理器逻辑
   - 考虑使用批量事件处理

### 调试技巧

```kotlin
// 启用详细日志
class DebugEventBus(config: EventBusConfig) : InMemoryEventBus(config) {
    override suspend fun <T : Event> publish(event: T) {
        logger.debug("发布事件: ${event::class.simpleName}")
        super.publish(event)
    }
    
    override fun <T : Event> subscribe(eventType: KClass<T>, handler: suspend (T) -> Unit) {
        logger.debug("订阅事件: ${eventType.simpleName}")
        super.subscribe(eventType, handler)
    }
}

// 创建调试事件总线
val debugEventBus = DebugEventBus()
```

## 相关链接

- [基础设施层文档](../README.md)
- [配置模块文档](../configuration/README.md)
- [监控模块文档](../monitoring/README.md)
- [Kotlin协程文档](https://kotlinlang.org/docs/coroutines-guide.html)
# 监控模块 (Monitoring Module)

监控模块为kt-terminal项目提供应用程序监控功能，包括指标收集、健康检查和性能监控。

## 功能特性

- ✅ 应用程序指标收集和导出
- ✅ 健康检查端点
- ✅ 性能监控和追踪
- ✅ 配置驱动的监控设置
- ✅ 与事件总线集成

## 核心类说明

### MonitoringManager

监控管理器，负责初始化和管理监控系统。

```kotlin
object MonitoringManager {
    // 初始化监控系统
    fun initialize()
    
    // 获取指标收集器
    fun getMetricsCollector(): MetricsCollector
    
    // 获取健康检查注册器
    fun getHealthCheckRegistry(): HealthCheckRegistry
    
    // 启动监控端点
    fun startMonitoringEndpoints()
    
    // 停止监控系统
    fun stop()
}
```

### MetricsCollector

指标收集器，负责收集和导出应用程序指标。

```kotlin
class MetricsCollector {
    // 记录计数器指标
    fun incrementCounter(name: String, labels: Map<String, String> = emptyMap())
    
    // 记录计时器指标
    fun recordTimer(name: String, duration: Long, labels: Map<String, String> = emptyMap())
    
    // 记录仪表指标
    fun setGauge(name: String, value: Double, labels: Map<String, String> = emptyMap())
    
    // 获取所有指标
    fun getMetrics(): Map<String, Any>
    
    // 导出指标为Prometheus格式
    fun exportPrometheusMetrics(): String
}
```

### HealthCheckRegistry

健康检查注册器，管理应用程序的健康检查。

```kotlin
class HealthCheckRegistry {
    // 注册健康检查
    fun register(name: String, check: HealthCheck)
    
    // 取消注册健康检查
    fun unregister(name: String)
    
    // 执行所有健康检查
    fun runHealthChecks(): Map<String, HealthCheckResult>
    
    // 检查整体健康状态
    fun isHealthy(): Boolean
}
```

### HealthCheck

健康检查接口，定义健康检查逻辑。

```kotlin
interface HealthCheck {
    // 执行健康检查
    fun check(): HealthCheckResult
}
```

### PerformanceMonitor

性能监控器，监控应用程序性能指标。

```kotlin
class PerformanceMonitor {
    // 开始性能追踪
    fun startTrace(name: String): TraceContext
    
    // 结束性能追踪
    fun endTrace(context: TraceContext)
    
    // 记录方法执行时间
    fun <T> measureExecutionTime(name: String, block: () -> T): T
    
    // 获取性能指标
    fun getPerformanceMetrics(): PerformanceMetrics
}
```

## 使用示例

### 基本使用

```kotlin
import org.now.terminal.infrastructure.monitoring.MonitoringManager

// 1. 初始化监控系统
MonitoringManager.initialize()

// 2. 获取指标收集器
val metrics = MonitoringManager.getMetricsCollector()

// 3. 记录业务指标
class OrderService {
    fun processOrder(order: Order) {
        // 记录订单处理指标
        metrics.incrementCounter("orders_processed", 
            mapOf("status" to "success"))
        
        // 记录处理时间
        val startTime = System.currentTimeMillis()
        
        try {
            // 业务逻辑
            processOrderLogic(order)
            
            val duration = System.currentTimeMillis() - startTime
            metrics.recordTimer("order_processing_time", duration)
            
        } catch (e: Exception) {
            metrics.incrementCounter("orders_processed", 
                mapOf("status" to "failed"))
            throw e
        }
    }
}

// 4. 启动监控端点（如果配置启用）
MonitoringManager.startMonitoringEndpoints()
```

### 健康检查使用

```kotlin
import org.now.terminal.infrastructure.monitoring.MonitoringManager
import org.now.terminal.infrastructure.monitoring.HealthCheck
import org.now.terminal.infrastructure.monitoring.HealthCheckResult

// 1. 创建自定义健康检查
class DatabaseHealthCheck : HealthCheck {
    override fun check(): HealthCheckResult {
        return try {
            // 检查数据库连接
            val isHealthy = checkDatabaseConnection()
            
            if (isHealthy) {
                HealthCheckResult.healthy("数据库连接正常")
            } else {
                HealthCheckResult.unhealthy("数据库连接失败")
            }
        } catch (e: Exception) {
            HealthCheckResult.unhealthy("数据库健康检查异常: ${e.message}")
        }
    }
    
    private fun checkDatabaseConnection(): Boolean {
        // 实现数据库连接检查逻辑
        return true
    }
}

class ExternalServiceHealthCheck : HealthCheck {
    override fun check(): HealthCheckResult {
        return try {
            // 检查外部服务可用性
            val isAvailable = checkExternalService()
            
            if (isAvailable) {
                HealthCheckResult.healthy("外部服务可用")
            } else {
                HealthCheckResult.unhealthy("外部服务不可用")
            }
        } catch (e: Exception) {
            HealthCheckResult.unhealthy("外部服务检查异常: ${e.message}")
        }
    }
    
    private fun checkExternalService(): Boolean {
        // 实现外部服务检查逻辑
        return true
    }
}

// 2. 注册健康检查
val healthRegistry = MonitoringManager.getHealthCheckRegistry()
healthRegistry.register("database", DatabaseHealthCheck())
healthRegistry.register("external-service", ExternalServiceHealthCheck())

// 3. 执行健康检查
fun checkApplicationHealth(): Boolean {
    val results = healthRegistry.runHealthChecks()
    
    results.forEach { (name, result) ->
        if (result.isHealthy) {
            println("✅ $name: ${result.message}")
        } else {
            println("❌ $name: ${result.message}")
        }
    }
    
    return healthRegistry.isHealthy()
}
```

### 性能监控使用

```kotlin
import org.now.terminal.infrastructure.monitoring.PerformanceMonitor

class PerformanceAwareService {
    private val performanceMonitor = PerformanceMonitor()
    
    fun processData(data: List<String>): List<String> {
        // 使用方法执行时间监控
        return performanceMonitor.measureExecutionTime("data_processing") {
            data.map { processItem(it) }
        }
    }
    
    suspend fun complexOperation(input: String): String {
        // 使用追踪上下文进行详细性能监控
        val trace = performanceMonitor.startTrace("complex_operation")
        
        try {
            // 步骤1
            val step1Result = performanceMonitor.measureExecutionTime("step1") {
                performStep1(input)
            }
            
            // 步骤2
            val step2Result = performanceMonitor.measureExecutionTime("step2") {
                performStep2(step1Result)
            }
            
            return step2Result
        } finally {
            performanceMonitor.endTrace(trace)
        }
    }
    
    private fun processItem(item: String): String {
        // 处理单个项目
        return item.uppercase()
    }
    
    private fun performStep1(input: String): String {
        // 步骤1逻辑
        return "step1_$input"
    }
    
    private fun performStep2(input: String): String {
        // 步骤2逻辑
        return "step2_$input"
    }
}

// 获取性能指标
fun printPerformanceMetrics() {
    val metrics = PerformanceMonitor().getPerformanceMetrics()
    
    println("=== 性能指标 ===")
    println("平均响应时间: ${metrics.averageResponseTime}ms")
    println("最大响应时间: ${metrics.maxResponseTime}ms")
    println("请求总数: ${metrics.totalRequests}")
    println("错误率: ${metrics.errorRate}%")
}
```

### 与事件总线集成

```kotlin
import org.now.terminal.infrastructure.monitoring.MonitoringManager
import org.now.terminal.infrastructure.eventbus.EventBus
import org.now.terminal.infrastructure.eventbus.EventBusFactory

class MonitoredEventService {
    private val eventBus = EventBusFactory.createDefault()
    private val metrics = MonitoringManager.getMetricsCollector()
    
    init {
        // 订阅事件并记录指标
        eventBus.subscribe(UserActivityEvent::class) { event ->
            val trace = PerformanceMonitor().startTrace("user_activity_processing")
            
            try {
                metrics.incrementCounter("user_activities")
                
                // 处理用户活动
                processUserActivity(event)
                
                metrics.incrementCounter("user_activities", 
                    mapOf("status" to "success"))
            } catch (e: Exception) {
                metrics.incrementCounter("user_activities", 
                    mapOf("status" to "failed"))
                throw e
            } finally {
                PerformanceMonitor().endTrace(trace)
            }
        }
    }
    
    private fun processUserActivity(event: UserActivityEvent) {
        // 用户活动处理逻辑
    }
}
```

## 配置详解

### MonitoringConfig

监控配置类：

```kotlin
@Serializable
data class MonitoringConfig(
    val enabled: Boolean = false,
    val metrics: MetricsConfig = MetricsConfig(),
    val health: HealthConfig = HealthConfig()
)

@Serializable
data class MetricsConfig(
    val enabled: Boolean = false,
    val exportInterval: Long = 30000L, // 30秒
    val endpoints: List<String> = emptyList()
)

@Serializable
data class HealthConfig(
    val enabled: Boolean = false,
    val checkInterval: Long = 60000L, // 60秒
    val endpoints: List<String> = emptyList()
)
```

### 配置属性说明

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| enabled | Boolean | false | 是否启用监控 |
| metrics.enabled | Boolean | false | 是否启用指标收集 |
| metrics.exportInterval | Long | 30000 | 指标导出间隔（毫秒） |
| metrics.endpoints | List<String> | [] | 指标导出端点 |
| health.enabled | Boolean | false | 是否启用健康检查 |
| health.checkInterval | Long | 60000 | 健康检查间隔（毫秒） |
| health.endpoints | List<String> | [] | 健康检查端点 |

### 配置方式

**YAML格式** (`config.yaml`):

```yaml
monitoring:
  enabled: true
  metrics:
    enabled: true
    exportInterval: 15000
    endpoints:
      - "/metrics"
      - "/prometheus"
  health:
    enabled: true
    checkInterval: 30000
    endpoints:
      - "/health"
      - "/health/ready"
      - "/health/live"
```

**JSON格式** (`config.json`):

```json
{
  "monitoring": {
    "enabled": true,
    "metrics": {
      "enabled": true,
      "exportInterval": 15000,
      "endpoints": ["/metrics", "/prometheus"]
    },
    "health": {
      "enabled": true,
      "checkInterval": 30000,
      "endpoints": ["/health", "/health/ready", "/health/live"]
    }
  }
}
```

**环境变量**:

```bash
KT_MONITORING_ENABLED=true
KT_MONITORING_METRICS_ENABLED=true
KT_MONITORING_METRICS_EXPORTINTERVAL=15000
KT_MONITORING_METRICS_ENDPOINTS=/metrics,/prometheus
KT_MONITORING_HEALTH_ENABLED=true
KT_MONITORING_HEALTH_CHECKINTERVAL=30000
KT_MONITORING_HEALTH_ENDPOINTS=/health,/health/ready,/health/live
```

## 高级功能

### 自定义指标收集器

```kotlin
import org.now.terminal.infrastructure.monitoring.MetricsCollector

class CustomMetricsCollector : MetricsCollector() {
    private val customMetrics = mutableMapOf<String, Any>()
    
    // 添加自定义指标类型
    fun recordHistogram(name: String, value: Double, labels: Map<String, String> = emptyMap()) {
        val key = buildMetricKey(name, labels)
        // 实现直方图逻辑
        updateHistogram(key, value)
    }
    
    fun recordSummary(name: String, value: Double, labels: Map<String, String> = emptyMap()) {
        val key = buildMetricKey(name, labels)
        // 实现摘要逻辑
        updateSummary(key, value)
    }
    
    override fun getMetrics(): Map<String, Any> {
        val baseMetrics = super.getMetrics()
        return baseMetrics + customMetrics
    }
    
    private fun buildMetricKey(name: String, labels: Map<String, String>): String {
        val labelStr = labels.entries.joinToString(",") { "${it.key}=\"${it.value}\"" }
        return if (labelStr.isNotEmpty()) "$name{$labelStr}" else name
    }
}
```

### 分布式追踪集成

```kotlin
import org.now.terminal.infrastructure.monitoring.PerformanceMonitor

class DistributedTracingMonitor : PerformanceMonitor() {
    private val tracer = // 初始化分布式追踪器
    
    override fun startTrace(name: String): TraceContext {
        val span = tracer.buildSpan(name).start()
        return DistributedTraceContext(span)
    }
    
    override fun endTrace(context: TraceContext) {
        if (context is DistributedTraceContext) {
            context.span.finish()
        }
    }
    
    // 添加追踪标签
    fun addTraceTag(context: TraceContext, key: String, value: String) {
        if (context is DistributedTraceContext) {
            context.span.setTag(key, value)
        }
    }
    
    // 记录追踪日志
    fun logTraceEvent(context: TraceContext, event: String) {
        if (context is DistributedTraceContext) {
            context.span.log(event)
        }
    }
}

class DistributedTraceContext(val span: Span) : TraceContext
```

### 监控数据导出

```kotlin
import org.now.terminal.infrastructure.monitoring.MonitoringManager

class MetricsExporter {
    private val metrics = MonitoringManager.getMetricsCollector()
    
    // 导出为Prometheus格式
    fun exportPrometheus(): String {
        return metrics.exportPrometheusMetrics()
    }
    
    // 导出为JSON格式
    fun exportJson(): String {
        val metricsData = metrics.getMetrics()
        return Json.encodeToString(metricsData)
    }
    
    // 导出到外部监控系统
    suspend fun exportToExternalSystem(endpoint: String) {
        val metricsData = metrics.getMetrics()
        
        // 使用HTTP客户端发送指标数据
        httpClient.post(endpoint) {
            contentType(ContentType.Application.Json)
            setBody(metricsData)
        }
    }
}

class HealthExporter {
    private val healthRegistry = MonitoringManager.getHealthCheckRegistry()
    
    // 导出健康状态
    fun exportHealthStatus(): Map<String, Any> {
        val results = healthRegistry.runHealthChecks()
        
        return mapOf(
            "status" to if (healthRegistry.isHealthy()) "healthy" else "unhealthy",
            "timestamp" to System.currentTimeMillis(),
            "checks" to results.mapValues { (_, result) ->
                mapOf(
                    "status" to if (result.isHealthy) "healthy" else "unhealthy",
                    "message" to result.message
                )
            }
        )
    }
}
```

## 最佳实践

### 1. 指标命名规范

```kotlin
class MetricNamingExample {
    private val metrics = MonitoringManager.getMetricsCollector()
    
    fun recordBusinessMetrics() {
        // 好的命名：使用下划线分隔，描述性名称
        metrics.incrementCounter("orders_created_total")
        metrics.recordTimer("order_processing_duration_seconds", duration)
        metrics.setGauge("active_users_count", activeUsers.toDouble())
        
        // 不好的命名：驼峰命名，不清晰
        metrics.incrementCounter("ordersCreated") // 不推荐
        metrics.recordTimer("orderProcessingTime", duration) // 不推荐
    }
    
    fun recordMetricsWithLabels() {
        // 使用标签区分不同维度
        metrics.incrementCounter("http_requests_total", 
            mapOf(
                "method" to "POST",
                "path" to "/api/orders",
                "status" to "200"
            ))
        
        // 记录错误指标
        metrics.incrementCounter("http_requests_total", 
            mapOf(
                "method" to "POST", 
                "path" to "/api/orders",
                "status" to "500"
            ))
    }
}
```

### 2. 健康检查设计

```kotlin
class ComprehensiveHealthCheck : HealthCheck {
    override fun check(): HealthCheckResult {
        val results = mutableListOf<HealthCheckResult>()
        
        // 检查数据库连接
        results.add(checkDatabase())
        
        // 检查外部服务
        results.add(checkExternalServices())
        
        // 检查磁盘空间
        results.add(checkDiskSpace())
        
        // 检查内存使用
        results.add(checkMemoryUsage())
        
        // 汇总结果
        val unhealthyResults = results.filter { !it.isHealthy }
        
        return if (unhealthyResults.isEmpty()) {
            HealthCheckResult.healthy("所有健康检查通过")
        } else {
            val errorMessages = unhealthyResults.joinToString(", ") { it.message }
            HealthCheckResult.unhealthy("健康检查失败: $errorMessages")
        }
    }
    
    private fun checkDatabase(): HealthCheckResult {
        return try {
            // 数据库连接检查逻辑
            HealthCheckResult.healthy("数据库连接正常")
        } catch (e: Exception) {
            HealthCheckResult.unhealthy("数据库连接失败: ${e.message}")
        }
    }
    
    private fun checkExternalServices(): HealthCheckResult {
        // 类似实现其他检查
        return HealthCheckResult.healthy("外部服务正常")
    }
    
    // 其他检查方法...
}
```

### 3. 性能监控策略

```kotlin
class PerformanceOptimizedService {
    private val performanceMonitor = PerformanceMonitor()
    
    // 异步性能监控
    suspend fun processWithAsyncMonitoring(data: List<String>): List<String> {
        val trace = performanceMonitor.startTrace("async_data_processing")
        
        return try {
            // 使用协程异步处理
            data.map { item ->
                async {
                    performanceMonitor.measureExecutionTime("item_processing") {
                        processItemAsync(item)
                    }
                }
            }.awaitAll()
        } finally {
            performanceMonitor.endTrace(trace)
        }
    }
    
    // 批量处理优化
    fun processBatchWithMonitoring(batch: List<BatchItem>): BatchResult {
        return performanceMonitor.measureExecutionTime("batch_processing") {
            val results = batch.map { processSingleItem(it) }
            
            // 记录批量处理指标
            MonitoringManager.getMetricsCollector().setGauge(
                "batch_size", 
                batch.size.toDouble()
            )
            
            BatchResult(results)
        }
    }
    
    private suspend fun processItemAsync(item: String): String {
        delay(10) // 模拟异步处理
        return item.uppercase()
    }
    
    private fun processSingleItem(item: BatchItem): ProcessedItem {
        // 单个项目处理逻辑
        return ProcessedItem(item.id, "processed")
    }
}
```

## 故障排除

### 常见问题

1. **指标不显示**
   - 检查监控是否启用
   - 验证指标端点配置
   - 检查指标收集器初始化

2. **健康检查失败**
   - 检查依赖服务状态
   - 验证健康检查逻辑
   - 检查网络连接

3. **性能监控数据异常**
   - 检查时间戳同步
   - 验证追踪上下文管理
   - 检查并发处理逻辑

### 调试技巧

```kotlin
// 启用详细监控日志
class DebugMonitoringManager {
    fun initializeWithDebug() {
        println("初始化监控系统...")
        
        MonitoringManager.initialize()
        
        // 添加调试健康检查
        val healthRegistry = MonitoringManager.getHealthCheckRegistry()
        healthRegistry.register("debug", DebugHealthCheck())
        
        println("监控系统初始化完成")
    }
    
    fun printDebugMetrics() {
        val metrics = MonitoringManager.getMetricsCollector().getMetrics()
        println("当前指标: $metrics")
        
        val healthStatus = MonitoringManager.getHealthCheckRegistry().runHealthChecks()
        println("健康状态: $healthStatus")
    }
}

class DebugHealthCheck : HealthCheck {
    override fun check(): HealthCheckResult {
        println("执行调试健康检查...")
        return HealthCheckResult.healthy("调试检查正常")
    }
}
```

## 相关链接

- [基础设施层文档](../README.md)
- [配置模块文档](../configuration/README.md)
- [事件总线模块文档](../event-bus/README.md)
- [Prometheus监控指南](https://prometheus.io/docs/introduction/overview/)
- [健康检查模式](https://microservices.io/patterns/observability/health-check.html)
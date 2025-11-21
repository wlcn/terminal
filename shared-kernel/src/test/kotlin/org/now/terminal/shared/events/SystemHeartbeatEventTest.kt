package org.now.terminal.shared.events

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.string.shouldContain
import java.time.Instant
import java.util.concurrent.TimeUnit

class SystemHeartbeatEventTest : StringSpec({
    
    "应该创建健康心跳事件" {
        val metrics = SystemHeartbeatEvent.SystemMetrics(
            cpuUsage = 25.5,
            memoryUsage = 60.0,
            diskUsage = 45.0,
            activeSessions = 10,
            uptimeSeconds = 3600,
            customMetrics = mapOf("queue_size" to "5")
        )
        
        val event = SystemHeartbeatEvent.createHealthy("system1", "database", metrics)
        
        event.systemId shouldBe "system1"
        event.component shouldBe "database"
        event.status shouldBe SystemHeartbeatEvent.SystemStatus.HEALTHY
        event.metrics.cpuUsage shouldBe 25.5
        event.metrics.memoryUsage shouldBe 60.0
        event.metrics.diskUsage shouldBe 45.0
        event.metrics.activeSessions shouldBe 10
        event.metrics.uptimeSeconds shouldBe 3600
        event.metrics.customMetrics["queue_size"] shouldBe "5"
        event.isHealthy() shouldBe true
        event.isAvailable() shouldBe true
    }
    
    "应该创建降级心跳事件" {
        val event = SystemHeartbeatEvent.createDegraded("system2", "webserver")
        
        event.systemId shouldBe "system2"
        event.component shouldBe "webserver"
        event.status shouldBe SystemHeartbeatEvent.SystemStatus.DEGRADED
        event.isHealthy() shouldBe false
        event.isAvailable() shouldBe true
    }
    
    "应该创建不健康心跳事件" {
        val event = SystemHeartbeatEvent(
            systemId = "system3",
            component = "cache",
            status = SystemHeartbeatEvent.SystemStatus.UNHEALTHY
        )
        
        event.systemId shouldBe "system3"
        event.component shouldBe "cache"
        event.status shouldBe SystemHeartbeatEvent.SystemStatus.UNHEALTHY
        event.isHealthy() shouldBe false
        event.isAvailable() shouldBe false
    }
    
    "应该创建离线心跳事件" {
        val event = SystemHeartbeatEvent(
            systemId = "system4",
            component = "monitoring",
            status = SystemHeartbeatEvent.SystemStatus.OFFLINE
        )
        
        event.systemId shouldBe "system4"
        event.component shouldBe "monitoring"
        event.status shouldBe SystemHeartbeatEvent.SystemStatus.OFFLINE
        event.isHealthy() shouldBe false
        event.isAvailable() shouldBe false
    }
    
    "应该自动生成事件ID" {
        val event1 = SystemHeartbeatEvent.createHealthy("system1", "component1")
        val event2 = SystemHeartbeatEvent.createHealthy("system1", "component1")
        
        event1.eventHelper.eventId shouldNotBe event2.eventHelper.eventId
        event1.eventHelper.eventId.toString().isNotEmpty() shouldBe true
        event2.eventHelper.eventId.toString().isNotEmpty() shouldBe true
    }
    
    "应该自动设置发生时间" {
        val before = Instant.now()
        TimeUnit.MILLISECONDS.sleep(10) // 确保时间不同
        val event = SystemHeartbeatEvent.createHealthy("system1", "component1")
        TimeUnit.MILLISECONDS.sleep(10)
        val after = Instant.now()
        
        event.eventHelper.occurredAt.isAfter(before) shouldBe true
        event.eventHelper.occurredAt.isBefore(after) shouldBe true
    }
    
    "应该正确计算事件年龄" {
        val oldTime = Instant.now().minusSeconds(60) // 1分钟前
        val eventHelper = EventHelper(
            eventId = org.now.terminal.shared.valueobjects.EventId.generate(),
            occurredAt = oldTime,
            eventType = "SystemHeartbeatEvent",
            aggregateType = "System"
        )
        val event = SystemHeartbeatEvent(
            eventHelper = eventHelper,
            systemId = "system1",
            component = "component1",
            status = SystemHeartbeatEvent.SystemStatus.HEALTHY
        )
        
        val age = event.getAgeSeconds()
        age shouldBe 60L
    }
    
    "系统状态枚举应该包含所有状态" {
        val statuses = SystemHeartbeatEvent.SystemStatus.values()
        
        statuses.map { it.name } shouldContain "HEALTHY"
        statuses.map { it.name } shouldContain "DEGRADED"
        statuses.map { it.name } shouldContain "UNHEALTHY"
        statuses.map { it.name } shouldContain "OFFLINE"
    }
    
    "系统指标应该有默认值" {
        val metrics = SystemHeartbeatEvent.SystemMetrics()
        
        metrics.cpuUsage shouldBe 0.0
        metrics.memoryUsage shouldBe 0.0
        metrics.diskUsage shouldBe 0.0
        metrics.activeSessions shouldBe 0
        metrics.uptimeSeconds shouldBe 0
        metrics.customMetrics shouldBe emptyMap()
    }
    
    "应该正确比较事件" {
        val event1 = SystemHeartbeatEvent.createHealthy("system1", "component1")
        val event2 = SystemHeartbeatEvent.createHealthy("system1", "component1")
        val event3 = SystemHeartbeatEvent.createDegraded("system2", "component2")
        
        event1 shouldNotBe event2 // 不同事件ID
        event1 shouldNotBe event3 // 不同系统ID
        event1 shouldBe event1 // 相同对象
    }
    
    "应该正确序列化事件" {
        val event = SystemHeartbeatEvent.createHealthy("system1", "component1")
        
        event.toString() shouldContain "SystemHeartbeatEvent"
        event.toString() shouldContain "system1"
        event.toString() shouldContain "component1"
        event.toString() shouldContain "HEALTHY"
    }
    
    "应该处理自定义指标" {
        val customMetrics = mapOf(
            "response_time" to "150.0",
            "error_rate" to "0.01",
            "throughput" to "1000"
        )
        
        val metrics = SystemHeartbeatEvent.SystemMetrics(
            customMetrics = customMetrics
        )
        
        metrics.customMetrics.containsKey("response_time") shouldBe true
        metrics.customMetrics.containsKey("error_rate") shouldBe true
        metrics.customMetrics.containsKey("throughput") shouldBe true
        metrics.customMetrics["response_time"] shouldBe "150.0"
        metrics.customMetrics["error_rate"] shouldBe "0.01"
        metrics.customMetrics["throughput"] shouldBe "1000"
    }
    
    "应该验证系统状态转换" {
        val healthyEvent = SystemHeartbeatEvent.createHealthy("system1", "component1")
        val degradedEvent = SystemHeartbeatEvent.createDegraded("system1", "component1")
        val unhealthyEvent = SystemHeartbeatEvent(
            systemId = "system1",
            component = "component1",
            status = SystemHeartbeatEvent.SystemStatus.UNHEALTHY
        )
        val offlineEvent = SystemHeartbeatEvent(
            systemId = "system1",
            component = "component1",
            status = SystemHeartbeatEvent.SystemStatus.OFFLINE
        )
        
        // 健康状态检查
        healthyEvent.isHealthy() shouldBe true
        degradedEvent.isHealthy() shouldBe false
        unhealthyEvent.isHealthy() shouldBe false
        offlineEvent.isHealthy() shouldBe false
        
        // 可用性检查
        healthyEvent.isAvailable() shouldBe true
        degradedEvent.isAvailable() shouldBe true
        unhealthyEvent.isAvailable() shouldBe false
        offlineEvent.isAvailable() shouldBe false
    }
})
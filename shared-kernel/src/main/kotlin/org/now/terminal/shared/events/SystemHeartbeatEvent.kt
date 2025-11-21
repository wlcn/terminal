package org.now.terminal.shared.events

import org.now.terminal.shared.valueobjects.EventId
import java.time.Instant

/**
 * 系统心跳事件
 * 用于监控系统运行状态和健康检查
 */
data class SystemHeartbeatEvent(
    val eventId: EventId = EventId.generate(),
    val occurredAt: Instant = Instant.now(),
    val systemId: String,
    val component: String,
    val status: SystemStatus,
    val metrics: SystemMetrics = SystemMetrics()
) {
    /**
     * 系统状态枚举
     */
    enum class SystemStatus {
        HEALTHY,
        DEGRADED,
        UNHEALTHY,
        OFFLINE
    }

    /**
     * 系统指标数据
     */
    data class SystemMetrics(
        val cpuUsage: Double = 0.0,
        val memoryUsage: Double = 0.0,
        val diskUsage: Double = 0.0,
        val activeSessions: Int = 0,
        val uptimeSeconds: Long = 0,
        val customMetrics: Map<String, Any> = emptyMap()
    )

    /**
     * 检查系统是否健康
     */
    fun isHealthy(): Boolean = status == SystemStatus.HEALTHY

    /**
     * 检查系统是否可用（健康或降级）
     */
    fun isAvailable(): Boolean = status == SystemStatus.HEALTHY || status == SystemStatus.DEGRADED

    /**
     * 获取事件年龄（秒）
     */
    fun getAgeSeconds(): Long = Instant.now().epochSecond - occurredAt.epochSecond

    companion object {
        /**
         * 创建健康心跳事件
         */
        fun createHealthy(systemId: String, component: String, metrics: SystemMetrics = SystemMetrics()): SystemHeartbeatEvent {
            return SystemHeartbeatEvent(
                systemId = systemId,
                component = component,
                status = SystemStatus.HEALTHY,
                metrics = metrics
            )
        }

        /**
         * 创建降级心跳事件
         */
        fun createDegraded(systemId: String, component: String, metrics: SystemMetrics = SystemMetrics()): SystemHeartbeatEvent {
            return SystemHeartbeatEvent(
                systemId = systemId,
                component = component,
                status = SystemStatus.DEGRADED,
                metrics = metrics
            )
        }
    }
}
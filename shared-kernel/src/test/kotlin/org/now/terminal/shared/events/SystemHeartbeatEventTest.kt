package org.now.terminal.shared.events

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SystemHeartbeatEventTest {

    @Test
    fun `should create system heartbeat event with default values`() {
        val event = SystemHeartbeatEvent(
            systemId = "terminal-server-1",
            component = "session-manager",
            status = SystemHeartbeatEvent.SystemStatus.HEALTHY
        )
        
        assertEquals("terminal-server-1", event.systemId)
        assertEquals("session-manager", event.component)
        assertEquals(SystemHeartbeatEvent.SystemStatus.HEALTHY, event.status)
        assertTrue(event.isHealthy())
        assertTrue(event.isAvailable())
    }

    @Test
    fun `should create healthy heartbeat event using companion method`() {
        val metrics = SystemHeartbeatEvent.SystemMetrics(
            cpuUsage = 25.5,
            memoryUsage = 45.2,
            diskUsage = 10.0,
            activeSessions = 5,
            uptimeSeconds = 3600
        )
        
        val event = SystemHeartbeatEvent.createHealthy(
            systemId = "terminal-server-1",
            component = "session-manager",
            metrics = metrics
        )
        
        assertEquals(SystemHeartbeatEvent.SystemStatus.HEALTHY, event.status)
        assertEquals(25.5, event.metrics.cpuUsage)
        assertEquals(5, event.metrics.activeSessions)
        assertTrue(event.isHealthy())
    }

    @Test
    fun `should create degraded heartbeat event using companion method`() {
        val event = SystemHeartbeatEvent.createDegraded(
            systemId = "terminal-server-1",
            component = "session-manager"
        )
        
        assertEquals(SystemHeartbeatEvent.SystemStatus.DEGRADED, event.status)
        assertFalse(event.isHealthy())
        assertTrue(event.isAvailable())
    }

    @Test
    fun `should check system availability correctly`() {
        val healthyEvent = SystemHeartbeatEvent(
            systemId = "server-1",
            component = "test",
            status = SystemHeartbeatEvent.SystemStatus.HEALTHY
        )
        
        val degradedEvent = SystemHeartbeatEvent(
            systemId = "server-1",
            component = "test",
            status = SystemHeartbeatEvent.SystemStatus.DEGRADED
        )
        
        val unhealthyEvent = SystemHeartbeatEvent(
            systemId = "server-1",
            component = "test",
            status = SystemHeartbeatEvent.SystemStatus.UNHEALTHY
        )
        
        val offlineEvent = SystemHeartbeatEvent(
            systemId = "server-1",
            component = "test",
            status = SystemHeartbeatEvent.SystemStatus.OFFLINE
        )
        
        assertTrue(healthyEvent.isAvailable())
        assertTrue(degradedEvent.isAvailable())
        assertFalse(unhealthyEvent.isAvailable())
        assertFalse(offlineEvent.isAvailable())
    }

    @Test
    fun `should calculate event age correctly`() {
        val event = SystemHeartbeatEvent(
            systemId = "server-1",
            component = "test",
            status = SystemHeartbeatEvent.SystemStatus.HEALTHY
        )
        
        val age = event.getAgeSeconds()
        
        assertTrue(age >= 0)
        assertTrue(age < 10) // Should be very recent
    }

    @Test
    fun `should create system metrics with default values`() {
        val metrics = SystemHeartbeatEvent.SystemMetrics()
        
        assertEquals(0.0, metrics.cpuUsage)
        assertEquals(0.0, metrics.memoryUsage)
        assertEquals(0.0, metrics.diskUsage)
        assertEquals(0, metrics.activeSessions)
        assertEquals(0L, metrics.uptimeSeconds)
        assertTrue(metrics.customMetrics.isEmpty())
    }

    @Test
    fun `should create system metrics with custom values`() {
        val customMetrics = mapOf("queue_size" to 100, "response_time" to 50.5)
        
        val metrics = SystemHeartbeatEvent.SystemMetrics(
            cpuUsage = 75.0,
            memoryUsage = 80.5,
            diskUsage = 90.0,
            activeSessions = 10,
            uptimeSeconds = 7200,
            customMetrics = customMetrics
        )
        
        assertEquals(75.0, metrics.cpuUsage)
        assertEquals(10, metrics.activeSessions)
        assertEquals(7200L, metrics.uptimeSeconds)
        assertEquals(100, metrics.customMetrics["queue_size"])
        assertEquals(50.5, metrics.customMetrics["response_time"])
    }

    @Test
    fun `should have correct system status values`() {
        val statuses = SystemHeartbeatEvent.SystemStatus.values()
        
        assertEquals(4, statuses.size)
        assertTrue(statuses.contains(SystemHeartbeatEvent.SystemStatus.HEALTHY))
        assertTrue(statuses.contains(SystemHeartbeatEvent.SystemStatus.DEGRADED))
        assertTrue(statuses.contains(SystemHeartbeatEvent.SystemStatus.UNHEALTHY))
        assertTrue(statuses.contains(SystemHeartbeatEvent.SystemStatus.OFFLINE))
    }
}
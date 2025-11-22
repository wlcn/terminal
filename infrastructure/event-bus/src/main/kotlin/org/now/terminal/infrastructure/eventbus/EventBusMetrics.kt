package org.now.terminal.infrastructure.eventbus

import org.now.terminal.infrastructure.logging.TerminalLogger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * 事件总线指标收集器
 */
class EventBusMetrics {
    private val logger = TerminalLogger.getLogger(EventBusMetrics::class.java)
    
    private val eventCounters = ConcurrentHashMap<String, AtomicLong>()
    private val eventTimers = ConcurrentHashMap<String, MutableList<Long>>()
    private val errorCounters = ConcurrentHashMap<String, AtomicLong>()
    
    private val totalEventsCounter = AtomicLong(0)
    private val totalErrorsCounter = AtomicLong(0)
    private val activeSubscriptions = AtomicLong(0)
    
    fun recordEventPublished(eventType: String) {
        totalEventsCounter.incrementAndGet()
        getEventCounter(eventType).incrementAndGet()
        logger.debug("Event published: {}", eventType)
    }
    
    fun recordEventProcessed(eventType: String, duration: Long, timeUnit: TimeUnit = TimeUnit.MILLISECONDS) {
        val durationMs = timeUnit.toMillis(duration)
        getEventTimer(eventType).add(durationMs)
        logger.debug("Event processed: {} in {} {}", eventType, duration, timeUnit)
    }
    
    fun recordEventError(eventType: String, errorType: String) {
        totalErrorsCounter.incrementAndGet()
        getErrorCounter(eventType, errorType).incrementAndGet()
        logger.warn("Event processing error: {} - {}", eventType, errorType)
    }
    
    fun updateActiveSubscriptions(delta: Int) {
        activeSubscriptions.addAndGet(delta.toLong())
        logger.debug("Active subscriptions updated: {}", activeSubscriptions.get())
    }
    
    fun getMetricsSnapshot(): EventBusMetricsSnapshot {
        return EventBusMetricsSnapshot(
            totalEvents = totalEventsCounter.get(),
            totalErrors = totalErrorsCounter.get(),
            activeSubscriptions = activeSubscriptions.get(),
            eventCounts = eventCounters.mapValues { it.value.get() },
            averageProcessingTimes = eventTimers.mapValues { 
                if (it.value.isEmpty()) 0.0 else it.value.average() 
            }
        )
    }
    
    private fun getEventCounter(eventType: String): AtomicLong {
        return eventCounters.computeIfAbsent(eventType) { AtomicLong(0) }
    }
    
    private fun getEventTimer(eventType: String): MutableList<Long> {
        return eventTimers.computeIfAbsent(eventType) { mutableListOf() }
    }
    
    private fun getErrorCounter(eventType: String, errorType: String): AtomicLong {
        val key = "$eventType.$errorType"
        return errorCounters.computeIfAbsent(key) { AtomicLong(0) }
    }
}

/**
 * 事件总线指标快照
 */
data class EventBusMetricsSnapshot(
    val totalEvents: Long,
    val totalErrors: Long,
    val activeSubscriptions: Long,
    val eventCounts: Map<String, Long>,
    val averageProcessingTimes: Map<String, Double>
)
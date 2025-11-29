package org.now.terminal.boundedcontexts.terminalsession.infrastructure.service

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag

/**
 * Terminal Monitoring Service
 * Collects metrics about terminal sessions and processes
 */
class TerminalMonitoringService(private val meterRegistry: MeterRegistry) {
    
    // Counters
    private val sessionCreatedCounter: Counter = meterRegistry.counter("terminal.sessions.created")
    private val sessionTerminatedCounter: Counter = meterRegistry.counter("terminal.sessions.terminated")
    private val processCreatedCounter: Counter = meterRegistry.counter("terminal.processes.created")
    private val processTerminatedCounter: Counter = meterRegistry.counter("terminal.processes.terminated")
    private val bytesWrittenCounter: Counter = meterRegistry.counter("terminal.bytes.written")
    private val bytesReadCounter: Counter = meterRegistry.counter("terminal.bytes.read")
    
    // Gauges
    private var activeSessionsGauge: Gauge? = null
    private var activeProcessesGauge: Gauge? = null
    
    /**
     * Initialize gauges
     */
    fun initializeGauges(
        activeSessionsProvider: () -> Int,
        activeProcessesProvider: () -> Int
    ) {
        activeSessionsGauge = Gauge.builder("terminal.sessions.active", activeSessionsProvider)
            .description("Number of active terminal sessions")
            .register(meterRegistry)
        
        activeProcessesGauge = Gauge.builder("terminal.processes.active", activeProcessesProvider)
            .description("Number of active terminal processes")
            .register(meterRegistry)
    }
    
    /**
     * Increment session created counter
     */
    fun incrementSessionCreated() {
        sessionCreatedCounter.increment()
    }
    
    /**
     * Increment session terminated counter
     */
    fun incrementSessionTerminated() {
        sessionTerminatedCounter.increment()
    }
    
    /**
     * Increment process created counter
     */
    fun incrementProcessCreated() {
        processCreatedCounter.increment()
    }
    
    /**
     * Increment process terminated counter
     */
    fun incrementProcessTerminated() {
        processTerminatedCounter.increment()
    }
    
    /**
     * Record bytes written to terminal
     */
    fun recordBytesWritten(bytes: Int) {
        bytesWrittenCounter.increment(bytes.toDouble())
    }
    
    /**
     * Record bytes read from terminal
     */
    fun recordBytesRead(bytes: Int) {
        bytesReadCounter.increment(bytes.toDouble())
    }
    
    /**
     * Record session duration
     */
    fun recordSessionDuration(durationMs: Long) {
        meterRegistry.timer("terminal.sessions.duration")
            .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS)
    }
    
    /**
     * Record process duration
     */
    fun recordProcessDuration(durationMs: Long) {
        meterRegistry.timer("terminal.processes.duration")
            .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS)
    }
    
    /**
     * Record error count
     */
    fun recordError(errorType: String) {
        meterRegistry.counter("terminal.errors", listOf(Tag.of("type", errorType)))
            .increment()
    }
}

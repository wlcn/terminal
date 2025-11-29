package org.now.terminal.boundedcontexts.terminalsession.infrastructure.config

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.koin.dsl.module
import org.now.terminal.boundedcontexts.terminalsession.domain.service.SessionStorage
import org.now.terminal.boundedcontexts.terminalsession.domain.service.InMemorySessionStorage
import org.now.terminal.boundedcontexts.terminalsession.domain.service.TerminalProcessManager
import org.now.terminal.boundedcontexts.terminalsession.domain.service.TerminalProcessService
import org.now.terminal.boundedcontexts.terminalsession.domain.service.TerminalSessionService
import org.now.terminal.boundedcontexts.terminalsession.infrastructure.service.Pty4jTerminalProcessManager
import org.now.terminal.boundedcontexts.terminalsession.infrastructure.service.TerminalConfigService
import org.now.terminal.boundedcontexts.terminalsession.infrastructure.service.TerminalMonitoringService

// Export the terminal session module for use in Koin configuration
val terminalSessionModule = module {
    // Meter registry for monitoring
    single<MeterRegistry> { PrometheusMeterRegistry(PrometheusConfig.DEFAULT) }
    
    // Terminal monitoring service
    single { TerminalMonitoringService(get()) }
    
    // Terminal configuration service
    single { TerminalConfigService(get()) }
    
    // Terminal configuration
    single { get<TerminalConfigService>().loadConfig() }
    
    // Session storage
    single<SessionStorage> { InMemorySessionStorage() }
    
    // Terminal process manager
    single<TerminalProcessManager> { Pty4jTerminalProcessManager() }
    
    // Terminal process service
    single { TerminalProcessService(get()) }
    
    // Terminal session service
    single { 
        val terminalConfig = get<org.now.terminal.boundedcontexts.terminalsession.domain.model.TerminalConfig>()
        val sessionStorage = get<SessionStorage>()
        val terminalProcessManager = get<TerminalProcessManager>()
        val monitoringService = get<TerminalMonitoringService>()
        
        val sessionService = TerminalSessionService(
            terminalConfig = terminalConfig,
            sessionStorage = sessionStorage,
            terminalProcessManager = terminalProcessManager
        )
        
        // Initialize gauges
        monitoringService.initializeGauges(
            activeSessionsProvider = { sessionService.getAllSessions().size },
            activeProcessesProvider = { 0 } // TODO: Implement active processes count
        )
        
        sessionService
    }
}

package org.now.terminal.boundedcontexts.terminalsession.infrastructure.config

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.runBlocking
import org.koin.dsl.module
import org.now.terminal.boundedcontexts.terminalsession.domain.InMemoryTerminalSessionRepository
import org.now.terminal.boundedcontexts.terminalsession.domain.TerminalSessionRepository
import org.now.terminal.boundedcontexts.terminalsession.domain.model.TerminalConfig
import org.now.terminal.boundedcontexts.terminalsession.domain.service.TerminalProcessManager
import org.now.terminal.boundedcontexts.terminalsession.domain.service.TerminalProcessService
import org.now.terminal.boundedcontexts.terminalsession.domain.service.TerminalSessionExpiryManager
import org.now.terminal.boundedcontexts.terminalsession.domain.service.TerminalSessionService
import org.now.terminal.boundedcontexts.terminalsession.infrastructure.service.Pty4jTerminalProcessManager
import org.now.terminal.boundedcontexts.terminalsession.infrastructure.service.TerminalConfigService
import org.now.terminal.boundedcontexts.terminalsession.infrastructure.service.TerminalMonitoringService

// Export the terminal session module for use in Koin configuration
val terminalSessionModule = module {
    // Meter registry for monitoring - using SimpleMeterRegistry for simplicity
    single<MeterRegistry> { SimpleMeterRegistry() }

    // Terminal monitoring service
    single { TerminalMonitoringService(get()) }

    // Terminal configuration service
    single { TerminalConfigService(get()) }

    // Terminal configuration
    single { get<TerminalConfigService>().loadConfig() }

    // Session storage
    single<TerminalSessionRepository> { InMemoryTerminalSessionRepository() }

    // Terminal process manager
    single<TerminalProcessManager> { Pty4jTerminalProcessManager(get()) }

    // Terminal process service
    single { TerminalProcessService(get()) }

    // Terminal session expiry manager - global singleton
    single {
        val terminalConfig = get<TerminalConfig>()
        val terminalProcessManager = get<TerminalProcessManager>()
        val terminalSessionRepository = get<TerminalSessionRepository>()
        TerminalSessionExpiryManager(
            terminalProcessManager = terminalProcessManager,
            terminalSessionRepository = terminalSessionRepository
        )
    }

    // Terminal session service
    single {
        val terminalConfig = get<TerminalConfig>()
        val terminalSessionRepository = get<TerminalSessionRepository>()
        val monitoringService = get<TerminalMonitoringService>()

        val sessionService = TerminalSessionService(
            terminalConfig = terminalConfig,
            terminalSessionRepository = terminalSessionRepository
        )

        // Initialize gauges
        monitoringService.initializeGauges(
            activeSessionsProvider = { sessionService.getAllSessions().size },
            activeProcessesProvider = { 0 } // TODO: Implement active processes count
        )

        sessionService
    }
}

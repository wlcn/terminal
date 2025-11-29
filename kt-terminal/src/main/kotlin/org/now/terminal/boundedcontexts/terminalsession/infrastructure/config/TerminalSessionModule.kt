package org.now.terminal.boundedcontexts.terminalsession.infrastructure.config

import org.koin.dsl.module
import org.now.terminal.boundedcontexts.terminalsession.domain.service.SessionStorage
import org.now.terminal.boundedcontexts.terminalsession.domain.service.InMemorySessionStorage
import org.now.terminal.boundedcontexts.terminalsession.domain.service.TerminalProcessManager
import org.now.terminal.boundedcontexts.terminalsession.domain.service.TerminalProcessService
import org.now.terminal.boundedcontexts.terminalsession.domain.service.TerminalSessionService
import org.now.terminal.boundedcontexts.terminalsession.infrastructure.service.Pty4jTerminalProcessManager
import org.now.terminal.boundedcontexts.terminalsession.infrastructure.service.TerminalConfigService

// Export the terminal session module for use in Koin configuration
val terminalSessionModule = module {
    // Terminal configuration service
    single { TerminalConfigService(get()) }
    
    // Terminal configuration
    single { get<TerminalConfigService>().loadConfig() }
    
    // Session storage
    single<SessionStorage> { InMemorySessionStorage() }
    
    // Terminal session service
    single { 
        val terminalConfig = get<org.now.terminal.boundedcontexts.terminalsession.domain.model.TerminalConfig>()
        val sessionStorage = get<SessionStorage>()
        val terminalProcessManager = get<TerminalProcessManager>()
        TerminalSessionService(
            terminalConfig = terminalConfig,
            sessionStorage = sessionStorage,
            terminalProcessManager = terminalProcessManager
        )
    }
    
    // Terminal process manager
    single<TerminalProcessManager> { Pty4jTerminalProcessManager() }
    
    // Terminal process service
    single { TerminalProcessService(get()) }
}

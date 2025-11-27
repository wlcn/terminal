package org.now.terminal.infrastructure.boundedcontext.terminalsession.config

import org.koin.core.module.Module
import org.koin.dsl.module
import org.now.terminal.boundedcontext.terminalsession.application.usecases.ExecuteTerminalCommandUseCase
import org.now.terminal.boundedcontext.terminalsession.application.usecases.TerminalSessionManagementUseCase
import org.now.terminal.boundedcontext.terminalsession.application.usecases.TerminalSessionQueryUseCase
import org.now.terminal.boundedcontext.terminalsession.domain.repositories.TerminalSessionRepository
import org.now.terminal.boundedcontext.terminalsession.domain.services.executor.CommandExecutor
import org.now.terminal.boundedcontext.terminalsession.infrastructure.ProcessFactory
import org.now.terminal.infrastructure.boundedcontext.terminalsession.executor.Pty4jCommandExecutor
import org.now.terminal.infrastructure.boundedcontext.terminalsession.process.Pty4jProcessFactory
import org.now.terminal.infrastructure.boundedcontext.terminalsession.repositories.InMemoryTerminalSessionRepository
import org.now.terminal.infrastructure.boundedcontext.terminalsession.web.controllers.TerminalSessionController

/**
 * Terminal Session Koin Module
 * 
 * Configures dependency injection for terminal session bounded context
 */
val terminalSessionModule: Module = module {
    // Repositories
    single<TerminalSessionRepository> { InMemoryTerminalSessionRepository() }
    
    // Domain Services
    single<CommandExecutor> { Pty4jCommandExecutor() }
    single<ProcessFactory> { Pty4jProcessFactory() }
    
    // Use Cases
    single { TerminalSessionManagementUseCase(get()) }
    single { TerminalSessionQueryUseCase(get()) }
    single { ExecuteTerminalCommandUseCase(get(), get()) }
    
    // Controllers
    single {
        TerminalSessionController(
            terminalSessionManagementUseCase = get(),
            terminalSessionQueryUseCase = get(),
            executeTerminalCommandUseCase = get(),
            processFactory = get()
        )
    }
}
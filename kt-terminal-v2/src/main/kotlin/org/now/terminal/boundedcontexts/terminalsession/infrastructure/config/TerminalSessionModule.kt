package org.now.terminal.boundedcontexts.terminalsession.infrastructure.config

import org.koin.dsl.module
import org.now.terminal.boundedcontexts.terminalsession.domain.service.TerminalProcessManager
import org.now.terminal.boundedcontexts.terminalsession.domain.service.TerminalProcessService
import org.now.terminal.boundedcontexts.terminalsession.domain.service.TerminalSessionService
import org.now.terminal.boundedcontexts.terminalsession.infrastructure.service.Pty4jTerminalProcessManager

// Export the terminal session module for use in Koin configuration
val terminalSessionModule = module {
    single { TerminalSessionService() }
    single<TerminalProcessManager> { Pty4jTerminalProcessManager() }
    single { TerminalProcessService(get()) }
}

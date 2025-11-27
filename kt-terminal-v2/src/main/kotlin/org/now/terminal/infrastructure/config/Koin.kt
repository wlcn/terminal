package org.now.terminal.infrastructure.config

import io.ktor.server.application.Application
import io.ktor.server.application.install
import org.koin.ktor.plugin.Koin
import org.now.terminal.infrastructure.boundedcontext.user.config.userModule
import org.now.terminal.infrastructure.boundedcontext.terminalsession.config.terminalSessionModule

/**
 * Koin Dependency Injection Configuration
 * Configures Koin DI framework with all application modules
 */
fun Application.configureKoin() {
    install(Koin) {
        modules(userModule, terminalSessionModule)
    }
}
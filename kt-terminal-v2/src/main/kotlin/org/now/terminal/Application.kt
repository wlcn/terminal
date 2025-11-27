package org.now.terminal

import io.ktor.server.application.*
import org.now.terminal.boundedcontexts.terminalsession.infrastructure.api.configureTerminalWebSocketRoutes
import org.now.terminal.boundedcontexts.terminalsession.infrastructure.api.configureTerminalWebTransportRoutes
import org.now.terminal.infrastructure.config.configureHTTP
import org.now.terminal.infrastructure.config.configureKoin
import org.now.terminal.infrastructure.config.configureMonitoring
import org.now.terminal.infrastructure.config.configureRouting
import org.now.terminal.infrastructure.config.configureSerialization
import org.now.terminal.infrastructure.config.installWebSockets

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureKoin()
    configureMonitoring()
    configureHTTP()
    configureSerialization()
    installWebSockets()
    configureRouting()
    configureTerminalWebSocketRoutes()
    configureTerminalWebTransportRoutes()
}

package org.now.terminal

import io.ktor.server.application.*
import org.now.terminal.infrastructure.config.configureDatabases
import org.now.terminal.infrastructure.config.configureHTTP
import org.now.terminal.infrastructure.config.configureMonitoring
import org.now.terminal.infrastructure.config.configureRouting
import org.now.terminal.infrastructure.config.configureSecurity
import org.now.terminal.infrastructure.config.configureSerialization
import org.now.terminal.infrastructure.config.configureSockets

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureMonitoring()
    configureHTTP()
    configureSecurity()
    configureSerialization()
    configureDatabases()
    configureSockets()
    configureRouting()
}

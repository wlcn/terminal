package org.now.terminal

import io.ktor.server.application.*
import org.now.terminal.boundedcontexts.terminalsession.infrastructure.api.configureTerminalSessionRoutes
import org.now.terminal.boundedcontexts.terminalsession.infrastructure.api.configureTerminalWebSocketRoutes
import org.now.terminal.boundedcontexts.terminalsession.infrastructure.api.configureTerminalWebTransportRoutes
import org.now.terminal.boundedcontexts.terminalsession.infrastructure.service.TerminalConfigManager
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
    // 初始化终端配置管理器
    TerminalConfigManager.init(this.environment.config)
    
    configureKoin()
    configureMonitoring()
    configureHTTP()
    configureSerialization()
    installWebSockets()
    configureRouting()
    configureTerminalSessionRoutes()
    configureTerminalWebSocketRoutes()
    configureTerminalWebTransportRoutes()
}

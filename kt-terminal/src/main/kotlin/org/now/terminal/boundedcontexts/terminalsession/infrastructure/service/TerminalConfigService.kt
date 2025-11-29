package org.now.terminal.boundedcontexts.terminalsession.infrastructure.service

import io.ktor.server.application.*
import org.now.terminal.boundedcontexts.terminalsession.domain.model.TerminalConfig
import org.now.terminal.boundedcontexts.terminalsession.domain.model.TerminalSize

/**
 * Terminal Configuration Service
 * Reads terminal configuration from application.conf
 */
class TerminalConfigService(private val application: Application) {
    
    /**
     * Load terminal configuration from application.conf
     */
    fun loadConfig(): TerminalConfig {
        val config = application.environment.config
        
        // Read default shell type
        val defaultShellType = config.property("terminal.defaultShellType").getString()
        
        // Read session timeout in milliseconds
        val sessionTimeoutMs = config.property("terminal.sessionTimeout").getString().toLong()
        
        // Read default terminal size if configured, otherwise use default
        val defaultColumns = config.propertyOrNull("terminal.defaultSize.columns")?.getString()?.toInt() ?: 80
        val defaultRows = config.propertyOrNull("terminal.defaultSize.rows")?.getString()?.toInt() ?: 24
        val defaultTerminalSize = TerminalSize(defaultColumns, defaultRows)
        
        return TerminalConfig(
            defaultShellType = defaultShellType,
            sessionTimeoutMs = sessionTimeoutMs,
            defaultTerminalSize = defaultTerminalSize
        )
    }
}

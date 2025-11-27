package org.now.terminal.boundedcontext.terminalsession.infrastructure

import org.now.terminal.boundedcontext.terminalsession.domain.valueobjects.ShellType
import org.now.terminal.boundedcontext.terminalsession.domain.valueobjects.TerminalSize

/**
 * Process factory interface - responsible for creating terminal processes
 */
interface ProcessFactory {
    fun createProcess(
        shellType: ShellType,
        workingDirectory: String,
        environment: Map<String, String>,
        terminalSize: TerminalSize
    ): TerminalProcess
}
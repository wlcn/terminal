package org.now.terminal.infrastructure.boundedcontext.terminalsession.process

import org.now.terminal.boundedcontext.terminalsession.domain.valueobjects.TerminalSize
import org.now.terminal.boundedcontext.terminalsession.domain.valueobjects.ShellType
import org.now.terminal.boundedcontext.terminalsession.infrastructure.ProcessFactory
import org.now.terminal.boundedcontext.terminalsession.infrastructure.TerminalProcess

/**
 * Process factory implementation for creating Pty4j-based terminal processes
 */
class Pty4jProcessFactory : ProcessFactory {
    
    override fun createProcess(
        shellType: ShellType,
        workingDirectory: String,
        environment: Map<String, String>,
        terminalSize: TerminalSize
    ): TerminalProcess {
        // Create terminal process directly using the provided parameters
        return Pty4jTerminalProcess(
            shellType = shellType,
            workingDirectory = workingDirectory,
            environment = environment,
            terminalSize = terminalSize
        )
    }
    

}
package org.now.terminal.session.infrastructure.process

import org.now.terminal.session.domain.services.Process
import org.now.terminal.session.domain.services.ProcessFactory
import org.now.terminal.session.domain.valueobjects.PtyConfiguration
import org.now.terminal.shared.valueobjects.SessionId

/**
 * Pty4jProcessFactory - 基于Pty4j的Process工厂实现
 */
class Pty4jProcessFactory : ProcessFactory {
    
    override fun createProcess(ptyConfig: PtyConfiguration, sessionId: SessionId): Process {
        return Pty4jProcess(ptyConfig, sessionId)
    }
}
package org.now.terminal.session.application.handlers

import org.now.terminal.session.domain.events.TerminalOutputEvent
import org.now.terminal.session.domain.services.TerminalOutputPublisher
import org.now.terminal.infrastructure.eventbus.EventBus
import org.now.terminal.shared.events.EventHandler

/**
 * 终端输出事件处理器
 * 业务模块只负责处理领域事件，不关心具体推送实现
 */
class TerminalOutputEventHandler(
    private val terminalOutputPublisher: TerminalOutputPublisher
) : EventHandler<TerminalOutputEvent> {

    override suspend fun handle(event: TerminalOutputEvent) {
        terminalOutputPublisher.publishOutput(event.sessionId, event.output)
    }

    override fun canHandle(eventType: String): Boolean {
        return eventType == "TerminalOutputEvent"
    }
}
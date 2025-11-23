package org.now.terminal.session.application.handlers

import org.now.terminal.session.domain.events.TerminalOutputEvent
import org.now.terminal.session.domain.services.TerminalOutputPublisher
import org.now.terminal.infrastructure.eventbus.EventBus
import org.now.terminal.infrastructure.logging.TerminalLogger
import org.now.terminal.shared.events.EventHandler

/**
 * 终端输出事件处理器
 * 业务模块只负责处理领域事件，不关心具体推送实现
 */
class TerminalOutputEventHandler(
    private val terminalOutputPublisher: TerminalOutputPublisher
) : EventHandler<TerminalOutputEvent> {
    
    private val logger = TerminalLogger.getLogger(TerminalOutputEventHandler::class.java)

    override suspend fun handle(event: TerminalOutputEvent) {
        logger.info("处理终端输出事件 - 会话ID: {}, 输出长度: {}, 输出内容: '{}'", 
            event.sessionId.value, event.output.length, event.output.replace("\n", "\\n").replace("\r", "\\r"))
        
        terminalOutputPublisher.publishOutput(event.sessionId, event.output)
        
        logger.debug("终端输出事件处理完成 - 会话ID: {}", event.sessionId.value)
    }

    override fun canHandle(eventType: String): Boolean {
        return eventType == "TerminalOutputEvent"
    }
}
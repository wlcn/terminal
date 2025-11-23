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
        // 优化：减少日志开销，仅在调试模式下记录详细信息
        if (logger.isDebugEnabled) {
            logger.debug("处理终端输出事件 - 会话ID: {}, 输出长度: {}", 
                event.sessionId.value, event.output.length)
        }
        
        // 立即发布输出，减少延迟
        terminalOutputPublisher.publishOutput(event.sessionId, event.output)
        
        if (logger.isTraceEnabled) {
            logger.trace("终端输出事件处理完成 - 会话ID: {}", event.sessionId.value)
        }
    }

    override fun canHandle(eventType: String): Boolean {
        return eventType == "TerminalOutputEvent"
    }
}
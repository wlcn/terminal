package org.now.terminal.infrastructure.eventbus

import org.now.terminal.infrastructure.logging.TerminalLogger
import org.now.terminal.shared.events.Event
import org.now.terminal.shared.events.EventHandler

/**
 * 日志事件处理器 - 默认打印所有事件
 * 用于调试和监控事件流
 */
class LogEventHandler : EventHandler<Event> {
    private val logger = TerminalLogger.getLogger(LogEventHandler::class.java)
    
    override suspend fun handle(event: Event) {
        logger.info("事件日志 - 类型: {}, ID: {}, 时间: {}, 聚合根: {}/{}, 版本: {}",
            event.eventType,
            event.eventId,
            event.occurredAt,
            event.aggregateType ?: "N/A",
            event.aggregateId ?: "N/A",
            event.version
        )
        
        // 记录事件的详细信息（调试级别）
        logger.debug("事件详情: {}", event)
    }
    
    override fun canHandle(eventType: String): Boolean {
        // 这个处理器可以处理所有类型的事件
        return true
    }
}
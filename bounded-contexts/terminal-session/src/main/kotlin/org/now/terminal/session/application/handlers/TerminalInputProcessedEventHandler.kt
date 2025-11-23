package org.now.terminal.session.application.handlers

import org.now.terminal.infrastructure.logging.TerminalLogger
import org.now.terminal.session.domain.events.TerminalInputProcessedEvent
import org.now.terminal.session.domain.repositories.TerminalSessionRepository
import org.now.terminal.shared.events.EventHandler
import kotlinx.coroutines.*

/**
 * TerminalInputProcessedEvent事件处理器
 * 负责处理终端输入处理完成事件，用于更新活跃会话统计等
 */
class TerminalInputProcessedEventHandler(
    private val sessionRepository: TerminalSessionRepository,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : EventHandler<TerminalInputProcessedEvent> {
    
    private val logger = TerminalLogger.getLogger(TerminalInputProcessedEventHandler::class.java)
    
    override suspend fun handle(event: TerminalInputProcessedEvent) {
        logger.debug("处理终端输入完成事件 - 会话ID: {}, 输入长度: {}", 
            event.sessionId.value, event.input.length)
        
        // 这里可以添加处理逻辑，比如：
        // 1. 更新会话的最后活动时间
        // 2. 记录用户输入统计
        // 3. 触发其他相关业务逻辑
        
        // 示例：检查会话是否仍然活跃
        val session = sessionRepository.findById(event.sessionId)
        if (session != null) {
            logger.debug("会话 {} 仍然活跃，输入事件已处理", event.sessionId.value)
        } else {
            logger.warn("会话 {} 不存在，但收到了输入事件", event.sessionId.value)
        }
    }
    
    override fun canHandle(eventType: String): Boolean {
        return eventType == "TerminalInputProcessedEvent"
    }
}
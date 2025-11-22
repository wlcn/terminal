package org.now.terminal.session.application.handlers

import org.now.terminal.infrastructure.logging.TerminalLogger
import org.now.terminal.session.domain.events.SessionCreatedEvent
import org.now.terminal.session.domain.repositories.TerminalSessionRepository
import org.now.terminal.shared.events.EventHandler
import kotlinx.coroutines.*

/**
 * SessionCreated事件处理器
 * 负责在会话创建后启动输出监听协程
 */
class SessionCreatedEventHandler(
    private val sessionRepository: TerminalSessionRepository,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : EventHandler<SessionCreatedEvent> {
    
    private val logger = TerminalLogger.getLogger(SessionCreatedEventHandler::class.java)
    
    override suspend fun handle(event: SessionCreatedEvent) {
        logger.info("处理SessionCreated事件 - 会话ID: {}", event.sessionId)
        
        // 从仓库获取会话
        val session = sessionRepository.findById(event.sessionId)
        if (session == null) {
            logger.warn("会话不存在，无法启动输出监听 - 会话ID: {}", event.sessionId)
            return
        }
        
        // 启动输出监听协程
        startOutputListener(session)
    }
    
    /**
     * 启动输出监听协程
     */
    private fun startOutputListener(session: org.now.terminal.session.domain.entities.TerminalSession) {
        coroutineScope.launch {
            try {
                logger.debug("启动输出监听协程 - 会话ID: {}", session.sessionId)
                session.startOutputListener()
            } catch (e: Exception) {
                logger.error("输出监听协程异常 - 会话ID: {}, 错误: {}", session.sessionId, e.message, e)
            }
        }
    }
    
    override fun canHandle(eventType: String): Boolean {
        return eventType == "SessionCreated"
    }
    
    /**
     * 清理资源
     */
    fun close() {
        coroutineScope.cancel()
        logger.info("SessionCreatedEventHandler已关闭")
    }
}
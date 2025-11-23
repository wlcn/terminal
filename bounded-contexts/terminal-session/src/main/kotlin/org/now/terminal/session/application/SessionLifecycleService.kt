package org.now.terminal.session.application

import kotlinx.coroutines.runBlocking
import org.now.terminal.infrastructure.eventbus.EventBus
import org.now.terminal.infrastructure.logging.TerminalLogger
import org.now.terminal.session.domain.entities.SessionStatistics
import org.now.terminal.session.domain.entities.TerminalSession
import org.now.terminal.session.domain.repositories.TerminalSessionRepository
import org.now.terminal.session.domain.services.ProcessFactory
import org.now.terminal.session.domain.services.TerminalSessionService
import org.now.terminal.shared.valueobjects.SessionId
import org.now.terminal.shared.valueobjects.UserId
import org.now.terminal.session.domain.valueobjects.TerminationReason
import org.now.terminal.session.domain.valueobjects.PtyConfiguration
import org.now.terminal.session.domain.valueobjects.TerminalSize
import org.now.terminal.infrastructure.eventbus.DynamicEventHandlerRegistry
import org.now.terminal.infrastructure.eventbus.DynamicEventHandlerRegistryFactory
import org.now.terminal.session.application.handlers.SessionCreatedEventHandler
import org.now.terminal.session.application.handlers.TerminalOutputEventHandler
import org.now.terminal.session.application.handlers.TerminalInputProcessedEventHandler
import org.now.terminal.session.domain.events.SessionCreatedEvent
import org.now.terminal.session.domain.events.TerminalOutputEvent
import org.now.terminal.session.domain.events.TerminalInputProcessedEvent
import org.now.terminal.shared.events.EventHandler
import java.util.concurrent.ConcurrentHashMap
/**
 * 会话生命周期管理服务
 * 实现TerminalSessionService接口
 */
class SessionLifecycleService(
    private val eventBus: EventBus,
    private val sessionRepository: TerminalSessionRepository,
    private val processFactory: ProcessFactory,
    private val sessionCreatedEventHandler: SessionCreatedEventHandler,
    private val terminalOutputEventHandler: TerminalOutputEventHandler,
    private val terminalInputProcessedEventHandler: TerminalInputProcessedEventHandler
) : TerminalSessionService {
    
    // 动态事件处理器注册服务，供业务层使用
    private val dynamicHandlerRegistry = DynamicEventHandlerRegistryFactory.create(eventBus)
    
    // 会话处理器映射，用于管理每个会话的独立处理器实例
    private val sessionHandlerMap = ConcurrentHashMap<SessionId, SessionHandlerMapping>()
    
    private val logger = TerminalLogger.getLogger(SessionLifecycleService::class.java)
    
    /**
     * 创建新的终端会话
     */
    override suspend fun createSession(
        userId: UserId,
        ptyConfig: PtyConfiguration
    ): SessionId {
        logger.info("开始创建终端会话 - 用户ID: {}, PTY配置: {}", userId, ptyConfig)
        
        // 检查用户会话数限制
        val maxSessionsPerUser = org.now.terminal.infrastructure.configuration.ConfigurationManager.getTerminalConfig().maxSessionsPerUser
        val activeSessions = listActiveSessions(userId)
        if (activeSessions.size >= maxSessionsPerUser) {
            logger.warn("用户已达到最大会话数限制 - 用户ID: {}, 当前会话数: {}, 最大限制: {}", 
                userId, activeSessions.size, maxSessionsPerUser)
            throw IllegalStateException("User has reached maximum session limit: $maxSessionsPerUser")
        }
        
        val sessionId = SessionId.generate()
        val session = TerminalSession(
            sessionId = sessionId,
            userId = userId,
            ptyConfig = ptyConfig,
            processFactory = processFactory
        )
        
        session.start()
        sessionRepository.save(session)
        
        // 注册会话相关的事件处理器
        registerSessionEventHandlers(sessionId)
        
        // 异步发布领域事件（包括初始输出事件）
        session.getDomainEvents().forEach { event ->
            eventBus.publish(event)
        }
        
        logger.info("终端会话创建成功 - 会话ID: {}, 用户ID: {}", sessionId, userId)
        return sessionId
    }
    
    /**
     * 终止会话
     */
    override suspend fun terminateSession(sessionId: SessionId, reason: TerminationReason) {
        logger.info("开始终止会话 - 会话ID: {}, 原因: {}", sessionId, reason)
        
        val session = sessionRepository.findById(sessionId)
            ?: throw IllegalArgumentException("Session not found: $sessionId")
        
        if (!session.canTerminate()) {
            logger.warn("会话无法终止 - 会话ID: {}, 当前状态: {}", sessionId, session.getStatus())
            throw IllegalStateException("Session cannot be terminated: $sessionId")
        }
        
        session.terminate(reason)
        sessionRepository.save(session)
        
        // 取消注册会话相关的事件处理器
        unregisterSessionEventHandlers(sessionId)
        
        // 异步发布领域事件
        session.getDomainEvents().forEach { event ->
            eventBus.publish(event)
        }
        
        logger.info("会话终止成功 - 会话ID: {}, 原因: {}", sessionId, reason)
    }
    
    /**
     * 处理终端输入
     */
    override suspend fun handleInput(sessionId: SessionId, input: String) {
        logger.info("📥 开始处理用户输入 - 会话ID: {}, 输入长度: {}, 输入内容: '{}'", 
            sessionId, input.length, input.replace("\n", "\\n").replace("\r", "\\r"))
        
        val session = sessionRepository.findById(sessionId)
            ?: throw IllegalArgumentException("Session not found: $sessionId")
        
        if (!session.canReceiveInput()) {
            logger.warn("⚠️ 会话无法处理输入 - 会话ID: {}, 当前状态: {}", sessionId, session.getStatus())
            throw IllegalStateException("Session cannot handle input: $sessionId")
        }
        
        session.handleInput(input)
        sessionRepository.save(session)
        
        // 异步发布领域事件
        session.getDomainEvents().forEach { event ->
            eventBus.publish(event)
        }
        
        logger.info("✅ 用户输入处理完成 - 会话ID: {}, 输入长度: {}", sessionId, input.length)
    }
    
    /**
     * 调整终端尺寸
     */
    override suspend fun resizeTerminal(sessionId: SessionId, size: TerminalSize) {
        val session = sessionRepository.findById(sessionId)
            ?: throw IllegalArgumentException("Session not found: $sessionId")
        
        session.resize(size)
        sessionRepository.save(session)
        
        // 异步发布领域事件
        session.getDomainEvents().forEach { event ->
            eventBus.publish(event)
        }
    }
    
    /**
     * 列出活跃会话
     */
    override suspend fun listActiveSessions(userId: UserId): List<TerminalSession> {
        return sessionRepository.findByUserId(userId)
            .filter { it.isAlive() }
    }
    
    /**
     * 读取会话输出
     */
    override suspend fun readOutput(sessionId: SessionId): String {
        logger.info("📤 开始读取终端输出 - 会话ID: {}", sessionId)
        
        val session = sessionRepository.findById(sessionId)
            ?: throw IllegalArgumentException("Session not found: $sessionId")
        
        if (!session.isAlive()) {
            logger.debug("📭 会话已终止或无输出 - 会话ID: {}", sessionId)
            return ""
        }
        
        val output = session.readOutput()
        sessionRepository.save(session)
        
        // 异步发布领域事件
        session.getDomainEvents().forEach { event ->
            eventBus.publish(event)
        }
        
        logger.info("✅ 终端输出读取完成 - 会话ID: {}, 输出长度: {}, 输出内容: '{}'", 
            sessionId, output.length, output.replace("\n", "\\n").replace("\r", "\\r"))
        return output
    }
    
    /**
     * 获取会话统计信息
     */
    override suspend fun getSessionStatistics(sessionId: SessionId): SessionStatistics {
        val session = sessionRepository.findById(sessionId)
            ?: throw IllegalArgumentException("Session not found: $sessionId")
        
        return session.getStatistics()
    }
    
    /**
     * 会话处理器映射数据结构
     */
    private data class SessionHandlerMapping(
        val outputHandler: EventHandler<TerminalOutputEvent>,
        val inputProcessedHandler: EventHandler<TerminalInputProcessedEvent>,
        val createdHandler: EventHandler<SessionCreatedEvent>
    )
    

    
    /**
     * 检查会话是否存在且活跃
     */
    override suspend fun isSessionActive(sessionId: SessionId): Boolean {
        val session = sessionRepository.findById(sessionId)
        return session?.isAlive() ?: false
    }
    
    /**
     * 获取会话配置
     */
    override suspend fun getSessionConfiguration(sessionId: SessionId): PtyConfiguration {
        val session = sessionRepository.findById(sessionId)
            ?: throw IllegalArgumentException("Session not found: $sessionId")
        
        return session.getConfiguration()
    }
    
    /**
     * 获取动态事件处理器注册服务实例
     * 供业务层根据需要动态注册事件处理器
     */
    fun getDynamicHandlerRegistry(): DynamicEventHandlerRegistry {
        return dynamicHandlerRegistry
    }
    
    /**
     * 强制终止所有用户会话
     */
    override suspend fun terminateAllUserSessions(userId: UserId, reason: TerminationReason) {
        val userSessions = sessionRepository.findByUserId(userId)
        userSessions.forEach { session ->
            if (session.canTerminate()) {
                session.terminate(reason)
                sessionRepository.delete(session.sessionId)
                
                // 取消注册会话相关的事件处理器
                unregisterSessionEventHandlers(session.sessionId)
                
                // 异步发布领域事件
                session.getDomainEvents().forEach { event ->
                    eventBus.publish(event)
                }
            }
        }
    }
    
    /**
     * 注册会话相关的事件处理器
     */
    private suspend fun registerSessionEventHandlers(sessionId: SessionId) {
        logger.info("注册会话事件处理器 - 会话ID: {}", sessionId)
        
        // 为每个会话创建独立的事件处理器实例
        val sessionOutputEventHandler = TerminalOutputEventHandler(terminalOutputPublisher)
        val sessionInputProcessedEventHandler = TerminalInputProcessedEventHandler()
        val sessionCreatedEventHandler = SessionCreatedEventHandler()
        
        // 注册终端输出事件处理器
        dynamicHandlerRegistry.registerHandler(TerminalOutputEvent::class.java, sessionOutputEventHandler)
        
        // 注册终端输入处理完成事件处理器
        dynamicHandlerRegistry.registerHandler(TerminalInputProcessedEvent::class.java, sessionInputProcessedEventHandler)
        
        // 注册会话创建事件处理器
        dynamicHandlerRegistry.registerHandler(SessionCreatedEvent::class.java, sessionCreatedEventHandler)
        
        // 存储会话处理器映射，用于后续取消注册
        sessionHandlerMap[sessionId] = SessionHandlerMapping(
            outputHandler = sessionOutputEventHandler,
            inputProcessedHandler = sessionInputProcessedEventHandler,
            createdHandler = sessionCreatedEventHandler
        )
        
        logger.info("会话事件处理器注册完成 - 会话ID: {}", sessionId)
    }
    
    /**
     * 取消注册会话相关的事件处理器
     */
    private suspend fun unregisterSessionEventHandlers(sessionId: SessionId) {
        logger.info("取消注册会话事件处理器 - 会话ID: {}", sessionId)
        
        val handlerMapping = sessionHandlerMap[sessionId]
        if (handlerMapping != null) {
            // 取消注册终端输出事件处理器
            dynamicHandlerRegistry.unregisterHandler(TerminalOutputEvent::class.java, handlerMapping.outputHandler)
            
            // 取消注册终端输入处理完成事件处理器
            dynamicHandlerRegistry.unregisterHandler(TerminalInputProcessedEvent::class.java, handlerMapping.inputProcessedHandler)
            
            // 取消注册会话创建事件处理器
            dynamicHandlerRegistry.unregisterHandler(SessionCreatedEvent::class.java, handlerMapping.createdHandler)
            
            // 从映射中移除
            sessionHandlerMap.remove(sessionId)
            
            logger.info("会话事件处理器取消注册完成 - 会话ID: {}", sessionId)
        } else {
            logger.warn("未找到会话处理器映射 - 会话ID: {}", sessionId)
        }
    }
}
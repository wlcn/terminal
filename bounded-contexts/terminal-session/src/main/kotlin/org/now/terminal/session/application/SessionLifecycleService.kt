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
/**
 * 会话生命周期管理服务
 * 实现TerminalSessionService接口
 */
class SessionLifecycleService(
    private val eventBus: EventBus,
    private val sessionRepository: TerminalSessionRepository,
    private val processFactory: ProcessFactory
) : TerminalSessionService {
    
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
        
        // 异步发布领域事件
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
        logger.info("开始处理用户输入 - 会话ID: {}, 输入长度: {}", sessionId, input.length)
        
        val session = sessionRepository.findById(sessionId)
            ?: throw IllegalArgumentException("Session not found: $sessionId")
        
        if (!session.canReceiveInput()) {
            logger.warn("会话无法处理输入 - 会话ID: {}, 当前状态: {}", sessionId, session.getStatus())
            throw IllegalStateException("Session cannot handle input: $sessionId")
        }
        
        session.handleInput(input)
        sessionRepository.save(session)
        
        // 异步发布领域事件
        session.getDomainEvents().forEach { event ->
            eventBus.publish(event)
        }
        
        logger.info("用户输入处理完成 - 会话ID: {}, 输入长度: {}", sessionId, input.length)
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
        val session = sessionRepository.findById(sessionId)
            ?: throw IllegalArgumentException("Session not found: $sessionId")
        
        val output = session.readOutput()
        sessionRepository.save(session)
        
        // 异步发布领域事件
        session.getDomainEvents().forEach { event ->
            eventBus.publish(event)
        }
        
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
     * 强制终止所有用户会话
     */
    override suspend fun terminateAllUserSessions(userId: UserId, reason: TerminationReason) {
        val userSessions = sessionRepository.findByUserId(userId)
        userSessions.forEach { session ->
            if (session.canTerminate()) {
                session.terminate(reason)
                sessionRepository.delete(session.sessionId)
                
                // 异步发布领域事件
                session.getDomainEvents().forEach { event ->
                    eventBus.publish(event)
                }
            }
        }
    }
    
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
}
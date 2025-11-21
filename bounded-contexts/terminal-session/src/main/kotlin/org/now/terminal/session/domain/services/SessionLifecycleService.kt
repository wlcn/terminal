package org.now.terminal.session.domain.services

import org.now.terminal.session.domain.aggregates.SessionAggregate
import org.now.terminal.session.domain.aggregates.PtyConfiguration
import org.now.terminal.session.domain.repositories.TerminalSessionRepository
import org.now.terminal.session.domain.valueobjects.TerminalCommand
import org.now.terminal.session.domain.valueobjects.TerminalSize
import org.now.terminal.shared.valueobjects.SessionId
import org.now.terminal.shared.valueobjects.UserId

/**
 * 会话生命周期领域服务
 * 处理跨聚合的业务逻辑
 */
class SessionLifecycleService(
    private val sessionRepository: TerminalSessionRepository,
    private val eventPublisher: DomainEventPublisher
) {
    
    /**
     * 创建新会话
     */
    fun createSession(userId: UserId, configuration: PtyConfiguration): SessionAggregate {
        val session = SessionAggregate(
            sessionId = SessionId.generate(),
            userId = userId
        )
        
        session.createProcess(configuration)
        
        val savedSession = sessionRepository.save(session)
        
        // 发布领域事件
        session.getPendingEvents().forEach { eventPublisher.publish(it) }
        
        return savedSession
    }
    
    /**
     * 终止会话
     */
    fun terminateSession(sessionId: SessionId, reason: TerminationReason) {
        val session = sessionRepository.findById(sessionId)
            ?: throw SessionNotFoundException(sessionId)
        
        session.terminate(reason)
        sessionRepository.delete(sessionId)
        
        // 发布领域事件
        session.getPendingEvents().forEach { eventPublisher.publish(it) }
    }
    
    /**
     * 处理终端输入
     */
    fun handleTerminalInput(sessionId: SessionId, command: TerminalCommand) {
        val session = sessionRepository.findById(sessionId)
            ?: throw SessionNotFoundException(sessionId)
        
        session.handleInput(command)
        sessionRepository.save(session)
        
        // 发布领域事件
        session.getPendingEvents().forEach { eventPublisher.publish(it) }
    }
    
    /**
     * 调整终端尺寸
     */
    fun resizeTerminal(sessionId: SessionId, newSize: TerminalSize) {
        val session = sessionRepository.findById(sessionId)
            ?: throw SessionNotFoundException(sessionId)
        
        session.resize(newSize)
        sessionRepository.save(session)
        
        // 发布领域事件
        session.getPendingEvents().forEach { eventPublisher.publish(it) }
    }
    
    /**
     * 获取活跃会话列表
     */
    fun getActiveSessions(): List<SessionAggregate> {
        return sessionRepository.findAll()
            .filter { it.getCurrentState() is SessionState.Active }
    }
    
    /**
     * 清理过期会话
     */
    fun cleanupExpiredSessions(timeoutMinutes: Long) {
        val expiredSessions = sessionRepository.findAll()
            .filter { session ->
                when (val state = session.getCurrentState()) {
                    is SessionState.Active -> {
                        val duration = java.time.Duration.between(state.startedAt, java.time.Instant.now())
                        duration.toMinutes() > timeoutMinutes
                    }
                    is SessionState.Created -> {
                        val duration = java.time.Duration.between(state.createdAt, java.time.Instant.now())
                        duration.toMinutes() > timeoutMinutes
                    }
                    is SessionState.Terminated -> false
                }
            }
        
        expiredSessions.forEach { session ->
            session.terminate(TerminationReason.TIMEOUT)
            sessionRepository.delete(session.sessionId)
            
            // 发布领域事件
            session.getPendingEvents().forEach { eventPublisher.publish(it) }
        }
    }
}

/**
 * 会话未找到异常
 */
class SessionNotFoundException(sessionId: SessionId) : 
    RuntimeException("Session not found: $sessionId")

/**
 * 领域事件发布器接口
 */
interface DomainEventPublisher {
    fun publish(event: SessionEvent)
}
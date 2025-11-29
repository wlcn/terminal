package org.now.terminal.boundedcontexts.terminalsession.domain.service

import java.util.UUID
import org.now.terminal.boundedcontexts.terminalsession.domain.InMemoryTerminalSessionRepository
import org.now.terminal.boundedcontexts.terminalsession.domain.TerminalSession
import org.now.terminal.boundedcontexts.terminalsession.domain.TerminalSessionRepository
import org.now.terminal.boundedcontexts.terminalsession.domain.TerminalSessionStatus
import org.now.terminal.boundedcontexts.terminalsession.domain.TerminalSize
import org.now.terminal.boundedcontexts.terminalsession.domain.model.TerminalConfig

class TerminalSessionService(
    private val terminalConfig: TerminalConfig,
    private val terminalSessionRepository: TerminalSessionRepository = InMemoryTerminalSessionRepository(),
    private val terminalProcessManager: TerminalProcessManager? = null
) {
    private val defaultShellType = terminalConfig.defaultShellType
    private val sessionTimeoutMs = terminalConfig.sessionTimeoutMs

    // 会话过期管理器
    private val terminalSessionExpiryManager = TerminalSessionExpiryManager(sessionTimeoutMs, terminalProcessManager)

    fun createSession(
        userId: String,
        title: String?,
        workingDirectory: String,
        shellType: String = defaultShellType,
        size: TerminalSize? = null
    ): TerminalSession {
        val now = System.currentTimeMillis()
        val session = TerminalSession(
            id = UUID.randomUUID().toString(),
            userId = userId,
            title = title,
            workingDirectory = workingDirectory,
            shellType = shellType,
            status = TerminalSessionStatus.ACTIVE,
            terminalSize = size ?: terminalConfig.defaultTerminalSize,
            createdAt = now,
            updatedAt = now,
            lastActiveTime = now,
            expiredAt = now + sessionTimeoutMs
        )
        terminalSessionRepository.save(session)

        // 为新会话启动过期检查
        terminalSessionExpiryManager.startExpiryCheck(session) { expiredSession ->
            // 会话过期回调，从存储中移除
            terminalSessionRepository.deleteById(expiredSession.id)
        }

        return session
    }

    fun getSessionById(id: String): TerminalSession? {
        return terminalSessionRepository.getById(id)?.also {
            // 使用领域模型的方法更新活动时间和过期时间
            val now = System.currentTimeMillis()
            it.updateActivity(now)
            it.updateExpiryTime(sessionTimeoutMs, now)
            terminalSessionRepository.update(it)
        }
    }

    fun getSessionsByUserId(userId: String): List<TerminalSession> {
        return terminalSessionRepository.getByUserId(userId)
    }

    fun getAllSessions(): List<TerminalSession> {
        return terminalSessionRepository.getAll()
    }

    fun resizeTerminal(id: String, columns: Int, rows: Int): TerminalSession? {
        return terminalSessionRepository.getById(id)?.also {
            // 使用领域模型的resize方法
            it.resize(columns, rows)
            terminalSessionRepository.update(it)
        }
    }

    fun terminateSession(id: String, reason: String? = null): TerminalSession? {
        return terminalSessionRepository.getById(id)?.also {
            // 取消过期检查
            terminalSessionExpiryManager.cancelExpiryCheck(id)

            // 使用领域模型的terminate方法
            it.terminate()

            // 清理相关资源
            terminalProcessManager?.terminateProcess(id)

            // 从存储中移除
            terminalSessionRepository.deleteById(id)
        }
    }

    fun updateSessionStatus(id: String, status: TerminalSessionStatus): TerminalSession? {
        return terminalSessionRepository.getById(id)?.also {
            // 使用领域模型的updateStatus方法
            it.updateStatus(status)
            terminalSessionRepository.update(it)
        }
    }

    fun deleteSession(id: String): Boolean {
        // 取消过期检查
        terminalSessionExpiryManager.cancelExpiryCheck(id)

        val session = terminalSessionRepository.deleteById(id)
        if (session != null) {
            // 清理相关资源
            terminalProcessManager?.terminateProcess(id)
        }
        return session != null
    }

    /**
     * 更新session活动时间
     */
    fun updateSessionActivity(session: TerminalSession) {
        val now = System.currentTimeMillis()
        
        // 使用领域模型的方法更新活动时间和过期时间
        session.updateActivity(now)
        session.updateExpiryTime(sessionTimeoutMs, now)

        // 更新存储中的会话
        terminalSessionRepository.update(session)

        // 重新启动过期检查
        terminalSessionExpiryManager.restartExpiryCheck(session) {
            // 会话过期回调，从存储中移除
            terminalSessionRepository.deleteById(session.id)
        }
    }

    /**
     * 关闭服务，清理资源
     */
    fun shutdown() {
        // 关闭会话过期管理器
        terminalSessionExpiryManager.shutdown()
    }
}

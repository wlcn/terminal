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
    private val terminalSessionRepository: TerminalSessionRepository = InMemoryTerminalSessionRepository()
) {
    private val defaultShellType = terminalConfig.defaultShellType
    private val sessionTimeoutMs = terminalConfig.sessionTimeoutMs


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

        return session
    }

    fun getSessionById(id: String): TerminalSession? {
        return terminalSessionRepository.getById(id)?.also {
            updateSessionActivity(it)
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
            it.resize(columns, rows)
            terminalSessionRepository.update(it)
        }
    }

    fun terminateSession(id: String, reason: String? = null): TerminalSession? {
        return terminalSessionRepository.getById(id)?.also {
            // 使用领域模型的terminate方法
            it.terminate()

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
        val session = terminalSessionRepository.deleteById(id)
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
    }


}

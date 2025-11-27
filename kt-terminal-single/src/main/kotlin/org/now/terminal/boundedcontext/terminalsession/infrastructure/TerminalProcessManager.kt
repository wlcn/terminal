package org.now.terminal.boundedcontext.terminalsession.infrastructure

import org.now.terminal.boundedcontext.terminalsession.domain.TerminalSession
import org.now.terminal.boundedcontext.terminalsession.domain.valueobjects.TerminalSessionId
import java.util.concurrent.ConcurrentHashMap

/**
 * Terminal process manager - infrastructure service
 * 
 * Manages the mapping between TerminalSession (domain) and PTY4J processes (infrastructure).
 * This service handles the technical details of process creation, management, and cleanup.
 */
class TerminalProcessManager(
    private val processFactory: ProcessFactory
) {
    
    private val sessionProcessMap = ConcurrentHashMap<TerminalSessionId, TerminalProcess>()
    
    /**
     * Create a new process for a terminal session
     */
    fun createProcessForSession(session: TerminalSession): TerminalProcess {
        val process = processFactory.createProcess(
            shellType = session.configuration.shellType,
            workingDirectory = session.configuration.workingDirectory,
            environment = session.configuration.environmentVariables,
            terminalSize = session.configuration.terminalSize
        )
        
        sessionProcessMap[session.id] = process
        return process
    }
    
    /**
     * Get the process associated with a session
     */
    fun getProcessForSession(sessionId: TerminalSessionId): TerminalProcess? {
        return sessionProcessMap[sessionId]
    }
    
    /**
     * Check if a session has an active process
     */
    fun hasActiveProcess(sessionId: TerminalSessionId): Boolean {
        val process = sessionProcessMap[sessionId]
        return process?.isAlive ?: false
    }
    
    /**
     * Terminate the process for a session
     */
    fun terminateProcessForSession(sessionId: TerminalSessionId) {
        val process = sessionProcessMap[sessionId]
        process?.terminate()
        sessionProcessMap.remove(sessionId)
    }
    
    /**
     * Resize the terminal for a session's process
     */
    fun resizeTerminal(sessionId: TerminalSessionId, rows: Int, cols: Int) {
        val process = sessionProcessMap[sessionId]
        process?.resizeTerminal(rows, cols)
    }
    
    /**
     * Clean up all processes (e.g., on application shutdown)
     */
    fun cleanupAllProcesses() {
        sessionProcessMap.values.forEach { it.terminate() }
        sessionProcessMap.clear()
    }
    
    /**
     * Get all active session IDs
     */
    fun getActiveSessionIds(): Set<TerminalSessionId> {
        return sessionProcessMap.keys.filter { hasActiveProcess(it) }.toSet()
    }
    
    /**
     * Get statistics about active processes
     */
    fun getProcessStatistics(): ProcessStatistics {
        val activeCount = sessionProcessMap.values.count { it.isAlive }
        val terminatedCount = sessionProcessMap.values.count { !it.isAlive }
        
        return ProcessStatistics(
            totalProcesses = sessionProcessMap.size,
            activeProcesses = activeCount,
            terminatedProcesses = terminatedCount
        )
    }
}
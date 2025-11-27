package org.now.terminal.interfaces.terminalsession.controllers

import org.now.terminal.interfaces.web.responses.ApiResponse
import org.now.terminal.interfaces.terminalsession.dto.requests.CreateSessionRequest
import org.now.terminal.interfaces.terminalsession.dto.responses.SessionResponse

/**
 * Terminal session management controller interface
 * Defines terminal session-related API interfaces, implementations are placed in infrastructure layer
 */
interface SessionController {
    
    /**
     * Create a new terminal session
     */
    fun createSession(request: CreateSessionRequest): ApiResponse<SessionResponse>
    
    /**
     * Get session details by ID
     */
    fun getSessionById(sessionId: String): ApiResponse<SessionResponse>
    
    /**
     * Get all active sessions
     */
    fun getActiveSessions(): ApiResponse<List<SessionResponse>>
    
    /**
     * Terminate a session
     */
    fun terminateSession(sessionId: String): ApiResponse<Unit>
    
    /**
     * Get session logs
     */
    fun getSessionLogs(sessionId: String): ApiResponse<List<String>>
}
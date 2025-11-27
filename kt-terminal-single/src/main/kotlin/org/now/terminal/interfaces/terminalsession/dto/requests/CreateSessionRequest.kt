package org.now.terminal.interfaces.terminalsession.dto.requests

/**
 * Create terminal session request DTO
 */
interface CreateSessionRequest {
    val userId: String
    val terminalType: String
    val host: String
    val port: Int
    val username: String
    val password: String?
    val privateKey: String?
}
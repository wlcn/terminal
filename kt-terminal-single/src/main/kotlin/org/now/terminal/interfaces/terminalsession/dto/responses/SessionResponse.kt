package org.now.terminal.interfaces.terminalsession.dto.responses

/**
 * Terminal session response DTO
 */
interface SessionResponse {
    val id: String
    val userId: String
    val terminalType: String
    val host: String
    val port: Int
    val status: String
    val createdAt: Long
    val lastActivityAt: Long?
    val connectionInfo: ConnectionInfo?
}

/**
 * Connection information DTO
 */
interface ConnectionInfo {
    val protocol: String
    val encryption: String
    val clientIp: String
}
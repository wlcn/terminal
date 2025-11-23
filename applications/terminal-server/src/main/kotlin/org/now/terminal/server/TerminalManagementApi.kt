package org.now.terminal.server

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import org.koin.ktor.plugin.koin
import org.slf4j.LoggerFactory
import org.now.terminal.shared.valueobjects.SessionId
import org.now.terminal.shared.valueobjects.UserId
import org.now.terminal.session.domain.valueobjects.TerminalSize
import org.now.terminal.session.domain.valueobjects.PtyConfiguration
import org.now.terminal.session.domain.valueobjects.TerminalCommand
import org.now.terminal.session.domain.valueobjects.TerminationReason
import org.now.terminal.session.application.usecases.CreateSessionUseCase
import org.now.terminal.session.application.usecases.TerminateSessionUseCase
import org.now.terminal.session.application.usecases.ResizeTerminalUseCase
import org.now.terminal.session.application.usecases.ListActiveSessionsUseCase
import org.now.terminal.infrastructure.configuration.ConfigurationManager

/**
 * 终端管理API配置
 * 处理会话管理、尺寸调整、会话终止等操作
 */
object TerminalManagementApi {
    
    /**
     * 配置管理API端点
     */
    fun Application.configureManagementApi() {
        val koin = koin()
        val createSessionUseCase = koin.get<CreateSessionUseCase>()
        val terminateSessionUseCase = koin.get<TerminateSessionUseCase>()
        val resizeTerminalUseCase = koin.get<ResizeTerminalUseCase>()
        val listActiveSessionsUseCase = koin.get<ListActiveSessionsUseCase>()
        val logger = LoggerFactory.getLogger("TerminalManagementApi")
        
        routing {
            // 创建新会话API
            post("/api/sessions") {
                try {
                    val sessionId = createSessionUseCase.execute()
                    
                    logger.info("✅ 通过API创建新会话: {}", sessionId.value)
                    call.respond(mapOf(
                        "sessionId" to sessionId.value,
                        "status" to "created"
                    ))
                } catch (e: Exception) {
                    logger.error("❌ 创建会话失败: {}", e.message)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to create session"))
                }
            }
            
            // 调整终端尺寸API
            put("/api/sessions/{sessionId}/resize") {
                try {
                    val sessionIdParam = call.parameters["sessionId"] ?: throw IllegalArgumentException("Session ID required")
                    val sessionId = SessionId.create(sessionIdParam)
                    
                    val request = call.receive<Map<String, Int>>()
                    val columns = request["columns"] ?: 80
                    val rows = request["rows"] ?: 24
                    val size = TerminalSize(columns, rows)
                    
                    resizeTerminalUseCase.execute(sessionId, size)
                    
                    logger.info("📐 通过API调整会话 {} 尺寸: {}x{}", sessionId.value, columns, rows)
                    call.respond(mapOf(
                        "sessionId" to sessionId.value,
                        "columns" to columns,
                        "rows" to rows,
                        "status" to "resized"
                    ))
                } catch (e: IllegalArgumentException) {
                    logger.error("❌ 调整尺寸参数错误: {}", e.message)
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid parameters"))
                } catch (e: Exception) {
                    logger.error("❌ 调整尺寸失败: {}", e.message)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to resize terminal"))
                }
            }
            
            // 终止会话API
            delete("/api/sessions/{sessionId}") {
                try {
                    val sessionIdParam = call.parameters["sessionId"] ?: throw IllegalArgumentException("Session ID required")
                    val sessionId = SessionId.create(sessionIdParam)
                    
                    val reason = call.request.queryParameters["reason"] ?: "USER_REQUESTED"
                    val terminationReason = when (reason) {
                        "USER_REQUESTED" -> TerminationReason.USER_REQUESTED
                        "TIMEOUT" -> TerminationReason.TIMEOUT
                        "SYSTEM_ERROR" -> TerminationReason.SYSTEM_ERROR
                        "PROCESS_ERROR" -> TerminationReason.PROCESS_ERROR
                        "NORMAL" -> TerminationReason.NORMAL
                        else -> TerminationReason.USER_REQUESTED
                    }
                    
                    terminateSessionUseCase.execute(sessionId, terminationReason)
                    
                    logger.info("🛑 通过API终止会话 {} - 原因: {}", sessionId.value, reason)
                    call.respond(mapOf(
                        "sessionId" to sessionId.value,
                        "reason" to reason,
                        "status" to "terminated"
                    ))
                } catch (e: IllegalArgumentException) {
                    logger.error("❌ 终止会话参数错误: {}", e.message)
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid parameters"))
                } catch (e: Exception) {
                    logger.error("❌ 终止会话失败: {}", e.message)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to terminate session"))
                }
            }
            
            // 获取会话列表API
            get("/api/sessions") {
                try {
                    val defaultUserId = UserId.generate()
                    val sessions = listActiveSessionsUseCase.execute(defaultUserId)
                    
                    logger.info("📋 通过API获取活跃会话列表 - 会话数量: {}", sessions.size)
                    call.respond(mapOf(
                        "sessions" to sessions.map { it.sessionId.value },
                        "count" to sessions.size
                    ))
                } catch (e: Exception) {
                    logger.error("❌ 获取会话列表失败: {}", e.message)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to list sessions"))
                }
            }
        }
    }
}
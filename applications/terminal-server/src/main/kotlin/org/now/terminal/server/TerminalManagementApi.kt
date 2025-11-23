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
import org.now.terminal.server.api.models.*

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
                    // 直接调用用例，由用例处理默认逻辑
                    val sessionId = createSessionUseCase.execute()
                    
                    // 获取配置管理器中的shell类型信息
                    val terminalConfig = org.now.terminal.infrastructure.configuration.ConfigurationManager.getTerminalConfig()
                    val shellType = try {
                        org.now.terminal.session.domain.valueobjects.ShellType.valueOf(terminalConfig.pty.shellType.uppercase())
                    } catch (e: IllegalArgumentException) {
                        org.now.terminal.session.domain.valueobjects.ShellType.AUTO
                    }
                    
                    logger.info("✅ 通过API创建新会话: {}, shell类型: {}", sessionId.value, shellType)
                    call.respond(CreateSessionResponse(
                        sessionId = sessionId.value,
                        status = "created",
                        shellType = shellType
                    ))
                } catch (e: Exception) {
                    logger.error("❌ 创建会话失败: {}", e.message)
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Failed to create session"))
                }
            }
            
            // 调整终端尺寸API
            put("/api/sessions/{sessionId}/resize") {
                try {
                    val sessionIdParam = call.parameters["sessionId"] ?: throw IllegalArgumentException("Session ID required")
                    
                    val request = call.receive<ResizeTerminalRequest>()
                    val size = TerminalSize(request.columns, request.rows)
                    
                    resizeTerminalUseCase.execute(sessionIdParam, size)
                    
                    logger.info("📐 通过API调整会话 {} 尺寸: {}x{}", sessionIdParam, request.columns, request.rows)
                    call.respond(ResizeTerminalResponse(
                        sessionId = sessionIdParam,
                        columns = request.columns,
                        rows = request.rows,
                        status = "resized"
                    ))
                } catch (e: IllegalArgumentException) {
                    logger.error("❌ 调整尺寸参数错误: {}", e.message)
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid parameters"))
                } catch (e: Exception) {
                    logger.error("❌ 调整尺寸失败: {}", e.message)
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Failed to resize terminal"))
                }
            }
            
            // 终止会话API
            delete("/api/sessions/{sessionId}") {
                try {
                    val sessionIdParam = call.parameters["sessionId"] ?: throw IllegalArgumentException("Session ID required")
                    
                    terminateSessionUseCase.execute(sessionIdParam, TerminationReason.USER_REQUESTED)
                    
                    logger.info("❌ 通过API终止会话: {}", sessionIdParam)
                    call.respond(TerminateSessionResponse(
                        sessionId = sessionIdParam,
                        reason = "user_request",
                        status = "terminated"
                    ))
                } catch (e: IllegalArgumentException) {
                    logger.error("❌ 终止会话参数错误: {}", e.message)
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid session ID"))
                } catch (e: Exception) {
                    logger.error("❌ 终止会话失败: {}", e.message)
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Failed to terminate session"))
                }
            }
            
            // 获取会话列表API
            get("/api/sessions") {
                try {
                    // 直接调用用例，由用例处理业务逻辑
                    val sessions = listActiveSessionsUseCase.execute()
                    
                    logger.info("📋 通过API获取活跃会话列表 - 会话数量: {}", sessions.size)
                    
                    // 使用数据类简化响应
                    val response = SessionListResponse(
                        sessions = sessions.map { it.sessionId.value },
                        count = sessions.size
                    )
                    
                    call.respond(response)
                } catch (e: Exception) {
                    logger.error("❌ 获取会话列表失败: {}", e.message)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to list sessions"))
                }
            }
        }
    }
}
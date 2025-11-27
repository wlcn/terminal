package org.now.terminal.interfaces.user.dto.requests

import org.now.terminal.boundedcontext.user.application.usecase.CreateUserCommand

/**
 * Create user request DTO - 直接使用业务模块的命令类
 */
typealias CreateUserRequest = CreateUserCommand
package org.now.terminal.session.di

import org.koin.core.module.Module
import org.koin.dsl.module
import org.now.terminal.infrastructure.eventbus.eventBusModule
import org.now.terminal.session.application.SessionLifecycleService
import org.now.terminal.session.application.handlers.TerminalOutputEventHandler
import org.now.terminal.session.application.usecases.*
import org.now.terminal.session.domain.repositories.TerminalSessionRepository
import org.now.terminal.session.domain.services.ProcessFactory
import org.now.terminal.session.domain.services.TerminalSessionService
import org.now.terminal.session.infrastructure.process.Pty4jProcessFactory
import org.now.terminal.session.infrastructure.repositories.InMemoryTerminalSessionRepository

/**
 * Terminal Session模块的Koin依赖注入配置
 * 配置终端会话相关的服务和用例
 */
val terminalSessionModule: Module = module {
    
    // 包含EventBus模块的依赖
    includes(eventBusModule)
    
    // 基础设施层
    single<TerminalSessionRepository> { InMemoryTerminalSessionRepository() }
    single<ProcessFactory> { Pty4jProcessFactory() }
    
    // 应用层 - 用例
    single { CreateSessionUseCase(get()) }
    single { HandleInputUseCase(get()) }
    single { TerminateSessionUseCase(get()) }
    single { ResizeTerminalUseCase(get()) }
    single { ListActiveSessionsUseCase(get()) }
    
    // 应用层 - 事件处理器
    single { TerminalOutputEventHandler(get()) }
    
    // 应用层 - 服务
    single<TerminalSessionService> { SessionLifecycleService(get(), get(), get()) }
    
    // 事件处理器注册服务
    // 注意：事件处理器注册现在由EventBusLifecycleService负责
    
    // TerminalOutputPublisher接口实现由Koin在运行时注入
    // 具体实现由websocket-gateway模块提供
}
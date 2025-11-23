package org.now.terminal.infrastructure.eventbus.di

import org.koin.core.module.Module
import org.koin.dsl.module
import org.now.terminal.infrastructure.configuration.ConfigurationManager
import org.now.terminal.infrastructure.eventbus.EventBus
import org.now.terminal.infrastructure.eventbus.EventBusFactory
import org.now.terminal.infrastructure.eventbus.EventBusLifecycleService

/**
 * EventBus模块的Koin依赖注入配置
 * 提供全局EventBus单例和生命周期管理服务
 */
val eventBusModule: Module = module {
    
    /**
     * 全局EventBus单例
     * 所有业务模块共享同一个EventBus实例
     */
    single<EventBus> { EventBusFactory.createDefault() }
    
    /**
     * EventBus生命周期管理服务
     * 负责EventBus的启动、停止和事件处理器注册
     */
    single { EventBusLifecycleService(get(), get()) }
}
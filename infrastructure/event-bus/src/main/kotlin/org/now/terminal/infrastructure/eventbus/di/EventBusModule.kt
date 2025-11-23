package org.now.terminal.infrastructure.eventbus.di

import org.koin.core.module.Module
import org.koin.dsl.module
import org.now.terminal.infrastructure.eventbus.EventBus
import org.now.terminal.infrastructure.eventbus.EventBusFactory

/**
 * EventBus模块的Koin依赖注入配置
 * 提供全局EventBus单例
 */
val eventBusModule: Module = module {
    
    /**
     * 全局EventBus单例
     * 所有业务模块共享同一个EventBus实例
     */
    single<EventBus> { EventBusFactory.createDefault() }
}
package org.now.terminal.infrastructure.eventbus.di

import org.koin.core.module.Module
import org.koin.dsl.module
import org.now.terminal.infrastructure.eventbus.EventBus
import org.now.terminal.infrastructure.eventbus.EventBusFactory
import org.now.terminal.infrastructure.eventbus.EventBusLifecycleService
import org.now.terminal.shared.events.EventHandler

/**
 * 事件总线模块的Koin依赖注入配置
 */
val eventBusModule: Module = module {
    single<EventBus> { EventBusFactory.createMonitoredEventBus() }
    
    // 注入所有EventHandler实例的集合
    single {
        EventBusLifecycleService(
            eventBus = get(),
            eventHandlers = getAll()
        )
    }
}
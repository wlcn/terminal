package org.now.terminal.infrastructure.eventbus

import org.koin.core.module.Module
import org.koin.dsl.module
import org.now.terminal.shared.events.EventHandler

/**
 * EventBus模块的Koin依赖注入配置
 * 配置事件总线相关的服务和组件
 */
val eventBusModule: Module = module {
    
    /**
     * 事件总线单例
     */
    single<EventBus> { 
        EventBusFactory.createInMemoryEventBus(
            eventHandlers = getAll<EventHandler<*>>().toSet()
        )
    }
    
    /**
     * 事件总线配置单例
     */
    single { EventBusProperties() }
    
    /**
     * 事件总线工厂单例
     */
    single { EventBusFactory }

    /**
     * 事件总线生命周期服务单例
     */
    single {
        EventBusLifecycleService(
            eventBus = get()
        )
    }
}
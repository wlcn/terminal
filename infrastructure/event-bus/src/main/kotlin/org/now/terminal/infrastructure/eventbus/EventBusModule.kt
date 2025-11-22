package org.now.terminal.infrastructure.eventbus

import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * EventBus模块的Koin依赖注入配置
 * 配置事件总线相关的服务和组件
 */
val eventBusModule: Module = module {
    
    /**
     * 事件总线单例
     * 使用默认配置创建内存事件总线
     */
    single<EventBus> { 
        EventBusFactory.createInMemoryEventBus()
    }
    
    /**
     * 事件总线配置单例
     */
    single { EventBusProperties() }
    
    /**
     * 事件总线工厂单例
     */
    single { EventBusFactory }
}
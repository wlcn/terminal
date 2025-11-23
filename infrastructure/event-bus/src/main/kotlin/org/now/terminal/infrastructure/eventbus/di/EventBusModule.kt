package org.now.terminal.infrastructure.eventbus.di

import org.koin.core.module.Module
import org.koin.dsl.module
import org.now.terminal.infrastructure.eventbus.EventBus
import org.now.terminal.infrastructure.eventbus.EventBusFactory
import org.now.terminal.infrastructure.eventbus.LogEventHandler
import org.now.terminal.shared.events.Event

/**
 * EventBus模块的Koin依赖注入配置
 * 提供全局EventBus单例，并自动注册默认事件处理器
 */
val eventBusModule: Module = module {
    
    /**
     * 日志事件处理器 - 默认打印所有事件
     */
    single { LogEventHandler() }
    
    /**
     * 全局EventBus单例
     * 所有业务模块共享同一个EventBus实例
     * 自动注册默认的事件处理器
     */
    single<EventBus> { 
        val eventBus = EventBusFactory.createDefault()
        val logEventHandler = get<LogEventHandler>()
        
        // 启动事件总线
        eventBus.start()
        
        // 注册日志事件处理器（处理所有事件类型）
        // 使用Event::class.java作为事件类型，这样LogEventHandler会处理所有Event的子类
        // 注意：在Koin模块初始化中不能直接调用挂起函数，需要在应用启动后注册
        // 这里只创建EventBus实例，注册将在应用启动流程中完成
        
        eventBus
    }
}
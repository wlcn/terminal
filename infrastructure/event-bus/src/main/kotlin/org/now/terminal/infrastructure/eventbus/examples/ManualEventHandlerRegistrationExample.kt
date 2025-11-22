package org.now.terminal.infrastructure.eventbus.examples

import org.now.terminal.infrastructure.eventbus.EventBus
import org.now.terminal.infrastructure.eventbus.EventBusFactory
import org.now.terminal.session.application.handlers.SessionCreatedEventHandler
import org.now.terminal.session.application.handlers.TerminalOutputEventHandler
import org.now.terminal.session.domain.events.SessionCreatedEvent
import org.now.terminal.session.domain.events.TerminalOutputEvent

/**
 * 手动注册事件处理器示例
 * 展示如何在业务层直接手动注册事件处理器，避免依赖注入框架的局限性
 */
class ManualEventHandlerRegistrationExample {
    
    private val eventBus: EventBus = EventBusFactory.createMonitoredEventBus()
    
    /**
     * 手动注册所有事件处理器
     * 业务层应该在应用启动时调用此方法
     */
    suspend fun registerAllEventHandlers(
        sessionCreatedHandler: SessionCreatedEventHandler,
        terminalOutputHandler: TerminalOutputEventHandler
    ) {
        // 使用 EventBus 提供的批量注册方法
        eventBus.registerHandlers(
            SessionCreatedEvent::class.java to sessionCreatedHandler,
            TerminalOutputEvent::class.java to terminalOutputHandler
        )
        
        // 或者也可以逐个注册
        // eventBus.subscribe(SessionCreatedEvent::class.java, sessionCreatedHandler)
        // eventBus.subscribe(TerminalOutputEvent::class.java, terminalOutputHandler)
    }
    
    /**
     * 在终端会话模块中注册事件处理器
     */
    suspend fun registerTerminalSessionHandlers(
        sessionCreatedHandler: SessionCreatedEventHandler,
        terminalOutputHandler: TerminalOutputEventHandler
    ) {
        println("🔧 开始注册终端会话事件处理器...")
        
        // 注册会话创建事件处理器
        eventBus.subscribe(SessionCreatedEvent::class.java, sessionCreatedHandler)
        println("✅ 注册 SessionCreatedEventHandler")
        
        // 注册终端输出事件处理器
        eventBus.subscribe(TerminalOutputEvent::class.java, terminalOutputHandler)
        println("✅ 注册 TerminalOutputEventHandler")
        
        println("🎉 终端会话事件处理器注册完成")
    }
    
    /**
     * 在应用启动时统一注册所有事件处理器
     */
    suspend fun initializeApplicationEventHandlers(
        handlers: List<Pair<Class<*>, Any>>
    ) {
        println("🚀 应用启动 - 开始注册事件处理器...")
        
        val handlerPairs = handlers.mapNotNull { (eventClass, handler) ->
            if (handler is org.now.terminal.sharedkernel.eventbus.EventHandler<*>) {
                @Suppress("UNCHECKED_CAST")
                eventClass as Class<org.now.terminal.sharedkernel.eventbus.Event> to handler
            } else {
                null
            }
        }
        
        if (handlerPairs.isNotEmpty()) {
            eventBus.registerHandlers(*handlerPairs.toTypedArray())
            println("✅ 批量注册了 ${handlerPairs.size} 个事件处理器")
        } else {
            println("⚠️  未找到有效的事件处理器")
        }
        
        println("🎯 事件处理器注册完成")
    }
}
package org.now.terminal.infrastructure.eventbus

import org.now.terminal.infrastructure.logging.TerminalLogger
import org.now.terminal.shared.events.Event
import org.now.terminal.shared.events.EventHandler

/**
 * 事件总线生命周期服务
 * 负责事件总线的启动、停止和事件处理器的自动注册
 * 
 * 通过构造参数注入的事件处理器集合，在启动时自动注册到事件总线
 */
class EventBusLifecycleService(
    private val eventBus: EventBus
) {
    private val logger = TerminalLogger.getLogger(EventBusLifecycleService::class.java)

    /**
     * 启动事件总线
     * EventBus应该负责自动注册所有事件处理器，Service只负责生命周期管理
     */
    fun start() {
        eventBus.start()
        logger.info("事件总线已启动")
    }

    /**
     * 停止事件总线
     */
    fun stop() {
        eventBus.stop()
        logger.info("事件总线已停止")
    }
}
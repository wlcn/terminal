package org.now.terminal.websocket.di

import org.koin.core.module.Module
import org.koin.dsl.module
import org.now.terminal.websocket.WebSocketOutputPublisher
import org.now.terminal.websocket.WebSocketServer

/**
 * WebSocket Gateway模块的Koin依赖注入配置（业务无关）
 * 配置WebSocket相关的服务和组件
 */
val webSocketModule: Module = module {
    
    /**
     * WebSocket输出发布器单例
     * 用于将终端输出推送到WebSocket客户端
     */
    single<org.now.terminal.session.domain.services.TerminalOutputPublisher> { WebSocketOutputPublisher() }
    
    /**
     * WebSocket服务器单例（业务无关）
     * 负责WebSocket连接管理和消息路由
     */
    single { WebSocketServer(get()) }
}
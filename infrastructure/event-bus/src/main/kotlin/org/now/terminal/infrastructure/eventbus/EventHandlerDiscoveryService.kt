package org.now.terminal.infrastructure.eventbus

import org.koin.core.Koin
import org.koin.core.qualifier.named
import org.now.terminal.infrastructure.logging.TerminalLogger
import org.now.terminal.shared.events.Event
import org.now.terminal.shared.events.EventHandler
import kotlin.reflect.KClass

/**
 * 事件处理器发现服务
 * 负责自动发现和注册所有实现EventHandler接口的组件
 */
class EventHandlerDiscoveryService(
    private val koin: Koin
) {
    private val logger = TerminalLogger.getLogger(EventHandlerDiscoveryService::class.java)
    
    /**
     * 发现并返回所有事件处理器实例
     */
    fun discoverEventHandlers(): List<EventHandler<*>> {
        val handlers = mutableListOf<EventHandler<*>>()
        
        try {
            // 获取所有已注册的EventHandler实例
            val allInstances = koin.getAll<EventHandler<*>>()
            handlers.addAll(allInstances)
            
            logger.info("✅ 发现 ${handlers.size} 个事件处理器")
            
            // 记录每个处理器的详细信息
            handlers.forEach { handler ->
                val handlerClass = handler.javaClass
                val eventTypes = getSupportedEventTypes(handler)
                logger.debug("📋 事件处理器: ${handlerClass.simpleName}, 支持事件类型: $eventTypes")
            }
            
        } catch (e: Exception) {
            logger.error("❌ 发现事件处理器时发生错误: ${e.message}")
        }
        
        return handlers
    }
    
    /**
     * 获取处理器支持的事件类型
     */
    private fun getSupportedEventTypes(handler: EventHandler<*>): List<String> {
        return try {
            // 通过反射获取处理器类的泛型参数
            val handlerClass = handler.javaClass
            val genericInterfaces = handlerClass.genericInterfaces
            
            val eventTypes = mutableListOf<String>()
            
            genericInterfaces.forEach { genericInterface ->
                if (genericInterface is java.lang.reflect.ParameterizedType) {
                    val rawType = genericInterface.rawType
                    if (rawType == EventHandler::class.java) {
                        val typeArguments = genericInterface.actualTypeArguments
                        if (typeArguments.isNotEmpty()) {
                            val eventType = typeArguments[0]
                            if (eventType is Class<*>) {
                                eventTypes.add(eventType.simpleName)
                            }
                        }
                    }
                }
            }
            
            eventTypes
        } catch (e: Exception) {
            logger.warn("⚠️ 无法获取处理器 ${handler.javaClass.simpleName} 支持的事件类型: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * 获取处理器处理的具体事件类
     */
    fun getEventClassForHandler(handler: EventHandler<*>): Class<out Event>? {
        return try {
            val handlerClass = handler.javaClass
            val genericInterfaces = handlerClass.genericInterfaces
            
            genericInterfaces.forEach { genericInterface ->
                if (genericInterface is java.lang.reflect.ParameterizedType) {
                    val rawType = genericInterface.rawType
                    if (rawType == EventHandler::class.java) {
                        val typeArguments = genericInterface.actualTypeArguments
                        if (typeArguments.isNotEmpty()) {
                            val eventType = typeArguments[0]
                            if (eventType is Class<*> && Event::class.java.isAssignableFrom(eventType)) {
                                @Suppress("UNCHECKED_CAST")
                                return eventType as Class<out Event>
                            }
                        }
                    }
                }
            }
            
            null
        } catch (e: Exception) {
            logger.warn("⚠️ 无法获取处理器 ${handler.javaClass.simpleName} 处理的事件类: ${e.message}")
            null
        }
    }
}
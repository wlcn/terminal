package org.now.terminal.shared.kernel

/**
 * 消息提供者接口 - 支持国际化（通用基础设施）
 */
interface MessageProvider {
    fun getMessage(key: String, params: Map<String, Any> = emptyMap()): String
    
    fun getMessage(key: String, vararg params: Pair<String, Any>): String {
        return getMessage(key, params.toMap())
    }
}

/**
 * 消息提供者管理器（通用基础设施）
 */
object MessageProviderManager {
    private var currentProvider: MessageProvider? = null
    
    /**
     * 设置当前消息提供者
     */
    fun setProvider(provider: MessageProvider) {
        currentProvider = provider
    }
    
    /**
     * 获取当前消息提供者
     */
    fun getProvider(): MessageProvider {
        return currentProvider ?: throw IllegalStateException("Message provider not set")
    }
    
    /**
     * 创建领域消息
     */
    fun createMessage(key: String, type: MessageType = MessageType.ERROR, vararg params: Pair<String, Any>): DomainMessage {
        val message = getProvider().getMessage(key, params.toMap())
        return DomainMessage(key, message, type, params.toMap())
    }
}
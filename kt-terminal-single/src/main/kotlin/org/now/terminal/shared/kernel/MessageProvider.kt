package org.now.terminal.shared.kernel

/**
 * Message provider interface - Supports internationalization (common infrastructure)
 */
interface MessageProvider {
    fun getMessage(key: String, params: Map<String, Any> = emptyMap()): String
    
    fun getMessage(key: String, vararg params: Pair<String, Any>): String {
        return getMessage(key, params.toMap())
    }
}

/**
 * Message provider manager (common infrastructure)
 */
object MessageProviderManager {
    private var currentProvider: MessageProvider? = null
    
    /**
     * Set current message provider
     */
    fun setProvider(provider: MessageProvider) {
        currentProvider = provider
    }
    
    /**
     * Get current message provider
     */
    fun getProvider(): MessageProvider {
        return currentProvider ?: throw IllegalStateException("Message provider not set")
    }
    
    /**
     * Create domain message
     */
    fun createMessage(key: String, type: MessageType = MessageType.ERROR, vararg params: Pair<String, Any>): DomainMessage {
        val message = getProvider().getMessage(key, params.toMap())
        return DomainMessage(key, message, type, params.toMap())
    }
}
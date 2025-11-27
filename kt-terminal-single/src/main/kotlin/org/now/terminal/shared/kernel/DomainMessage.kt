package org.now.terminal.shared.kernel

/**
 * 领域消息 - 封装领域层产生的各种消息
 */
data class DomainMessage(
    val key: String,
    val message: String,
    val type: MessageType = MessageType.ERROR,
    val params: Map<String, Any> = emptyMap()
) {
    /**
     * 获取格式化后的消息（支持参数替换）
     */
    fun getFormattedMessage(): String {
        var formatted = message
        params.forEach { (key, value) ->
            formatted = formatted.replace("{\$key}", value.toString())
        }
        return formatted
    }
    
    override fun toString(): String = getFormattedMessage()
}
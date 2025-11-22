package org.now.terminal.session.domain.valueobjects

import org.now.terminal.infrastructure.configuration.ConfigurationManager

/**
 * 输出缓冲区值对象
 */
class OutputBuffer {
    private val buffer = StringBuilder()
    private val maxSize = ConfigurationManager.getTerminalConfig().bufferSize
    
    /**
     * 追加内容到缓冲区
     */
    fun append(content: String) {
        if (buffer.length + content.length > maxSize) {
            // 如果超过限制，移除最旧的内容以容纳新内容
            val requiredSpace = content.length
            val availableSpace = maxSize - requiredSpace
            if (availableSpace < 0) {
                // 如果新内容本身就超过限制，只保留最后的部分
                buffer.clear()
                buffer.append(content.takeLast(maxSize))
                return
            }
            // 删除最旧的内容，确保有足够空间
            val overflow = buffer.length - availableSpace
            if (overflow > 0) {
                buffer.delete(0, overflow)
            }
        }
        buffer.append(content)
    }
    
    /**
     * 获取缓冲区内容
     */
    fun getContent(): String = buffer.toString()
    
    /**
     * 清空缓冲区
     */
    fun clear() {
        buffer.clear()
    }
    
    /**
     * 获取缓冲区大小
     */
    fun size(): Int = buffer.length
}
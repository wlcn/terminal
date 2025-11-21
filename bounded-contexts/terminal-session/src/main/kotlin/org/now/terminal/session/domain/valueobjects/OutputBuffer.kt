package org.now.terminal.session.domain.valueobjects

/**
 * 输出缓冲区值对象
 */
class OutputBuffer {
    private val buffer = StringBuilder()
    private val maxSize = 100_000 // 100KB限制
    
    /**
     * 追加内容到缓冲区
     */
    fun append(content: String) {
        if (buffer.length + content.length > maxSize) {
            // 如果超过限制，移除最旧的内容
            val overflow = (buffer.length + content.length) - maxSize
            buffer.delete(0, overflow)
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
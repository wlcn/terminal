package org.now.terminal.session.domain.valueobjects

import kotlinx.serialization.Serializable

/**
 * 输出缓冲区值对象
 * 管理终端输出内容，包含分页和截断逻辑
 */
@Serializable
data class OutputBuffer(
    private val content: String = "",
    private val maxSize: Int = 10000
) {
    /**
     * 添加输出内容
     */
    fun append(output: String): OutputBuffer {
        val newContent = if (content.isEmpty()) {
            output
        } else {
            "$content\n$output"
        }
        
        // 如果超过最大尺寸，截断最旧的内容
        return if (newContent.length > maxSize) {
            val truncated = newContent.substring(newContent.length - maxSize)
            OutputBuffer(truncated, maxSize)
        } else {
            OutputBuffer(newContent, maxSize)
        }
    }
    
    /**
     * 获取输出内容
     */
    fun getContent(): String = content
    
    /**
     * 清空缓冲区
     */
    fun clear(): OutputBuffer = OutputBuffer("", maxSize)
    
    /**
     * 获取行数
     */
    fun getLineCount(): Int = content.lines().size
    
    /**
     * 获取指定行范围的内容
     */
    fun getLines(startLine: Int, endLine: Int): String {
        val lines = content.lines()
        return lines.subList(
            startLine.coerceAtLeast(0),
            endLine.coerceAtMost(lines.size)
        ).joinToString("\n")
    }
    
    /**
     * 检查缓冲区是否为空
     */
    fun isEmpty(): Boolean = content.isBlank()
    
    /**
     * 检查缓冲区是否已满
     */
    fun isFull(): Boolean = content.length >= maxSize
}
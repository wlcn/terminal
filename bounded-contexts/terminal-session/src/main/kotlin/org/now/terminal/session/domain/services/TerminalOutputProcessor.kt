package org.now.terminal.session.domain.services

import org.now.terminal.session.domain.aggregates.SessionAggregate
import org.now.terminal.session.domain.repositories.TerminalSessionRepository
import org.now.terminal.shared.valueobjects.SessionId

/**
 * 终端输出处理器领域服务
 * 处理终端输出的解析、格式化和安全性检查
 */
class TerminalOutputProcessor(
    private val sessionRepository: TerminalSessionRepository,
    private val eventPublisher: DomainEventPublisher
) {
    
    /**
     * 处理终端输出
     */
    fun processOutput(sessionId: SessionId, rawOutput: String): ProcessedOutput {
        val session = sessionRepository.findById(sessionId)
            ?: throw SessionNotFoundException(sessionId)
        
        // 安全检查
        val sanitizedOutput = sanitizeOutput(rawOutput)
        
        // 格式化输出
        val formattedOutput = formatOutput(sanitizedOutput)
        
        // 发布输出事件
        val outputEvent = TerminalOutputEvent(
            sessionId = sessionId,
            output = formattedOutput,
            outputType = OutputType.STDOUT,
            timestamp = java.time.Instant.now()
        )
        eventPublisher.publish(outputEvent)
        
        return ProcessedOutput(
            original = rawOutput,
            processed = formattedOutput,
            isSanitized = sanitizedOutput != rawOutput,
            timestamp = java.time.Instant.now()
        )
    }
    
    /**
     * 处理终端错误输出
     */
    fun processErrorOutput(sessionId: SessionId, errorOutput: String): ProcessedOutput {
        val session = sessionRepository.findById(sessionId)
            ?: throw SessionNotFoundException(sessionId)
        
        // 安全检查
        val sanitizedOutput = sanitizeOutput(errorOutput)
        
        // 格式化错误输出
        val formattedOutput = formatErrorOutput(sanitizedOutput)
        
        // 发布错误事件
        val errorEvent = TerminalOutputEvent(
            sessionId = sessionId,
            output = formattedOutput,
            outputType = OutputType.STDERR,
            timestamp = java.time.Instant.now()
        )
        eventPublisher.publish(errorEvent)
        
        return ProcessedOutput(
            original = errorOutput,
            processed = formattedOutput,
            isSanitized = sanitizedOutput != errorOutput,
            timestamp = java.time.Instant.now()
        )
    }
    
    /**
     * 安全检查输出内容
     */
    private fun sanitizeOutput(output: String): String {
        // 移除控制字符（除了换行和制表符）
        var sanitized = output.replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]"), "")
        
        // 限制输出长度
        if (sanitized.length > 8192) {
            sanitized = sanitized.substring(0, 8192) + "... [TRUNCATED]"
        }
        
        return sanitized
    }
    
    /**
     * 格式化标准输出
     */
    private fun formatOutput(output: String): String {
        // 添加时间戳前缀
        val timestamp = java.time.format.DateTimeFormatter.ISO_INSTANT
            .format(java.time.Instant.now())
        return "[$timestamp] $output"
    }
    
    /**
     * 格式化错误输出
     */
    private fun formatErrorOutput(output: String): String {
        // 添加错误前缀和时间戳
        val timestamp = java.time.format.DateTimeFormatter.ISO_INSTANT
            .format(java.time.Instant.now())
        return "[$timestamp] [ERROR] $output"
    }
    
    /**
     * 批量处理输出
     */
    fun batchProcessOutputs(sessionId: SessionId, outputs: List<String>): List<ProcessedOutput> {
        return outputs.map { processOutput(sessionId, it) }
    }
    
    /**
     * 获取会话输出历史
     */
    fun getOutputHistory(sessionId: SessionId, limit: Int = 100): List<ProcessedOutput> {
        // 这里应该从持久化存储中获取历史输出
        // 暂时返回空列表
        return emptyList()
    }
}

/**
 * 处理后的输出数据类
 */
data class ProcessedOutput(
    val original: String,
    val processed: String,
    val isSanitized: Boolean,
    val timestamp: java.time.Instant
)

/**
 * 输出类型枚举
 */
enum class OutputType {
    STDOUT, // 标准输出
    STDERR  // 错误输出
}

/**
 * 终端输出事件
 */
data class TerminalOutputEvent(
    val sessionId: SessionId,
    val output: String,
    val outputType: OutputType,
    val timestamp: java.time.Instant
)
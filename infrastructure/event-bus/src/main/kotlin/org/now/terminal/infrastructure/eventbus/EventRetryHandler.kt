package org.now.terminal.infrastructure.eventbus

import kotlinx.coroutines.delay
import org.now.terminal.shared.events.Event
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 * 事件重试处理器
 */
class EventRetryHandler(
    private val config: EventBusProperties
) {
    private val logger = LoggerFactory.getLogger(EventRetryHandler::class.java)
    
    /**
     * 执行重试逻辑
     */
    suspend fun <T : Event> retry(
        event: T,
        handlerId: String,
        operation: suspend (T) -> Unit
    ): Boolean {
        if (!config.enableRetry) {
            return false
        }
        
        var attempt = 0
        var lastDelay = config.retryDelayMs
        
        while (attempt < config.maxRetries) {
            try {
                operation(event)
                logger.debug("Event successfully processed after {} retries: {}", attempt, event.eventId)
                return true
            } catch (e: Exception) {
                attempt++
                
                if (attempt >= config.maxRetries) {
                    logger.warn("Event processing failed after {} retries: {}, handler: {}", 
                        attempt, event.eventId, handlerId, e)
                    return false
                }
                
                // 计算下一次重试的延迟时间（指数退避）
                val nextDelay = calculateNextDelay(lastDelay, attempt)
                logger.debug("Event processing failed, retrying in {}ms (attempt {}/{}): {}, handler: {}", 
                    nextDelay, attempt, config.maxRetries, event.eventId, handlerId)
                
                delay(nextDelay)
                lastDelay = nextDelay
            }
        }
        
        return false
    }
    
    /**
     * 计算下一次重试的延迟时间（指数退避算法）
     */
    private fun calculateNextDelay(currentDelay: Long, attempt: Int): Long {
        val nextDelay = (currentDelay * config.retryBackoffMultiplier).toLong()
        return minOf(nextDelay, config.maxRetryDelayMs)
    }
    
    /**
     * 检查是否应该重试特定异常
     */
    fun shouldRetry(exception: Throwable): Boolean {
        // 对于某些不可恢复的异常，不应该重试
        return when (exception) {
            is EventBusException -> {
                // 事件总线异常通常可以重试
                true
            }
            is IllegalArgumentException -> {
                // 参数异常通常不应该重试
                false
            }
            is IllegalStateException -> {
                // 状态异常通常不应该重试
                false
            }
            else -> {
                // 其他异常默认可以重试
                true
            }
        }
    }
}
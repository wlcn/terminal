package org.now.terminal.infrastructure.eventbus

import org.now.terminal.shared.events.Event
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * 死信队列 - 存储处理失败的事件
 */
class DeadLetterQueue(
    private val capacity: Int = 1000
) {
    private val logger = LoggerFactory.getLogger(DeadLetterQueue::class.java)
    private val queue = ConcurrentLinkedQueue<DeadLetterEvent>()
    private val size = AtomicInteger(0)
    
    /**
     * 添加事件到死信队列
     */
    fun add(event: Event, error: Throwable, retryCount: Int = 0): Boolean {
        if (size.get() >= capacity) {
            logger.warn("Dead letter queue is full, discarding event: {}", event.eventId)
            return false
        }
        
        val deadLetterEvent = DeadLetterEvent(event, error, retryCount)
        if (queue.offer(deadLetterEvent)) {
            size.incrementAndGet()
            logger.info("Event added to dead letter queue: {}, retry count: {}", event.eventId, retryCount)
            return true
        }
        return false
    }
    
    /**
     * 从死信队列获取事件
     */
    fun poll(): DeadLetterEvent? {
        val event = queue.poll()
        if (event != null) {
            size.decrementAndGet()
        }
        return event
    }
    
    /**
     * 获取队列大小
     */
    fun size(): Int = size.get()
    
    /**
     * 检查队列是否为空
     */
    fun isEmpty(): Boolean = size.get() == 0
    
    /**
     * 清空队列
     */
    fun clear() {
        queue.clear()
        size.set(0)
        logger.info("Dead letter queue cleared")
    }
}

/**
 * 死信队列事件包装类
 */
data class DeadLetterEvent(
    val event: Event,
    val error: Throwable,
    val retryCount: Int,
    val timestamp: Long = System.currentTimeMillis()
)
package org.now.terminal.infrastructure.eventbus

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.now.terminal.shared.events.Event
import org.slf4j.LoggerFactory

/**
 * 事件序列化器
 * 支持事件的JSON序列化和反序列化
 */
class EventSerializer {
    private val logger = LoggerFactory.getLogger(EventSerializer::class.java)
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        allowStructuredMapKeys = true
    }
    
    /**
     * 序列化事件为JSON字符串
     */
    fun <T : Event> serialize(event: T): String {
        return try {
            json.encodeToString(event)
        } catch (e: Exception) {
            logger.error("Failed to serialize event: {}", event.eventId, e)
            throw EventSerializationException(
                eventType = event.eventType,
                operation = "serialize",
                message = "Failed to serialize event: ${event.eventId}",
                cause = e
            )
        }
    }
    
    /**
     * 反序列化JSON字符串为事件对象
     */
    fun <T : Event> deserialize(jsonString: String, eventClass: Class<T>): T {
        return try {
            json.decodeFromString(jsonString)
        } catch (e: Exception) {
            logger.error("Failed to deserialize event from JSON: {}", jsonString.take(100), e)
            throw EventSerializationException(
                eventType = eventClass.simpleName,
                operation = "deserialize",
                message = "Failed to deserialize event from JSON",
                cause = e
            )
        }
    }
    
    /**
     * 验证事件JSON格式
     */
    fun validateEventJson(jsonString: String): Boolean {
        return try {
            // 尝试解析为通用Map来验证基本结构
            val map = json.decodeFromString<Map<String, Any>>(jsonString)
            map.containsKey("eventHelper") && map.containsKey("eventType")
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取事件类型从JSON字符串
     */
    fun getEventTypeFromJson(jsonString: String): String? {
        return try {
            val map = json.decodeFromString<Map<String, Any>>(jsonString)
            (map["eventHelper"] as? Map<String, Any>)?.get("eventType") as? String
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * 事件序列化异常
 */
data class EventSerializationException(
    val eventType: String,
    val operation: String,
    override val message: String,
    override val cause: Throwable? = null
) : EventBusException(
    code = "EVENT_SERIALIZATION_ERROR",
    message = "Failed to $operation event of type '$eventType': $message",
    cause = cause,
    context = mapOf(
        "eventType" to eventType,
        "operation" to operation
    )
)
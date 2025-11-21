package org.now.terminal.infrastructure.eventbus

import org.now.terminal.shared.events.Event

/**
 * 事件存储接口
 * 支持事件的持久化存储和检索
 */
interface EventStore {
    /**
     * 存储事件
     */
    suspend fun store(event: Event): Boolean
    
    /**
     * 根据事件ID检索事件
     */
    suspend fun retrieve(eventId: String): Event?
    
    /**
     * 根据事件类型检索事件
     */
    suspend fun retrieveByType(eventType: String, limit: Int = 100): List<Event>
    
    /**
     * 根据聚合ID检索事件
     */
    suspend fun retrieveByAggregateId(aggregateId: String, limit: Int = 100): List<Event>
    
    /**
     * 获取存储的事件数量
     */
    suspend fun count(): Long
    
    /**
     * 清理过时的事件
     */
    suspend fun cleanup(beforeTimestamp: Long): Int
}

/**
 * 基于文件的事件存储实现
 */
class FileEventStore(
    private val storageDir: String = "events",
    private val serializer: EventSerializer = EventSerializer()
) : EventStore {
    private val logger = org.slf4j.LoggerFactory.getLogger(FileEventStore::class.java)
    
    init {
        // 确保存储目录存在
        val dir = java.io.File(storageDir)
        if (!dir.exists()) {
            dir.mkdirs()
            logger.info("Created event storage directory: {}", storageDir)
        }
    }
    
    override suspend fun store(event: Event): Boolean {
        return try {
            val jsonString = serializer.serialize(event)
            val fileName = "${event.eventId}.json"
            val file = java.io.File(storageDir, fileName)
            
            file.writeText(jsonString)
            logger.debug("Event stored: {} in file: {}", event.eventId, fileName)
            true
        } catch (e: Exception) {
            logger.error("Failed to store event: {}", event.eventId, e)
            false
        }
    }
    
    override suspend fun retrieve(eventId: String): Event? {
        return try {
            val fileName = "$eventId.json"
            val file = java.io.File(storageDir, fileName)
            
            if (!file.exists()) {
                return null
            }
            
            val jsonString = file.readText()
            // 这里需要根据事件类型反序列化，简化处理
            // 实际实现中需要更复杂的类型推断
            null
        } catch (e: Exception) {
            logger.error("Failed to retrieve event: {}", eventId, e)
            null
        }
    }
    
    override suspend fun retrieveByType(eventType: String, limit: Int): List<Event> {
        // 简化实现，实际中需要更复杂的文件扫描和解析
        return emptyList()
    }
    
    override suspend fun retrieveByAggregateId(aggregateId: String, limit: Int): List<Event> {
        // 简化实现，实际中需要更复杂的文件扫描和解析
        return emptyList()
    }
    
    override suspend fun count(): Long {
        return try {
            val dir = java.io.File(storageDir)
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles { file -> file.name.endsWith(".json") }?.size?.toLong() ?: 0
            } else {
                0
            }
        } catch (e: Exception) {
            logger.error("Failed to count events", e)
            0
        }
    }
    
    override suspend fun cleanup(beforeTimestamp: Long): Int {
        return try {
            val dir = java.io.File(storageDir)
            if (!dir.exists()) return 0
            
            var deletedCount = 0
            dir.listFiles { file -> file.name.endsWith(".json") }?.forEach { file ->
                if (file.lastModified() < beforeTimestamp) {
                    if (file.delete()) {
                        deletedCount++
                        logger.debug("Cleaned up event file: {}", file.name)
                    }
                }
            }
            deletedCount
        } catch (e: Exception) {
            logger.error("Failed to cleanup events", e)
            0
        }
    }
}
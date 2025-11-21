package org.now.terminal.shared.valueobjects

import java.util.UUID

/**
 * ID工具类 - 提供通用的ID操作和验证逻辑
 * 使用扩展函数和工厂函数消除重复代码，避免继承
 */
object IdUtils {
    private const val SEPARATOR = "_"
    
    /**
     * 验证ID格式
     */
    fun isValidFormat(id: String, prefix: String): Boolean {
        if (!id.startsWith("${prefix}${SEPARATOR}")) return false
        val uuidPart = id.removePrefix("${prefix}${SEPARATOR}")
        return try {
            UUID.fromString(uuidPart)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }
    
    /**
     * 生成带前缀的ID字符串
     */
    fun generateIdString(prefix: String): String = "${prefix}${SEPARATOR}${UUID.randomUUID()}"
    
    /**
     * 从UUID生成ID字符串
     */
    fun fromUuid(uuid: UUID, prefix: String): String = "${prefix}${SEPARATOR}$uuid"
    
    /**
     * 创建ID的通用验证逻辑
     */
    fun <T> createId(id: String, prefix: String, factory: (String) -> T): T {
        require(id.isNotBlank()) { "ID cannot be blank" }
        require(isValidFormat(id, prefix)) { "ID must be in format ${prefix}${SEPARATOR}{UUID}" }
        return factory(id.trim())
    }
}

/**
 * ID值对象的组合式工具类
 * 使用组合而不是继承，提供ID相关的通用功能
 */
class IdValueObjectHelper(private val value: String, private val prefix: String) {
    
    /**
     * 获取UUID部分（不含前缀）
     */
    val uuid: UUID
        get() = UUID.fromString(value.removePrefix("${prefix}_"))

    /**
     * 检查ID是否有效
     */
    fun isValid(): Boolean = value.isNotBlank() && IdUtils.isValidFormat(value, prefix)

    /**
     * 获取简化的ID表示（前缀 + 前8个字符）
     */
    fun toShortString(): String = "${prefix}_${uuid.toString().substring(0, 8)}"

    /**
     * 获取纯UUID字符串（不含前缀）
     */
    fun toUuidString(): String = uuid.toString()
    
    /**
     * 获取完整的ID值
     */
    fun getValue(): String = value
    
    /**
     * 获取前缀
     */
    fun getPrefix(): String = prefix
}

/**
 * 创建ID值对象辅助工具的扩展函数
 */
fun String.toIdHelper(prefix: String): IdValueObjectHelper = IdValueObjectHelper(this, prefix)
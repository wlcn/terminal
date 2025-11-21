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
 * ID值对象的通用扩展函数
 */
interface IdValueObject {
    val value: String
    val prefix: String
}

/**
 * 获取UUID部分（不含前缀）
 */
val IdValueObject.uuid: UUID
    get() = UUID.fromString(value.removePrefix("${prefix}_"))

/**
 * 检查ID是否有效
 */
fun IdValueObject.isValid(): Boolean = value.isNotBlank() && IdUtils.isValidFormat(value, prefix)

/**
 * 获取简化的ID表示（前缀 + 前8个字符）
 */
fun IdValueObject.toShortString(): String = "${prefix}_${uuid.toString().substring(0, 8)}"

/**
 * 获取纯UUID字符串（不含前缀）
 */
fun IdValueObject.toUuidString(): String = uuid.toString()
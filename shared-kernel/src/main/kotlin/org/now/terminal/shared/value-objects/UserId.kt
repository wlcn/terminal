package org.now.terminal.shared.valueobjects

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * 用户ID值对象
 * 表示用户的唯一标识符
 */
@JvmInline
@Serializable
value class UserId private constructor(val value: String) {
    private val helper: IdValueObjectHelper get() = value.toIdHelper(PREFIX)
    
    val prefix: String get() = PREFIX
    
    companion object {
        private const val PREFIX = "usr"
        
        /**
         * 创建用户ID
         * @param id 用户ID字符串，必须符合前缀格式
         * @return 有效的UserId实例
         * @throws IllegalArgumentException 如果ID格式无效
         */
        fun create(id: String): UserId = IdUtils.createId(id, PREFIX) { UserId(it) }

        /**
         * 生成新的用户ID
         * @return 新生成的UserId实例
         */
        fun generate(): UserId = UserId(IdUtils.generateIdString(PREFIX))

        /**
         * 从字符串解析用户ID
         * @param idString 用户ID字符串
         * @return UserId实例，如果格式有效
         * @throws IllegalArgumentException 如果格式无效
         */
        fun fromString(idString: String): UserId = create(idString)

        /**
         * 从纯UUID创建用户ID（向后兼容）
         */
        fun fromUuid(uuid: UUID): UserId = UserId(IdUtils.fromUuid(uuid, PREFIX))
    }

    /**
     * 转换为字符串表示（包含前缀）
     */
    override fun toString(): String = value
    
    /**
     * 获取UUID部分（不含前缀）
     */
    val uuid: UUID get() = helper.uuid
    
    /**
     * 检查ID是否有效
     */
    fun isValid(): Boolean = helper.isValid()
    
    /**
     * 获取简化的ID表示（前缀 + 前8个字符）
     */
    fun toShortString(): String = helper.toShortString()
    
    /**
     * 获取纯UUID字符串（不含前缀）
     */
    fun toUuidString(): String = helper.toUuidString()
}
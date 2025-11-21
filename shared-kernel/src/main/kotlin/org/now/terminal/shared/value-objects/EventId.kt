package org.now.terminal.shared.valueobjects

import java.util.UUID

/**
 * 事件ID值对象
 * 表示系统中的唯一事件标识符，使用前缀明确职责
 * 格式：evt_{UUID}
 */
@JvmInline
value class EventId private constructor(val value: String) {
    private val helper: IdValueObjectHelper get() = value.toIdHelper(PREFIX)
    
    val prefix: String get() = PREFIX
    
    companion object {
        private const val PREFIX = "evt"
        
        /**
         * 创建事件ID
         * @param id 事件ID字符串，必须符合前缀格式
         * @return 有效的EventId实例
         * @throws IllegalArgumentException 如果ID格式无效
         */
        fun create(id: String): EventId = IdUtils.createId(id, PREFIX) { EventId(it) }

        /**
         * 生成新的事件ID
         * @return 新生成的EventId实例
         */
        fun generate(): EventId = EventId(IdUtils.generateIdString(PREFIX))

        /**
         * 从字符串解析事件ID
         * @param idString 事件ID字符串
         * @return EventId实例，如果格式有效
         * @throws IllegalArgumentException 如果格式无效
         */
        fun fromString(idString: String): EventId = create(idString)

        /**
         * 从纯UUID创建事件ID（向后兼容）
         */
        fun fromUuid(uuid: UUID): EventId = EventId(IdUtils.fromUuid(uuid, PREFIX))
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
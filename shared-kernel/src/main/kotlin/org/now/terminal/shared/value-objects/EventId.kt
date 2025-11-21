package org.now.terminal.shared.valueobjects

import java.util.UUID

/**
 * 事件ID值对象
 * 表示领域事件的唯一标识符，使用前缀明确职责
 * 格式：evt_{UUID}
 */
@JvmInline
value class EventId private constructor(val value: String) {
    companion object {
        private const val PREFIX = "evt"
        private const val SEPARATOR = "_"
        
        /**
         * 创建事件ID
         * @param id 事件ID字符串，必须符合前缀格式
         * @return 有效的EventId实例
         * @throws IllegalArgumentException 如果ID格式无效
         */
        fun create(id: String): EventId {
            require(id.isNotBlank()) { "Event ID cannot be blank" }
            require(isValidFormat(id)) { "Event ID must be in format ${PREFIX}${SEPARATOR}{UUID}" }
            return EventId(id.trim())
        }

        /**
         * 生成新的事件ID
         * @return 新生成的EventId实例
         */
        fun generate(): EventId = EventId("${PREFIX}${SEPARATOR}${UUID.randomUUID()}")

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
        fun fromUuid(uuid: UUID): EventId = EventId("${PREFIX}${SEPARATOR}$uuid")

        private fun isValidFormat(id: String): Boolean {
            if (!id.startsWith("${PREFIX}${SEPARATOR}")) return false
            val uuidPart = id.removePrefix("${PREFIX}${SEPARATOR}")
            return try {
                UUID.fromString(uuidPart)
                true
            } catch (e: IllegalArgumentException) {
                false
            }
        }
    }

    /**
     * 获取UUID部分（不含前缀）
     */
    val uuid: UUID get() = UUID.fromString(value.removePrefix("${PREFIX}${SEPARATOR}"))

    /**
     * 检查事件ID是否有效
     */
    fun isValid(): Boolean = value.isNotBlank() && isValidFormat(value)

    /**
     * 转换为字符串表示（包含前缀）
     */
    override fun toString(): String = value

    /**
     * 获取简化的ID表示（前缀 + 前8个字符）
     */
    fun toShortString(): String = "${PREFIX}${SEPARATOR}${uuid.toString().substring(0, 8)}"

    /**
     * 获取纯UUID字符串（不含前缀）
     */
    fun toUuidString(): String = uuid.toString()

    private fun isValidFormat(id: String): Boolean = Companion.isValidFormat(id)
}
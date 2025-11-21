package org.now.terminal.shared.valueobjects

import java.util.UUID

/**
 * 会话ID值对象
 * 表示终端会话的唯一标识符，使用前缀明确职责
 * 格式：ses_{UUID}
 */
@JvmInline
value class SessionId private constructor(val value: String) {
    companion object {
        private const val PREFIX = "ses"
        private const val SEPARATOR = "_"
        
        /**
         * 创建会话ID
         * @param id 会话ID字符串，必须符合前缀格式
         * @return 有效的SessionId实例
         * @throws IllegalArgumentException 如果ID格式无效
         */
        fun create(id: String): SessionId {
            require(id.isNotBlank()) { "Session ID cannot be blank" }
            require(isValidFormat(id)) { "Session ID must be in format ${PREFIX}${SEPARATOR}{UUID}" }
            return SessionId(id.trim())
        }

        /**
         * 生成新的会话ID
         * @return 新生成的SessionId实例
         */
        fun generate(): SessionId = SessionId("${PREFIX}${SEPARATOR}${UUID.randomUUID()}")

        /**
         * 从字符串解析会话ID
         * @param idString 会话ID字符串
         * @return SessionId实例，如果格式有效
         * @throws IllegalArgumentException 如果格式无效
         */
        fun fromString(idString: String): SessionId = create(idString)

        /**
         * 从纯UUID创建会话ID（向后兼容）
         */
        fun fromUuid(uuid: UUID): SessionId = SessionId("${PREFIX}${SEPARATOR}$uuid")

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
     * 检查会话ID是否有效
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
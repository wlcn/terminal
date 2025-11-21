package org.now.terminal.shared.valueobjects

import java.util.UUID

/**
 * 用户ID值对象
 * 表示系统中的唯一用户标识符，使用前缀明确职责
 * 格式：usr_{UUID}
 */
@JvmInline
value class UserId private constructor(val value: String) {
    companion object {
        private const val PREFIX = "usr"
        private const val SEPARATOR = "_"
        
        /**
         * 创建用户ID
         * @param id 用户ID字符串，必须符合前缀格式
         * @return 有效的UserId实例
         * @throws IllegalArgumentException 如果ID格式无效
         */
        fun create(id: String): UserId {
            require(id.isNotBlank()) { "User ID cannot be blank" }
            require(isValidFormat(id)) { "User ID must be in format ${PREFIX}${SEPARATOR}{UUID}" }
            return UserId(id.trim())
        }

        /**
         * 生成新的用户ID
         * @return 新生成的UserId实例
         */
        fun generate(): UserId = UserId("${PREFIX}${SEPARATOR}${UUID.randomUUID()}")

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
        fun fromUuid(uuid: UUID): UserId = UserId("${PREFIX}${SEPARATOR}$uuid")

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
     * 检查用户ID是否有效
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
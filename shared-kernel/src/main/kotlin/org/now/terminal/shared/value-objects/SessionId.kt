package org.now.terminal.shared.valueobjects

import java.util.UUID

/**
 * 会话ID值对象
 * 表示终端会话的唯一标识符，使用前缀明确职责
 * 格式：ses_{UUID}
 */
@JvmInline
value class SessionId private constructor(val value: String) {
    private val helper: IdValueObjectHelper get() = value.toIdHelper(PREFIX)
    
    val prefix: String get() = PREFIX
    
    companion object {
        private const val PREFIX = "ses"
        
        /**
         * 创建会话ID
         * @param id 会话ID字符串，必须符合前缀格式
         * @return 有效的SessionId实例
         * @throws IllegalArgumentException 如果ID格式无效
         */
        fun create(id: String): SessionId = IdUtils.createId(id, PREFIX) { SessionId(it) }

        /**
         * 生成新的会话ID
         * @return 新生成的SessionId实例
         */
        fun generate(): SessionId = SessionId(IdUtils.generateIdString(PREFIX))

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
        fun fromUuid(uuid: UUID): SessionId = SessionId(IdUtils.fromUuid(uuid, PREFIX))
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
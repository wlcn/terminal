package org.now.terminal.session.domain.valueobjects

/**
 * 终端尺寸值对象
 * 表示终端的行数和列数
 */
data class TerminalSize(val rows: Int, val columns: Int) {
    init {
        require(rows > 0) { "Rows must be positive" }
        require(columns > 0) { "Columns must be positive" }
        require(rows <= MAX_ROWS) { "Rows cannot exceed $MAX_ROWS" }
        require(columns <= MAX_COLUMNS) { "Columns cannot exceed $MAX_COLUMNS" }
    }

    companion object {
        const val MAX_ROWS = 1000
        const val MAX_COLUMNS = 1000
        
        /**
         * 默认终端尺寸（24行80列）
         */
        val DEFAULT = TerminalSize(24, 80)

        /**
         * 创建终端尺寸
         * @param rows 行数
         * @param columns 列数
         * @return 有效的TerminalSize实例
         * @throws IllegalArgumentException 如果尺寸无效
         */
        fun create(rows: Int, columns: Int): TerminalSize = TerminalSize(rows, columns)

        /**
         * 从字符串解析终端尺寸
         * 格式："rowsxcolumns"，例如 "24x80"
         * @param sizeString 尺寸字符串
         * @return TerminalSize实例
         * @throws IllegalArgumentException 如果格式无效
         */
        fun fromString(sizeString: String): TerminalSize {
            require(sizeString.isNotBlank()) { "Size string cannot be blank" }
            
            val parts = sizeString.split("x", "X")
            require(parts.size == 2) { "Size string must be in format 'rowsxcolumns'" }
            
            val rows = parts[0].trim().toIntOrNull() ?: throw IllegalArgumentException("Invalid rows format")
            val columns = parts[1].trim().toIntOrNull() ?: throw IllegalArgumentException("Invalid columns format")
            
            return create(rows, columns)
        }
    }

    /**
     * 计算终端面积（行数 × 列数）
     */
    fun area(): Int = rows * columns

    /**
     * 检查尺寸是否有效
     */
    fun isValid(): Boolean = rows > 0 && columns > 0 && rows <= MAX_ROWS && columns <= MAX_COLUMNS

    /**
     * 转换为字符串表示（格式："rowsxcolumns"）
     */
    override fun toString(): String = "${rows}x${columns}"

    /**
     * 检查是否大于指定尺寸
     */
    fun isLargerThan(other: TerminalSize): Boolean = area() > other.area()

    /**
     * 检查是否小于指定尺寸
     */
    fun isSmallerThan(other: TerminalSize): Boolean = area() < other.area()

    /**
     * 检查是否等于指定尺寸
     */
    fun isEqualTo(other: TerminalSize): Boolean = rows == other.rows && columns == other.columns
}
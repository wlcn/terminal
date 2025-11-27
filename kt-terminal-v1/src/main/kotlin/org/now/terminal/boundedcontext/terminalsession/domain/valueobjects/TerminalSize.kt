package org.now.terminal.boundedcontext.terminalsession.domain.valueobjects

import kotlinx.serialization.Serializable

/**
 * Terminal Size Value Object
 * Represents the dimensions of a terminal (rows and cols)
 */
@Serializable
data class TerminalSize(
    val rows: Int,
    val cols: Int
) {
    
    companion object {
        private const val MIN_ROWS = 1
        private const val MAX_ROWS = 1000
        private const val MIN_COLS = 1
        private const val MAX_COLS = 1000
        
        private const val ROWS_OUT_OF_RANGE = "terminal.rows.out.of.range"
        private const val COLS_OUT_OF_RANGE = "terminal.cols.out.of.range"
        
        /**
         * Default terminal size (24 rows, 80 cols - standard VT100 size)
         */
        val DEFAULT: TerminalSize = TerminalSize(24, 80)
        
        /**
         * Create terminal size with validation
         */
        fun create(rows: Int, cols: Int): TerminalSize {
            require(rows in MIN_ROWS..MAX_ROWS) { ROWS_OUT_OF_RANGE }
            require(cols in MIN_COLS..MAX_COLS) { COLS_OUT_OF_RANGE }
            return TerminalSize(rows, cols)
        }
    }
    
    init {
        require(rows in MIN_ROWS..MAX_ROWS) { ROWS_OUT_OF_RANGE }
        require(cols in MIN_COLS..MAX_COLS) { COLS_OUT_OF_RANGE }
    }
    
    /**
     * Check if this size is the default size
     */
    val isDefault: Boolean
        get() = rows == DEFAULT.rows && cols == DEFAULT.cols
    
    /**
     * Calculate total character capacity
     */
    val capacity: Int
        get() = rows * cols
    
    /**
     * Resize terminal
     */
    fun resize(newRows: Int, newCols: Int): TerminalSize {
        return create(newRows, newCols)
    }
    
    /**
     * Increase rows
     */
    fun increaseRows(amount: Int = 1): TerminalSize {
        return resize(rows + amount, cols)
    }
    
    /**
     * Decrease rows
     */
    fun decreaseRows(amount: Int = 1): TerminalSize {
        return resize(rows - amount, cols)
    }
    
    /**
     * Increase cols
     */
    fun increaseCols(amount: Int = 1): TerminalSize {
        return resize(rows, cols + amount)
    }
    
    /**
     * Decrease cols
     */
    fun decreaseCols(amount: Int = 1): TerminalSize {
        return resize(rows, cols - amount)
    }
    
    override fun toString(): String {
        return "${rows}x${cols}"
    }
}
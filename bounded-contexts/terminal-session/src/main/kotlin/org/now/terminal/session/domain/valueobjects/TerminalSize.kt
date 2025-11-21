package org.now.terminal.session.domain.valueobjects

import kotlinx.serialization.Serializable

/**
 * 终端尺寸值对象
 */
@Serializable
data class TerminalSize(val columns: Int, val rows: Int) {
    init {
        require(columns > 0) { "Columns must be positive" }
        require(rows > 0) { "Rows must be positive" }
        require(columns <= 500) { "Columns too large" }
        require(rows <= 200) { "Rows too large" }
    }
    
    companion object {
        val DEFAULT = TerminalSize(80, 24)
        
        fun fromString(size: String): TerminalSize {
            val parts = size.split("x")
            require(parts.size == 2) { "Terminal size format should be 'columnsxrows'" }
            
            val columns = parts[0].toInt()
            val rows = parts[1].toInt()
            
            return TerminalSize(columns, rows)
        }
    }
}
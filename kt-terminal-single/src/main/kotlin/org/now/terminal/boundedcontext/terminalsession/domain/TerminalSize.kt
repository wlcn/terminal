package org.now.terminal.boundedcontext.terminalsession.domain

/**
 * 终端大小值对象 - 表示终端的列数和行数
 */
class TerminalSize(
    val columns: Int,
    val rows: Int
) {
    init {
        require(columns > 0) { "列数必须为正整数" }
        require(rows > 0) { "行数必须为正整数" }
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as TerminalSize
        
        if (columns != other.columns) return false
        if (rows != other.rows) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = columns
        result = 31 * result + rows
        return result
    }
    
    override fun toString(): String = "TerminalSize(columns=$columns, rows=$rows)"
}
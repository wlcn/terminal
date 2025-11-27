package org.now.terminal.shared.kernel.utils

/**
 * 字符串工具类
 */
object StringUtils {
    
    /**
     * 生成随机字符串
     */
    fun generateRandomString(length: Int = 16): String {
        val charPool = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..length)
            .map { charPool.random() }
            .joinToString("")
    }
    
    /**
     * 生成UUID
     */
    fun generateUUID(): String = java.util.UUID.randomUUID().toString()
    
    /**
     * 驼峰转下划线
     */
    fun camelToSnakeCase(input: String): String {
        return input.replace(
            Regex("(?<=[a-z])[A-Z]"), "_$0"
        ).lowercase()
    }
    
    /**
     * 下划线转驼峰
     */
    fun snakeToCamelCase(input: String): String {
        return input.split('_')
            .mapIndexed { index, part ->
                if (index == 0) part.lowercase() 
                else part.replaceFirstChar { it.uppercase() }
            }
            .joinToString("")
    }
    
    /**
     * 截断字符串并添加省略号
     */
    fun truncateWithEllipsis(input: String, maxLength: Int): String {
        return if (input.length <= maxLength) input 
        else input.substring(0, maxLength - 3) + "..."
    }
}
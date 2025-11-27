package org.now.terminal.shared.kernel.utils

/**
 * 验证工具类 - 提供通用的验证逻辑，不包含硬编码的业务规则
 */
object ValidationUtils {
    
    /**
     * 验证字符串不为空且长度在范围内
     * @param errorMessageProvider 错误消息提供函数，允许自定义错误消息
     */
    fun validateString(
        value: String?,
        minLength: Int = 1,
        maxLength: Int = 255,
        errorMessageProvider: (String) -> String = { "Validation failed for: $it" }
    ): String {
        requireNotNull(value) { errorMessageProvider("Value cannot be null") }
        require(value.isNotBlank()) { errorMessageProvider("Value cannot be blank") }
        require(value.length in minLength..maxLength) { 
            errorMessageProvider("Length must be between $minLength and $maxLength") 
        }
        return value
    }
    
    /**
     * 验证正则表达式匹配
     */
    fun validatePattern(
        value: String,
        pattern: Regex,
        errorMessage: String = "Pattern validation failed"
    ): String {
        require(pattern.matches(value)) { errorMessage }
        return value
    }
    
    /**
     * 验证数字范围
     */
    fun validateNumber(
        value: Number?,
        min: Number? = null,
        max: Number? = null,
        errorMessageProvider: (String) -> String = { "Validation failed for: $it" }
    ): Number {
        requireNotNull(value) { errorMessageProvider("Value cannot be null") }
        
        min?.let { 
            require(value.toDouble() >= it.toDouble()) { 
                errorMessageProvider("Value must be >= $min") 
            } 
        }
        max?.let { 
            require(value.toDouble() <= it.toDouble()) { 
                errorMessageProvider("Value must be <= $max") 
            } 
        }
        
        return value
    }
    
    /**
     * 验证集合不为空
     */
    fun <T> validateNotEmpty(
        collection: Collection<T>?,
        errorMessage: String = "Collection cannot be empty"
    ): Collection<T> {
        requireNotNull(collection) { "Collection cannot be null" }
        require(collection.isNotEmpty()) { errorMessage }
        return collection
    }
    
    /**
     * 验证布尔条件
     */
    fun validateCondition(
        condition: Boolean,
        errorMessage: String
    ) {
        require(condition) { errorMessage }
    }
}
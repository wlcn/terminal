package org.now.terminal.session.domain.valueobjects

import kotlinx.serialization.Serializable

/**
 * 环境变量值对象
 * 管理终端进程的环境变量配置
 */
@Serializable
data class EnvironmentVariables(
    private val variables: Map<String, String> = emptyMap()
) {
    /**
     * 添加或更新环境变量
     */
    fun setVariable(name: String, value: String): EnvironmentVariables {
        require(name.isNotBlank()) { "Variable name cannot be blank" }
        require(name.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*$"))) { "Invalid variable name format" }
        
        val newVariables = variables.toMutableMap()
        newVariables[name] = value
        return EnvironmentVariables(newVariables)
    }
    
    /**
     * 获取环境变量值
     */
    fun getVariable(name: String): String? = variables[name]
    
    /**
     * 删除环境变量
     */
    fun removeVariable(name: String): EnvironmentVariables {
        val newVariables = variables.toMutableMap()
        newVariables.remove(name)
        return EnvironmentVariables(newVariables)
    }
    
    /**
     * 获取所有环境变量
     */
    fun getAllVariables(): Map<String, String> = variables.toMap()
    
    /**
     * 合并环境变量
     */
    fun merge(other: EnvironmentVariables): EnvironmentVariables {
        val merged = variables.toMutableMap()
        merged.putAll(other.variables)
        return EnvironmentVariables(merged)
    }
    
    /**
     * 检查是否包含特定变量
     */
    fun contains(name: String): Boolean = variables.containsKey(name)
    
    /**
     * 获取变量数量
     */
    fun size(): Int = variables.size
    
    /**
     * 检查是否为空
     */
    fun isEmpty(): Boolean = variables.isEmpty()
    
    companion object {
        /**
         * 创建默认环境变量
         */
        fun default(): EnvironmentVariables = EnvironmentVariables(
            mapOf(
                "TERM" to "xterm-256color",
                "COLORTERM" to "truecolor",
                "LANG" to "en_US.UTF-8"
            )
        )
        
        /**
         * 从Map创建环境变量
         */
        fun fromMap(map: Map<String, String>): EnvironmentVariables {
            val validMap = map.filterKeys { key ->
                key.isNotBlank() && key.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*$"))
            }
            return EnvironmentVariables(validMap)
        }
    }
}
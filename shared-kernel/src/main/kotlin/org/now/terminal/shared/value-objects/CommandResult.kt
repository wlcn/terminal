package org.now.terminal.shared.valueobjects

/**
 * 命令执行结果值对象
 * 表示命令执行的结果状态和输出
 */
sealed class CommandResult {
    /**
     * 命令执行成功
     * @property output 命令输出内容
     * @property code 退出代码
     */
    data class Success(
        val output: String,
        val code: Int = 0
    ) : CommandResult()

    /**
     * 命令执行失败
     * @property errorMessage 错误信息
     * @property code 退出代码
     */
    data class Failure(
        val errorMessage: String,
        val code: Int = 1
    ) : CommandResult()

    /**
     * 命令执行超时
     * @property timeoutMs 超时时间（毫秒）
     */
    data class Timeout(
        val timeoutMs: Long
    ) : CommandResult()

    /**
     * 检查是否成功
     */
    fun isSuccess(): Boolean = this is Success

    /**
     * 检查是否失败
     */
    fun isFailure(): Boolean = this is Failure

    /**
     * 检查是否超时
     */
    fun isTimeout(): Boolean = this is Timeout

    /**
     * 获取输出内容（如果成功）
     */
    fun getOutputOrNull(): String? = (this as? Success)?.output

    /**
     * 获取错误信息（如果失败）
     */
    fun getErrorMessageOrNull(): String? = (this as? Failure)?.errorMessage

    /**
     * 获取退出代码
     */
    fun getExitCode(): Int = when (this) {
        is Success -> this.code
        is Failure -> this.code
        is Timeout -> -1
    }
}
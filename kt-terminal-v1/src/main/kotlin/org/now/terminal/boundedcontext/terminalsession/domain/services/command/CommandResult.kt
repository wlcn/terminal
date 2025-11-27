package org.now.terminal.boundedcontext.terminalsession.domain.services.command

/**
 * Command execution result
 */
data class CommandResult(
    val exitCode: Int,
    val output: String,
    val errorOutput: String = "",
    val executionTimeMs: Long
) {
    /**
     * Check if command execution was successful
     */
    val isSuccess: Boolean
        get() = exitCode == 0
    
    /**
     * Check if command execution failed
     */
    val isFailure: Boolean
        get() = !isSuccess
    
    /**
     * Get combined output (stdout + stderr)
     */
    val combinedOutput: String
        get() = if (errorOutput.isBlank()) output else "$output\n$errorOutput"
    
    /**
     * Get execution time in seconds
     */
    val executionTimeSeconds: Double
        get() = executionTimeMs / 1000.0
    
    /**
     * Create a successful result
     */
    companion object {
        fun success(output: String, executionTimeMs: Long = 0L): CommandResult {
            return CommandResult(
                exitCode = 0,
                output = output,
                executionTimeMs = executionTimeMs
            )
        }
        
        /**
         * Create a failed result
         */
        fun failure(exitCode: Int, errorOutput: String, executionTimeMs: Long = 0L): CommandResult {
            return CommandResult(
                exitCode = exitCode,
                output = "",
                errorOutput = errorOutput,
                executionTimeMs = executionTimeMs
            )
        }
    }
}
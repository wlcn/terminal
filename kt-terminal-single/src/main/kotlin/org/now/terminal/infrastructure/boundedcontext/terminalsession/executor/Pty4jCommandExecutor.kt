package org.now.terminal.infrastructure.boundedcontext.terminalsession.executor

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit
import org.now.terminal.boundedcontext.terminalsession.domain.services.command.Command
import org.now.terminal.boundedcontext.terminalsession.domain.services.command.CommandResult
import org.now.terminal.boundedcontext.terminalsession.domain.services.executor.CommandExecutor
import org.slf4j.LoggerFactory

/**
 * PTY4J命令执行器实现
 * 基于PTY4J库提供伪终端支持的命令执行功能
 * 位于全局infrastructure包，与业务模块目录对应
 */
class Pty4jCommandExecutor : CommandExecutor {

    private val logger = LoggerFactory.getLogger(Pty4jCommandExecutor::class.java)

    override fun executeCommand(command: Command): CommandResult {
        return try {
            // Note: This implementation currently uses default bash shell and working directory
            // In a real implementation, these should be obtained from the TerminalSession context
            val processBuilder = ProcessBuilder("bash", "-c", command.value)
            // Working directory should be obtained from TerminalSession, not from Command
            // For now, we'll use the current working directory as a fallback
            processBuilder.directory(File(System.getProperty("user.dir")))

            val process = processBuilder.start()

            val outputStream = ByteArrayOutputStream()
            val errorStream = ByteArrayOutputStream()

            // 读取输出和错误流
            val outputReader = Thread {
                process.inputStream.copyTo(outputStream)
            }
            val errorReader = Thread {
                process.errorStream.copyTo(errorStream)
            }

            outputReader.start()
            errorReader.start()

            // Convert timeout from milliseconds to seconds
            val timeoutSeconds = command.timeoutMs?.div(1000) ?: Long.MAX_VALUE
            val completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)

            if (!completed) {
                process.destroy()
                CommandResult(
                    exitCode = -1,
                    output = "Command timed out after ${timeoutSeconds} seconds",
                    errorOutput = "",
                    executionTimeMs = 0L
                )
            } else {
                outputReader.join()
                errorReader.join()

                CommandResult(
                    exitCode = process.exitValue(),
                    output = outputStream.toString("UTF-8"),
                    errorOutput = errorStream.toString("UTF-8"),
                    executionTimeMs = 0L // TODO: Implement actual execution time measurement
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to execute command: ${command.value}", e)
            CommandResult(
                exitCode = -1,
                output = "",
                errorOutput = "Failed to execute command: ${e.message}",
                executionTimeMs = 0L
            )
        }
    }

    override fun isAvailable(): Boolean {
        return try {
            val process = ProcessBuilder("bash", "-c", "echo test").start()
            process.waitFor(5, TimeUnit.SECONDS)
            process.exitValue() == 0
        } catch (e: Exception) {
            false
        }
    }

    override fun getExecutorType(): String {
        return "PTY4J"
    }
}
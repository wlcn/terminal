package org.now.terminal.boundedcontexts.terminalsession.infrastructure.service

import io.ktor.server.application.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.now.terminal.boundedcontexts.terminalsession.domain.TerminalSize
import org.now.terminal.boundedcontexts.terminalsession.domain.model.ShellConfig
import org.now.terminal.boundedcontexts.terminalsession.domain.model.TerminalConfig

/**
 * Terminal Configuration Service
 * Reads terminal configuration from application.conf
 */
class TerminalConfigService(private val application: Application) {

    /**
     * Load terminal configuration from application.conf
     */
    fun loadConfig(): TerminalConfig {
        val config = application.environment.config

        // Read default shell type
        val defaultShellType = config.property("terminal.defaultShellType").getString()

        // Read session timeout in milliseconds
        val sessionTimeoutMs = config.property("terminal.sessionTimeout").getString().toLong()

        // Read default working directory
        val defaultWorkingDirectory = config.property("terminal.defaultWorkingDirectory").getString()

        // Read default terminal size if configured, otherwise use default
        val defaultColumns = config.propertyOrNull("terminal.defaultSize.columns")?.getString()?.toInt() ?: 80
        val defaultRows = config.propertyOrNull("terminal.defaultSize.rows")?.getString()?.toInt() ?: 24
        val defaultTerminalSize = TerminalSize(defaultColumns, defaultRows)

        // Load shells configuration
        val shellsConfig = mutableMapOf<String, ShellConfig>()
        val shellsPath = "terminal.shells"

        // Check if shells configuration exists
        if (config.propertyOrNull(shellsPath) != null) {
            val shells = config.config(shellsPath)
            shells.keys().forEach { shellName ->
                val shell = shells.config(shellName)

                // Read command
                val command = shell.property("command").getString().split(" ")

                // Read optional working directory
                val workingDirectory = shell.propertyOrNull("workingDirectory")?.getString()

                // Read optional size
                val size = shell.propertyOrNull("size.columns")?.let {
                    val columns = shell.property("size.columns").getString().toInt()
                    val rows = shell.property("size.rows").getString().toInt()
                    TerminalSize(columns, rows)
                }

                // Read environment variables
                val environment = mutableMapOf<String, String>()
                shell.config("environment").toMap().forEach { entry ->
                    val key = entry.key
                    entry.value?.also { environment[key] = it.toString() }
                }

                shellsConfig[shellName] = ShellConfig(
                    command = command,
                    workingDirectory = workingDirectory,
                    size = size,
                    environment = environment
                )
            }
        }

        return TerminalConfig(
            defaultShellType = defaultShellType,
            sessionTimeoutMs = sessionTimeoutMs,
            defaultWorkingDirectory = defaultWorkingDirectory,
            defaultTerminalSize = defaultTerminalSize,
            shells = shellsConfig
        )
    }

    /**
     * Load terminal configuration asynchronously
     */
    suspend fun loadConfigAsync(): TerminalConfig {
        return withContext(Dispatchers.Default) {
            val config = application.environment.config
            
            // 并行加载配置的各个部分，提高效率
            val defaultShellType = config.property("terminal.defaultShellType").getString()
            val sessionTimeoutMs = config.property("terminal.sessionTimeout").getString().toLong()
            val defaultWorkingDirectory = config.property("terminal.defaultWorkingDirectory").getString()
            
            // 读取默认终端尺寸
            val defaultColumns = config.propertyOrNull("terminal.defaultSize.columns")?.getString()?.toInt() ?: 80
            val defaultRows = config.propertyOrNull("terminal.defaultSize.rows")?.getString()?.toInt() ?: 24
            val defaultTerminalSize = TerminalSize(defaultColumns, defaultRows)
            
            // 加载shells配置
            val shellsConfig = mutableMapOf<String, ShellConfig>()
            val shellsPath = "terminal.shells"
            
            if (config.propertyOrNull(shellsPath) != null) {
                val shells = config.config(shellsPath)
                shells.keys().forEach { shellName ->
                    val shell = shells.config(shellName)
                    
                    // 读取命令
                    val command = shell.property("command").getString().split(" ")
                    
                    // 读取可选工作目录
                    val workingDirectory = shell.propertyOrNull("workingDirectory")?.getString()
                    
                    // 读取可选尺寸
                    val size = shell.propertyOrNull("size.columns")?.let {
                        val columns = shell.property("size.columns").getString().toInt()
                        val rows = shell.property("size.rows").getString().toInt()
                        TerminalSize(columns, rows)
                    }
                    
                    // 读取环境变量
                    val environment = mutableMapOf<String, String>()
                    shell.config("environment").toMap().forEach { entry ->
                        val key = entry.key
                        entry.value?.also { environment[key] = it.toString() }
                    }
                    
                    shellsConfig[shellName] = ShellConfig(
                        command = command,
                        workingDirectory = workingDirectory,
                        size = size,
                        environment = environment
                    )
                }
            }
            
            TerminalConfig(
                defaultShellType = defaultShellType,
                sessionTimeoutMs = sessionTimeoutMs,
                defaultWorkingDirectory = defaultWorkingDirectory,
                defaultTerminalSize = defaultTerminalSize,
                shells = shellsConfig
            )
        }
    }
}

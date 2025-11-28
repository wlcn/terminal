package org.now.terminal.boundedcontexts.terminalsession.infrastructure.service

import io.ktor.server.config.ApplicationConfig

/**
 * Terminal Size Configuration
 */
data class TerminalSizeConfig(
    val columns: Int,
    val rows: Int
)

/**
 * Shell Configuration
 */
data class ShellConfig(
    val command: List<String>,
    val environment: Map<String, String>,
    val workingDirectory: String? = null,
    val size: TerminalSizeConfig? = null
)

/**
 * Terminal Configuration
 */
data class TerminalConfig(
    val defaultShellType: String,
    val defaultSize: TerminalSizeConfig,
    val defaultWorkingDirectory: String,
    val shells: Map<String, ShellConfig>
)

/**
 * Terminal Config Manager
 * Singleton to manage terminal configuration
 */
object TerminalConfigManager {
    private lateinit var config: ApplicationConfig
    private var cachedConfig: TerminalConfig? = null

    fun init(config: ApplicationConfig) {
        this.config = config
        this.cachedConfig = loadTerminalConfig()
    }

    fun getTerminalConfig(): TerminalConfig {
        return cachedConfig ?: throw IllegalStateException("Config not initialized")
    }

    private fun loadTerminalConfig(): TerminalConfig {
        val terminalConfig = config.config("terminal")

        val defaultShellType = terminalConfig.property("defaultShellType").getString()
        val defaultSize = TerminalSizeConfig(
            terminalConfig.config("defaultSize").property("columns").getString().toInt(),
            terminalConfig.config("defaultSize").property("rows").getString().toInt()
        )
        val defaultWorkingDirectory = terminalConfig.property("defaultWorkingDirectory").getString()

        val shells = mutableMapOf<String, ShellConfig>()

        val shellsConfig = terminalConfig.config("shells")
        val shellsKeys = shellsConfig.toMap().keys

        shellsKeys.forEach { shellName ->
            val shellConfig = shellsConfig.config(shellName)
            val command = shellConfig.property("command").getList()
            val environment = mutableMapOf<String, String>()
            val envConfig = shellConfig.config("environment")
            envConfig.keys().forEach { key ->
                environment[key] = envConfig.property(key).getString()
            }
            
            val workingDirectory = try {
                shellConfig.property("workingDirectory").getString()
            } catch (e: Exception) {
                null
            }
            
            // 读取shell级别的终端尺寸（可选）
            val size = try {
                val sizeConfig = shellConfig.config("size")
                TerminalSizeConfig(
                    sizeConfig.property("columns").getString().toInt(),
                    sizeConfig.property("rows").getString().toInt()
                )
            } catch (e: Exception) {
                null
            }
            
            shells[shellName] = ShellConfig(command, environment, workingDirectory, size)
        }

        return TerminalConfig(defaultShellType, defaultSize, defaultWorkingDirectory, shells)
    }
}
package org.now.terminal.infrastructure.configuration

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import com.typesafe.config.ConfigResolveOptions
import org.slf4j.LoggerFactory
import java.io.File

/**
 * 配置加载器 - 负责加载和解析配置文件
 */
object ConfigLoader {
    private val logger = LoggerFactory.getLogger(ConfigLoader::class.java)
    
    /**
     * 加载应用程序配置
     * @param configPath 配置文件路径（可选，默认为类路径下的application.conf）
     * @param environment 环境名称（可选，用于加载环境特定配置）
     * @param osType 操作系统类型（可选，用于加载操作系统特定配置，如"windows"、"linux"）
     */
    fun loadAppConfig(configPath: String? = null, environment: String? = null, osType: String? = null): AppConfig {
        try {
            val baseConfig = loadBaseConfig(configPath)
            val envConfig = loadEnvironmentConfig(environment)
            val osConfig = loadOperatingSystemConfig(osType)
            
            // 合并配置（优先级：环境配置 > 操作系统配置 > 基础配置）
            val finalConfig = envConfig.withFallback(osConfig).withFallback(baseConfig)
            
            // 转换为AppConfig对象
            return convertToAppConfig(finalConfig)
        } catch (e: Exception) {
            logger.error("Failed to load application configuration", e)
            throw ConfigurationException("Failed to load application configuration", e)
        }
    }
    
    /**
     * 加载基础配置
     */
    private fun loadBaseConfig(configPath: String? = null): com.typesafe.config.Config {
        return if (configPath != null) {
            // 从指定路径加载配置
            val configFile = File(configPath)
            if (configFile.exists()) {
                ConfigFactory.parseFile(configFile, ConfigParseOptions.defaults())
                    .resolve(ConfigResolveOptions.defaults())
            } else {
                logger.warn("Config file not found at: {}. Using default configuration.", configPath)
                ConfigFactory.load()
            }
        } else {
            // 从类路径加载默认配置
            ConfigFactory.load()
        }
    }
    
    /**
     * 加载环境特定配置
     */
    private fun loadEnvironmentConfig(environment: String?): com.typesafe.config.Config {
        if (environment == null) {
            return ConfigFactory.empty()
        }
        
        val envConfigName = "application-$environment.conf"
        try {
            return ConfigFactory.parseResources(envConfigName)
                .resolve(ConfigResolveOptions.defaults())
        } catch (e: Exception) {
            logger.debug("Environment specific config not found: {}", envConfigName)
            return ConfigFactory.empty()
        }
    }
    
    /**
     * 加载操作系统特定配置
     * @param osType 操作系统类型（可选，如"windows"、"linux"，如果为null则自动检测）
     */
    private fun loadOperatingSystemConfig(osType: String? = null): com.typesafe.config.Config {
        val targetOsType = osType ?: detectOperatingSystem()
        
        if (targetOsType == null) {
            logger.debug("Unable to detect operating system, skipping OS-specific configuration")
            return ConfigFactory.empty()
        }
        
        val osConfigName = "application-$targetOsType.conf"
        try {
            val config = ConfigFactory.parseResources(osConfigName)
                .resolve(ConfigResolveOptions.defaults())
            logger.info("Loaded OS-specific configuration for: {}", targetOsType)
            return config
        } catch (e: Exception) {
            logger.debug("OS-specific config not found: {}", osConfigName)
            return ConfigFactory.empty()
        }
    }
    
    /**
     * 检测当前操作系统类型
     */
    private fun detectOperatingSystem(): String? {
        return when {
            System.getProperty("os.name").lowercase().contains("windows") -> "windows"
            System.getProperty("os.name").lowercase().contains("linux") -> "linux"
            System.getProperty("os.name").lowercase().contains("mac") -> "mac"
            else -> null
        }
    }
    
    /**
     * 将Typesafe Config转换为AppConfig对象
     */
    private fun convertToAppConfig(config: com.typesafe.config.Config): AppConfig {
        val appConfig = config.getConfig("app")
        
        return AppConfig(
            name = appConfig.getString("name"),
            version = appConfig.getString("version"),
            environment = appConfig.getString("environment"),
            server = convertToServerConfig(appConfig.getConfig("server")),
            database = convertToDatabaseConfig(appConfig.getConfig("database")),
            eventBus = convertToEventBusConfig(appConfig.getConfig("eventBus")),
            logging = convertToLoggingConfig(appConfig.getConfig("logging")),
            monitoring = convertToMonitoringConfig(appConfig.getConfig("monitoring")),
            terminal = convertToTerminalConfig(appConfig.getConfig("terminal"))
        )
    }
    
    private fun convertToServerConfig(config: com.typesafe.config.Config): ServerConfig {
        val corsConfig = config.getConfig("cors")
        
        return ServerConfig(
            port = config.getInt("port"),
            host = config.getString("host"),
            contextPath = config.getString("contextPath"),
            cors = CorsConfig(
                allowedOrigins = corsConfig.getStringList("allowedOrigins"),
                allowedMethods = corsConfig.getStringList("allowedMethods"),
                allowedHeaders = corsConfig.getStringList("allowedHeaders"),
                allowCredentials = corsConfig.getBoolean("allowCredentials")
            )
        )
    }
    
    private fun convertToDatabaseConfig(config: com.typesafe.config.Config): DatabaseConfig {
        return DatabaseConfig(
            url = config.getString("url"),
            driver = config.getString("driver"),
            username = config.getString("username"),
            password = config.getString("password"),
            poolSize = config.getInt("poolSize"),
            connectionTimeout = config.getLong("connectionTimeout"),
            maxLifetime = config.getLong("maxLifetime")
        )
    }
    
    private fun convertToEventBusConfig(config: com.typesafe.config.Config): EventBusConfig {
        return EventBusConfig(
            bufferSize = config.getInt("bufferSize"),
            enableMetrics = config.getBoolean("enableMetrics"),
            enableDeadLetterQueue = config.getBoolean("enableDeadLetterQueue"),
            deadLetterQueueCapacity = config.getInt("deadLetterQueueCapacity"),
            maxRetries = config.getInt("maxRetries")
        )
    }
    
    private fun convertToLoggingConfig(config: com.typesafe.config.Config): LoggingConfig {
        val fileConfig = config.getConfig("file")
        
        return LoggingConfig(
            level = config.getString("level"),
            file = LogFileConfig(
                enabled = fileConfig.getBoolean("enabled"),
                path = fileConfig.getString("path"),
                maxFileSize = fileConfig.getString("maxFileSize"),
                maxHistory = fileConfig.getInt("maxHistory")
            ),
            format = config.getString("format")
        )
    }
    
    private fun convertToMonitoringConfig(config: com.typesafe.config.Config): MonitoringConfig {
        val metricsConfig = config.getConfig("metrics")
        val healthConfig = config.getConfig("health")
        
        return MonitoringConfig(
            enabled = config.getBoolean("enabled"),
            metrics = MetricsConfig(
                enabled = metricsConfig.getBoolean("enabled"),
                exportInterval = metricsConfig.getLong("exportInterval"),
                endpoints = metricsConfig.getStringList("endpoints")
            ),
            health = HealthConfig(
                enabled = healthConfig.getBoolean("enabled"),
                checkInterval = healthConfig.getLong("checkInterval"),
                endpoints = healthConfig.getStringList("endpoints")
            )
        )
    }
    
    private fun convertToTerminalConfig(config: com.typesafe.config.Config): TerminalConfig {
        val ptyConfig = config.getConfig("pty")
        
        return TerminalConfig(
            defaultTerm = config.getString("defaultTerm"),
            maxSessionsPerUser = config.getInt("maxSessionsPerUser"),
            sessionTimeout = config.getLong("sessionTimeout"),
            bufferSize = config.getInt("bufferSize"),
            pty = convertToPtyConfig(ptyConfig)
        )
    }
    
    private fun convertToPtyConfig(config: com.typesafe.config.Config): PtyConfig {
        val defaultEnvironment = config.getConfig("defaultEnvironment")
        val envMap = mutableMapOf<String, String>()
        
        defaultEnvironment.entrySet().forEach { entry ->
            envMap[entry.key] = entry.value.unwrapped().toString()
        }
        
        return PtyConfig(
            defaultCommand = config.getString("defaultCommand"),
            defaultWorkingDirectory = config.getString("defaultWorkingDirectory"),
            defaultEnvironment = envMap,
            shellType = config.getString("shellType"),
            customShellPath = config.getString("customShellPath")
        )
    }
    
    /**
     * 验证配置的完整性
     */
    fun validateConfig(config: AppConfig): Boolean {
        // 基本验证
        if (config.name.isBlank()) {
            throw ConfigurationException("Application name cannot be blank")
        }
        
        if (config.server.port <= 0 || config.server.port > 65535) {
            throw ConfigurationException("Server port must be between 1 and 65535")
        }
        
        if (config.database.poolSize <= 0) {
            throw ConfigurationException("Database pool size must be positive")
        }
        
        return true
    }
}

/**
 * 配置异常类
 */
class ConfigurationException(message: String, cause: Throwable? = null) : 
    RuntimeException(message, cause)
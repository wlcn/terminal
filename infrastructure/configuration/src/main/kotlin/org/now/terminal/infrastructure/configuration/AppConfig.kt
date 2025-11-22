package org.now.terminal.infrastructure.configuration

import kotlinx.serialization.Serializable

/**
 * 应用程序主配置类
 */
@Serializable
data class AppConfig(
    val name: String = "kt-terminal",
    val version: String = "1.0.0",
    val environment: String = "dev",
    val server: ServerConfig = ServerConfig(),
    val database: DatabaseConfig = DatabaseConfig(),
    val eventBus: EventBusConfig = EventBusConfig(),
    val logging: LoggingConfig = LoggingConfig(),
    val monitoring: MonitoringConfig = MonitoringConfig(),
    val terminal: TerminalConfig = TerminalConfig()
)

/**
 * 服务器配置
 */
@Serializable
data class ServerConfig(
    val port: Int = 8080,
    val host: String = "localhost",
    val contextPath: String = "/api",
    val cors: CorsConfig = CorsConfig()
)

/**
 * CORS配置
 */
@Serializable
data class CorsConfig(
    val allowedOrigins: List<String> = listOf("*"),
    val allowedMethods: List<String> = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS"),
    val allowedHeaders: List<String> = listOf("*"),
    val allowCredentials: Boolean = true
)

/**
 * 数据库配置
 */
@Serializable
data class DatabaseConfig(
    val url: String = "jdbc:h2:mem:testdb",
    val driver: String = "org.h2.Driver",
    val username: String = "sa",
    val password: String = "",
    val poolSize: Int = 10,
    val connectionTimeout: Long = 30000,
    val maxLifetime: Long = 1800000
)

/**
 * 事件总线配置
 */
@Serializable
data class EventBusConfig(
    val bufferSize: Int = 1000,
    val enableMetrics: Boolean = true,
    val enableDeadLetterQueue: Boolean = true,
    val deadLetterQueueCapacity: Int = 100,
    val maxRetries: Int = 3
)

/**
 * 日志配置
 */
@Serializable
data class LoggingConfig(
    val level: String = "INFO",
    val file: LogFileConfig = LogFileConfig(),
    val format: String = "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
)

/**
 * 日志文件配置
 */
@Serializable
data class LogFileConfig(
    val enabled: Boolean = false,
    val path: String = "logs/application.log",
    val maxFileSize: String = "10MB",
    val maxHistory: Int = 30
)

/**
 * 监控配置
 */
@Serializable
data class MonitoringConfig(
    val enabled: Boolean = true,
    val metrics: MetricsConfig = MetricsConfig(),
    val health: HealthConfig = HealthConfig()
)

/**
 * 指标配置
 */
@Serializable
data class MetricsConfig(
    val enabled: Boolean = true,
    val exportInterval: Long = 60000,
    val endpoints: List<String> = listOf("jvm", "system", "process")
)

/**
 * 健康检查配置
 */
@Serializable
data class HealthConfig(
    val enabled: Boolean = true,
    val checkInterval: Long = 30000,
    val endpoints: List<String> = listOf("disk", "database", "memory")
)

/**
 * 终端会话配置
 */
@Serializable
data class TerminalConfig(
    val defaultTerm: String = "xterm",
    val maxSessionsPerUser: Int = 10,
    val sessionTimeout: Long = 3600000, // 1小时
    val bufferSize: Int = 8192,
    val pty: PtyConfig = PtyConfig()
)

/**
 * PTY配置
 */
@Serializable
data class PtyConfig(
    val defaultCommand: String = "/bin/bash",
    val defaultWorkingDirectory: String = "/home/user",
    val defaultEnvironment: Map<String, String> = mapOf(
        "TERM" to "xterm",
        "HOME" to "/home/user",
        "PATH" to "/usr/local/bin:/usr/bin:/bin"
    )
)
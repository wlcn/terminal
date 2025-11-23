pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

// 启用版本目录功能
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "kt-terminal"

// 包含所有模块
include("shared-kernel")

// 基础设施层模块
include("infrastructure:event-bus")
include("infrastructure:monitoring")
include("infrastructure:configuration")
include("infrastructure:logging")

// 限界上下文模块
include("bounded-contexts:terminal-session")
include("bounded-contexts:file-transfer")
include("bounded-contexts:collaboration")
include("bounded-contexts:audit-logging")



// 应用层模块
include("applications:terminal-server")

// Gateway层模块
// include("gateways:websocket-gateway") // 已迁移到应用层
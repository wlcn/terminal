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

// 限界上下文模块
include("bounded-contexts:terminal-session")
include("bounded-contexts:file-transfer")
include("bounded-contexts:collaboration")
include("bounded-contexts:audit-logging")

// 端口层模块
include("ports:cli-port")
include("ports:http-port")
include("ports:websocket-port")

// 防腐层模块
include("anti-corruption-layers:session-acl")
include("anti-corruption-layers:filetransfer-acl")

// 应用层模块
include("applications:cli-application")
include("applications:ktor-application")
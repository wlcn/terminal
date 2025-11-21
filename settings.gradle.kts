pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

// 启用版本目录功能
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "kt-terminal"

// 只包含共享内核模块
include("shared-kernel")
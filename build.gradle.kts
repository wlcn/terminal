plugins {
    // 应用Kotlin JVM插件到所有子项目，使用版本目录中的配置
    alias(libs.plugins.kotlin.jvm) apply false
}

// 为所有子项目配置仓库
subprojects {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
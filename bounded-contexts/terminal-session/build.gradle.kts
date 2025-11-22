plugins {
    id("org.jetbrains.kotlin.jvm")
    kotlin("plugin.serialization") version "2.2.21"
}

repositories {
    mavenCentral()
}

dependencies {
    // 项目内部依赖
    implementation(project(":shared-kernel"))
    implementation(project(":infrastructure:configuration"))
    implementation(project(":infrastructure:logging"))
    implementation(project(":infrastructure:event-bus"))
    
    
    // Kotlin标准库
    implementation(libs.kotlin.stdlib)
    
    // Kotlin协程
    implementation(libs.kotlinx.coroutines.core)
    
    // Kotlin序列化
    implementation(libs.kotlinx.serialization.json)
    
    // Pty4j - 伪终端实现
    implementation(libs.pty4j)
    
    // Koin依赖注入框架
    implementation(libs.koin.core)
    
    // SLF4J API依赖
    implementation(libs.slf4j.api)
    
    // Test dependencies - Kotest (现代化测试框架)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.framework.engine)
    
    // Test dependencies - Mockk (Kotlin模拟框架)
    testImplementation(libs.mockk)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
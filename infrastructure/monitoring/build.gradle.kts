plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    // 项目内部依赖
    implementation(project(":shared-kernel"))
    implementation(project(":infrastructure:event-bus"))
    
    // Kotlin标准库
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    
    // 监控相关依赖
    implementation(libs.micrometer.core)
    implementation(libs.micrometer.prometheus)
    
    // HTTP服务器（用于暴露指标）
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.metrics.micrometer)
    
    // 日志
    implementation(libs.logback.classic)
    
    // 测试依赖
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}


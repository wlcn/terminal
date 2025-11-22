plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    // 项目内部依赖
    implementation(project(":shared-kernel"))
    implementation(project(":infrastructure:configuration"))
    implementation(project(":infrastructure:logging"))
    
    // Kotlin标准库
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    
    // Kotlin序列化
    implementation(libs.kotlinx.serialization.json)
    
    // 日志
    implementation(libs.logback.classic)
    
    // Koin依赖注入
    implementation(libs.koin.core)
    
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
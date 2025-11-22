plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    // 项目内部依赖
    implementation(project(":shared-kernel"))
    
    // Kotlin标准库
    implementation(libs.kotlin.stdlib)
    
    // Kotlin序列化（编译时注解）
    compileOnly(libs.kotlinx.serialization.json)
    
    // 配置框架 - Kotlinx Configuration
    implementation(libs.typesafe.config)
    implementation(libs.kaml)
    
    // 日志
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
    
    // Koin依赖注入
    implementation(libs.koin.core)
    
    // 测试依赖
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.mockk)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
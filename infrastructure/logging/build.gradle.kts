plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    // 项目内部依赖
    implementation(project(":shared-kernel"))
    implementation(project(":infrastructure:configuration"))
    
    // Kotlin
    implementation(libs.kotlin.stdlib)
    
    // 日志依赖
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
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

dependencies {
    // 项目内部依赖
    implementation(project(":shared-kernel"))
    implementation(project(":infrastructure:configuration"))
    implementation(project(":infrastructure:event-bus"))
    implementation(project(":infrastructure:logging"))
    
    // 业务接口依赖（仅依赖接口，不依赖业务逻辑）
    implementation(project(":bounded-contexts:terminal-session"))
    
    // Kotlin标准库
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    
    // Kotlin序列化
    implementation(libs.kotlinx.serialization.json)
    
    // Ktor WebSocket
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    
    // 日志
    implementation(libs.logback.classic)
    
    // 依赖注入
    implementation(libs.koin.core)
    implementation(libs.koin.ktor)
    
    // 测试依赖
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.ktor.server.test.host)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

application {
    mainClass = "org.now.terminal.server.TerminalServerApplication"
}
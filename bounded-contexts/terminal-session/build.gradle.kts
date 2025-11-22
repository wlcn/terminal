plugins {
    id("org.jetbrains.kotlin.jvm")
    kotlin("plugin.serialization") version "2.2.21"
    id("jacoco")
}

// 集成测试使用test源集，但通过目录区分

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
    
    // Koin测试依赖
    testImplementation(libs.koin.test)
    
    // Test dependencies - Kotest (现代化测试框架)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.framework.engine)
    
    // Test dependencies - Mockk (Kotlin模拟框架)
    testImplementation(libs.mockk)
    
    // Test dependencies - Kotlin Coroutines Test
    testImplementation(libs.kotlinx.coroutines.test)
    
    // 集成测试使用test依赖，无需额外声明
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.95".toBigDecimal() // 要求95%的覆盖率
            }
        }
    }
}

// 集成测试通过test任务运行，使用test源集
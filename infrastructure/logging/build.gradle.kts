plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    // 项目内部依赖
    implementation(project(":shared-kernel"))
    implementation(project(":infrastructure:configuration"))
    
    // Kotlin
    implementation(libs.findLibrary("kotlin-stdlib").get())
    implementation(libs.findLibrary("kotlinx-serialization-json").get())
    
    // 日志依赖
    implementation(libs.findLibrary("slf4j-api").get())
    implementation(libs.findLibrary("logback-classic").get())
    
    // 测试依赖
    testImplementation(libs.findLibrary("kotest-runner-junit5").get())
    testImplementation(libs.findLibrary("kotest-assertions-core").get())
    testImplementation(libs.findLibrary("mockk").get())
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
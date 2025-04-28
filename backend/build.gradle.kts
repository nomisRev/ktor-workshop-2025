import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.assert)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
}

group = "org.jetbrains"
version = "0.0.1"
application.mainClass = "io.ktor.server.netty.EngineMain"

dependencies {
    implementation(libs.bundles.ktor.server)
    implementation(libs.ktor.client.apache)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.bundles.exposed)
    implementation(libs.logback.classic)
    implementation(libs.kotlinx.datetime)
    implementation(libs.bundles.flyway)
    implementation(libs.bundles.langchain4j)
    implementation(libs.micrometer.registry.prometheus)

    testImplementation(libs.bundles.ktor.client)
    testImplementation(libs.bundles.testing)
}

tasks {
    withType<KotlinCompile> {
        compilerOptions {
            // Needed for LangChain4J reflection tricks
            // Maintains the parameter names instead of replacing with $0, $1, etc.
            javaParameters = true
        }
    }
}

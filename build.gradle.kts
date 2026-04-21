plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
}

group = "MusicPlayerServer"
version = "0.0.1"
val ktor_version: String by project

application {
    mainClass = "io.ktor.server.cio.EngineMain"
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.cio)
    implementation(libs.logback.classic)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)

    // Database logic
    implementation("org.jetbrains.exposed:exposed-core:0.41.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.41.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.41.1")

// SQLite Driver
    implementation("org.xerial:sqlite-jdbc:3.42.0.0")
    implementation("io.ktor:ktor-server-call-logging:2.3.12")
    implementation("io.ktor:ktor-server-request-validation:2.3.12")
    implementation("io.ktor:ktor-server-status-pages:2.3.12")
    implementation("io.ktor:ktor-server-auth:${ktor_version}")
    implementation("io.ktor:ktor-server-auth-jwt:${ktor_version}")
    implementation("org.mindrot:jbcrypt:0.4")

    implementation("io.ktor:ktor-server-swagger:$ktor_version")
    implementation("io.ktor:ktor-server-openapi:$ktor_version")
    // Redis Client (Jedis) - Using the latest 2026 stable version
    implementation("redis.clients:jedis:5.1.0")

    // For turning your Character objects into JSON strings for Redis
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

}

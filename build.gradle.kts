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
    // Standard Ktor 3.x modules
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.cio)
    implementation(libs.logback.classic)

    // Test dependencies
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)

    // Database logic (Exposed)
    implementation("org.jetbrains.exposed:exposed-core:0.41.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.41.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.41.1")
    implementation("org.postgresql:postgresql:42.7.2")

    // Auth & Security (USE THE VARIABLE VERSION)
    implementation("io.ktor:ktor-server-auth:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages:$ktor_version")
    implementation("io.ktor:ktor-server-request-validation:$ktor_version")
    implementation("org.mindrot:jbcrypt:0.4")

    // Utils (CRITICAL for asStream)
    implementation("io.ktor:ktor-utils:$ktor_version")
    implementation("io.ktor:ktor-io:$ktor_version") // Add this one too!

    // Documentation & Cache
    implementation("io.ktor:ktor-server-swagger:$ktor_version")
    implementation("io.ktor:ktor-server-openapi:$ktor_version")
    implementation("redis.clients:jedis:5.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")

    testImplementation("com.h2database:h2:2.2.224")
    testImplementation("io.ktor:ktor-client-content-negotiation:${ktor_version}")
}

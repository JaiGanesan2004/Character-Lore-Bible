package LoreBible.com.lorebible

import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import org.slf4j.event.*

fun Application.configureMonitoring() {
    install(CallLogging) {
        level = Level.INFO

        // Only log requests that start with '/' after domain name. Eg: http://localhost:8080'/'character
        filter { call -> call.request.path().startsWith("/") }
    }
}

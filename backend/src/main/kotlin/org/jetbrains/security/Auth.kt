package org.jetbrains.security

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.sessions.SessionTransportTransformerEncrypt
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.server.sessions.maxAge
import io.ktor.util.hex
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.minutes

@Serializable
data class AuthConfig(
    val encryptionKey: String,
    val signKey: String,
)

@Serializable
data class UserSession(val userId: String)

fun Application.configureSecurity(config: AuthConfig) {
    configureOAuth(config)
    configureSession(config)
}

private fun Application.configureOAuth(config: AuthConfig) {
    // https://ktor.io/docs/server-oauth.html
}

private fun Application.configureSession(config: AuthConfig) {
    val shouldBeSecure = !developmentMode
    install(Sessions) {
        cookie<UserSession>("SESSION") {
            cookie.secure = shouldBeSecure
            cookie.extensions["SameSite"] = "lax"
            cookie.maxAge = 5.minutes
            cookie.httpOnly = true
            transform(SessionTransportTransformerEncrypt(hex(config.encryptionKey), hex(config.signKey)))
        }
    }
}

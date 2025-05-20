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
data class UserSession(val email: String)

fun Application.configureSecurity(config: AuthConfig) {
    configureOAuth(config)
    configureJwt(config)
    configureSession(config)
}

private fun Application.configureOAuth(config: AuthConfig) {
    /**
     * TODO Configure OAuth to login with Google.
     *   Setup OAuth Plugin with Google with following configuration.
     *     redirectUrl: "http://localhost:8000/callback"
     *     authorizeUrl: "https://accounts.google.com/o/oauth2/auth"
     *     accessTokenUrl: "https://oauth2.googleapis.com/token"
     *     And secrets provided during workshop.
     *     Follow [documentation](https://ktor.io/docs/server-oauth.html) to setup required routes
     *     and finally install the `UserSession` with the users `email`.
     */
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

/**
 * TODO Configure JWT to verify id_token, or access_token, issued by Google.
 *   Setup JWT Plugin with Jwk with following configuration.
 *     jwkUrl: "https://www.googleapis.com/oauth2/v3/certs"
 *     issuer: "https://accounts.google.com"
 *   Create a route, and test the JWT flow with a token issued by the login flow.
 *   Use debugging, or logging to retrieve the id_token.
 *   https://ktor.io/docs/server-jwt.html
 */
private fun Application.configureJwt(config: AuthConfig) {
}
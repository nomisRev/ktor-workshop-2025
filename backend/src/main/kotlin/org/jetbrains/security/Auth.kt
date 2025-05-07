package org.jetbrains.security

import com.auth0.jwt.JWT
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode.Companion.Unauthorized
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.OAuthAccessTokenResponse.OAuth2
import io.ktor.server.auth.OAuthServerSettings
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.auth.oauth
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sessions.SessionTransportTransformerEncrypt
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.server.sessions.maxAge
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.ktor.util.hex
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.minutes

@Serializable
data class AuthConfig(
    val encryptionKey: String,
    val signKey: String,
    val authorizeUrl: String,
    val accessTokenUrl: String,
    val clientId: String,
    val clientSecret: String,
    val redirectUrl: String,
)

@Serializable
data class UserSession(val userId: String)

fun Application.configureSecurity(config: AuthConfig) {
    configureOAuth(config)
    configureSession(config)
}

private fun Application.configureOAuth(config: AuthConfig) {
    // https://ktor.io/docs/server-oauth.html
    authentication {
        oauth {
            urlProvider = { config.redirectUrl }
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "google",
                    authorizeUrl = config.authorizeUrl,
                    accessTokenUrl = config.accessTokenUrl,
                    requestMethod = HttpMethod.Post,
                    clientId = config.clientId,
                    clientSecret = config.clientSecret,
                    extraAuthParameters = listOf("access_type" to "offline"),
                    defaultScopes = listOf("https://www.googleapis.com/auth/userinfo.profile"),
                )
            }
            client = HttpClient(Apache) { install(ContentNegotiation) { json() }  }
        }
    }
    routing {
        authenticate {
            get("/login") {
                // The OAuth plugin will intercept this request and redirect to Google
                // No need to do anything here
            }

            get("/callback") {
                val principal: OAuth2? = call.authentication.principal()
                if (principal == null) call.respond(Unauthorized)
                else {
                    val userSession =
                        UserSession(JWT.decode(principal.extraParameters["id_token"]).subject)
                    call.sessions.set(userSession)
                    call.respondRedirect("/")
                }
            }
        }
    }
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

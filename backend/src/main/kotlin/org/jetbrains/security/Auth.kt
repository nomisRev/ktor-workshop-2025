package org.jetbrains.security

import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.OAuthAccessTokenResponse.OAuth2
import io.ktor.server.auth.OAuthServerSettings
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.oauth
import io.ktor.server.auth.principal
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
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes

@Serializable
data class AuthConfig(
    val encryptionKey: String,
    val signKey: String,
    val clientId: String,
    val clientSecret: String,
)

@Serializable
data class UserSession(val email: String)

fun Application.configureSecurity(config: AuthConfig) {
    configureOAuth(config)
    configureJwt(config)
    configureSession(config)
}

private fun Application.configureOAuth(config: AuthConfig) {
    authentication {
        oauth("oauth") {
            urlProvider = { "http://localhost:8000/callback" }
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "google",
                    authorizeUrl = "https://accounts.google.com/o/oauth2/auth",
                    accessTokenUrl = "https://oauth2.googleapis.com/token",
                    requestMethod = HttpMethod.Post,
                    clientId = config.clientId,
                    clientSecret = config.clientSecret,
                    defaultScopes = listOf("email"),
                )
            }
            client = HttpClient(CIO) { install(ContentNegotiation) { json() } }
        }
    }
    routing {
        authenticate("oauth") {
            get("/login") {
                // The OAuth plugin will intercept this request and redirect to Google
                // No need to do anything here
            }

            get("/callback") {
                val principal: OAuth2? = call.authentication.principal()
                val email = principal?.extraParameters["id_token"]
                    ?.let(JWT::decode)
                    ?.getClaim("email")
                    ?.asString()

                if (email != null) {
                    call.sessions.set(UserSession(email))
                    call.respondRedirect("/")
                } else {
                    call.respond(HttpStatusCode.Unauthorized)
                    call.respondRedirect("/login")
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


private fun Application.configureJwt(config: AuthConfig) {
    authentication {
        jwt("jwt") {
            val provider =
                JwkProviderBuilder(URL("https://www.googleapis.com/oauth2/v3/certs"))
                    .cached(10, 24, TimeUnit.HOURS)
                    .rateLimited(10, 1, TimeUnit.MINUTES)
                    .build()

            verifier(provider, "https://accounts.google.com")
            validate { credential ->
                val isCorrect = credential.audience.single() == config.clientId
                val email = credential.payload.claims["email"]?.asString()
                if (isCorrect && email != null) UserSession(email) else null
            }
        }
    }

    routing {
        authenticate("jwt") {
            get("/token") {
                val user = call.principal<UserSession>()!!
                call.respond("Hi, ${user.email}")
            }
        }
    }
}

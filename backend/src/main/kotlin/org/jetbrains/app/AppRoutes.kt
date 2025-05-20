package org.jetbrains.app

import io.ktor.server.http.content.staticResources
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import org.jetbrains.security.UserSession

fun Routing.configureChatRoutes() {
    get("/") {
        val hasSession = call.sessions.get<UserSession>() != null
        val redirectUrl = if (hasSession) "/home" else "login"
        call.respondRedirect(redirectUrl)
    }

    staticResources("/", "web")
    staticResources("/login", "web")
    staticResources("/home", "web") {
        modify { _, call ->
            if (call.sessions.get<UserSession>() == null) call.respondRedirect("/login")
        }
    }
}

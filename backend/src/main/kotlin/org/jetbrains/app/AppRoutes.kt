package org.jetbrains.app

import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Routing
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.ai.TravelService
import org.jetbrains.security.UserSession

fun Routing.configureChatRoutes() {
    get("/") {
        val hasSession = call.sessions.get<UserSession>() != null
        val redirectUrl = if (hasSession) "/home" else "login"
        call.respondRedirect(redirectUrl)
    }

    staticResources("/", "web")
    staticResources("/home", "web") {
        modify { _, call ->
            if (call.sessions.get<UserSession>() == null) call.respondRedirect("/login")
        }
    }

    val travelService: TravelService by application.dependencies

    /**
     * TODO: Setup webSocket communication between AI, and client.
     *   1. Verify the user is logged in, and act appropriately if not.
     *   2. Introduce yourself as the travel assistant AI.
     *   3. When a message is received from the user, send it to the AI, and stream the response back to the user.
     *     => Think about how to signal the end of the answer.
     *   4. Handle possible errors
     *   Do not forget to setup the WebSocket plugin in Application.kt
     *
     * https://ktor.io/docs/server-websockets.html
     * Endpoint: /ws
     */

    /**
     * TODO: Answer a single question in a streaming fashion using Server Sent Events (SSE).
     *   1.  Verify the user is logged in, and act appropriately if not.
     *   2. Receive the question, and stream the answer back to the user.
     *     => Think about how to signal the end of the answer.
     *   3. Handle possible errors
     *   Do not forget to setup the SSE plugin in Application.kt
     * https://ktor.io/docs/server-server-sent-events.html
     * Endpoint: /sse
     */
}

@Serializable
private sealed interface Message {
    @Serializable
    @SerialName("partial_answer")
    data class PartialAnswer(val token: String) : Message

    @Serializable
    @SerialName("answer_end")
    data object AnswerEnd : Message

    @Serializable
    @SerialName("error")
    data class Error(val text: String) : Message
}

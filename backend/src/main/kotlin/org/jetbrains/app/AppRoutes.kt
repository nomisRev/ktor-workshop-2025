package org.jetbrains.app

import io.ktor.http.HttpStatusCode
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.di.provideDelegate
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Routing
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sse.send
import io.ktor.server.sse.sse
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.jetbrains.ai.TravelService
import org.jetbrains.app.Message.AnswerEnd
import org.jetbrains.app.Message.PartialAnswer
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

    val ai: TravelService by application.dependencies

    /**
     * TODO: Setup webSocket communication between AI, and client.
     *   1. Verify the user is logged in, and act appropriately if not.
     *   2. Introduce yourself as the travel assistant AI.
     *   3. When a message is received from the user, send it to the AI, and stream the response back to the user.
     *     => Think about how to signal the end of the answer.
     *   4. Handle possible errors
     */
    webSocket("/ws") {
        val session = call.sessions.get<UserSession>()
        if (session == null) return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No session"))

        sendSerialized<Message>(PartialAnswer("Hey, I am your personal travel assistant. How may I help you today?"))
        sendSerialized<Message>(AnswerEnd)
        incoming.consumeAsFlow().filterIsInstance<Frame.Text>().collect { frame ->
            val question = frame.readText()
            ai.answer(session.userId, question)
                .catch { throwable ->
                    sendSerialized<Message>(Message.Error("Something went wrong... Please refresh."))
                    throwable.printStackTrace()
                }
                .collect { sendSerialized<Message>(PartialAnswer(it)) }
            sendSerialized<Message>(AnswerEnd)
        }
    }

    /**
     * TODO: Answer a single question in a streaming fashion.
     *   1.  Verify the user is logged in, and act appropriately if not.
     *   2. Receive the question, and stream the answer back to the user.
     *     => Think about how to signal the end of the answer.
     *   3. Handle possible errors
     */
    sse("/chat", { _, value -> Json.encodeToString(serializer(), value as Message) }) {
        val session = call.sessions.get<UserSession>() ?: return@sse call.respond(HttpStatusCode.Unauthorized)
        val question = call.request.queryParameters["question"] ?: return@sse call.respond(HttpStatusCode.BadRequest)
        ai.answer(session.userId, question)
            .catch { throwable ->
                send(Message.Error("Something went wrong... Please refresh."))
                throwable.printStackTrace()
            }
            .collect { token -> send(PartialAnswer(token)) }
        send(AnswerEnd)
    }
}

@Serializable
sealed interface Message {
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

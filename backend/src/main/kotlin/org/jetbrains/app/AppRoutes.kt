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

    webSocket("/ws") {
        val session = call.sessions.get<UserSession>()
        if (session == null) return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No session"))

        sendSerialized<Message>(Message.PartialAnswer("Hey, I am your personal travel assistant. How may I help you today?"))
        sendSerialized<Message>(Message.AnswerEnd)
        incoming.consumeAsFlow().filterIsInstance<Frame.Text>().collect { frame ->
            val question = frame.readText()
            travelService.answer(session.email, question)
                .catch { throwable ->
                    sendSerialized<Message>(Message.Error("Something went wrong... Please refresh."))
                    throwable.printStackTrace()
                }
                .collect { sendSerialized<Message>(Message.PartialAnswer(it)) }
            sendSerialized<Message>(Message.AnswerEnd)
        }
    }

    sse("/chat", { _, value -> Json.encodeToString(serializer(), value as Message) }) {
        val session = call.sessions.get<UserSession>() ?: return@sse call.respond(HttpStatusCode.Unauthorized)
        val question = call.request.queryParameters["question"] ?: return@sse call.respond(HttpStatusCode.BadRequest)
        travelService.answer(session.email, question)
            .catch { throwable ->
                send(Message.Error("Something went wrong... Please refresh."))
                throwable.printStackTrace()
            }
            .collect { token -> send(Message.PartialAnswer(token)) }
        send(Message.AnswerEnd)
    }
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

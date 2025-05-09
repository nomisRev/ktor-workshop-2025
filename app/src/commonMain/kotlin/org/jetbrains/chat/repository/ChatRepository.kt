package org.jetbrains.chat.repository

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.http.HttpMethod
import io.ktor.serialization.WebsocketDeserializeException
import io.ktor.websocket.Frame
import io.ktor.websocket.readReason
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface ChatRepository {
    fun connect(): Flow<WebSocketMessage>

    fun sendMessage(message: String)
}

class WebSocketChatRepository(
    private val client: HttpClient,
    private val baseUrl: String,
    private val scope: CoroutineScope,
) : ChatRepository {
    private var session: DefaultClientWebSocketSession? = null

    override fun connect(): Flow<WebSocketMessage> = flow {
        val s = client.webSocketSession(method = HttpMethod.Get, host = baseUrl, path = "/ws")
        session = s
        try {
            while (true) {
                emit(s.receiveDeserialized<WebSocketMessage>())
            }
        } catch (cause: WebsocketDeserializeException) {
            when (val frame = cause.frame) {
                is Frame.Close -> emit(
                    WebSocketMessage.Error(
                        frame.readReason()?.message ?: "Connection closed. Please try connecting again."
                    )
                )

                else -> throw cause
            }
        }
    }

    override fun sendMessage(message: String) {
        scope.launch {
            requireNotNull(session?.send(Frame.Text(message))) { "Session is not connected" }
        }
    }
}

@Serializable
sealed interface WebSocketMessage {
    @Serializable
    @SerialName("partial_answer")
    data class PartialAnswer(val token: String) : WebSocketMessage

    @Serializable
    @SerialName("answer_end")
    data object AnswerEnd : WebSocketMessage

    @Serializable
    @SerialName("error")
    data class Error(val text: String) : WebSocketMessage
}

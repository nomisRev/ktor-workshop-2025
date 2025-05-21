package org.jetbrains.chat.repository

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
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
) : ChatRepository, AutoCloseable {
    private val messages = Channel<String>()
    override fun connect(): Flow<WebSocketMessage> = flow {
        client.webSocket(host = baseUrl, path = "/ws") {
            launch { messages.consumeAsFlow().collect { send(Frame.Text(it)) } }
            while (true) {
                emit(receiveDeserialized<WebSocketMessage>())
            }
        }
    }

    override fun sendMessage(message: String) {
        scope.launch { messages.send(message) }
    }

    override fun close() {
        messages.close()
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

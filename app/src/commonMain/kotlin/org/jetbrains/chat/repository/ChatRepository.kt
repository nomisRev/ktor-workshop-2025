package org.jetbrains.chat.repository

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.plugins.websocket.webSocketSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
    override fun connect(): Flow<WebSocketMessage> = flow {
        /**
         * TODO connect to the server using webSockets and emit incoming messages as they arrive
         *  Hint: There are two kind-of approaches using [webSocketSession] or [webSocket].
         */
    }

    override fun sendMessage(message: String) {
        /**
         * TODO implement sending a message to the server using webSockets
         *   Hint: you need to use the session created in connect.
         */
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

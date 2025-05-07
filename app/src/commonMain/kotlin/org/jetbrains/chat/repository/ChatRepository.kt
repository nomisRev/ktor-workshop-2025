package org.jetbrains.chat.repository

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.converter
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.http.HttpMethod
import io.ktor.serialization.WebsocketConverterNotFoundException
import io.ktor.serialization.deserialize
import io.ktor.serialization.suitableCharset
import io.ktor.util.reflect.typeInfo
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
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
            while(true) {
                emit(s.receiveDeserialized<WebSocketMessage>())
            }
        } catch (e: ClosedReceiveChannelException) {
            emit(WebSocketMessage.Error("Connection closed"))
            e.printStackTrace()
        }
    }

    override fun sendMessage(message: String) {
        scope.launch {
            requireNotNull(session?.send(Frame.Text(message))) { "Session is not connected" }
        }
    }
}

private data object SocketClosed : RuntimeException("Socket closed")


inline fun <reified A> DefaultClientWebSocketSession.consumeDeserialized(): Flow<A> {
    val converter = converter ?: throw WebsocketConverterNotFoundException("No converter was found for websocket")
    val charSet = call.request.headers.suitableCharset()
    return incoming
        .consumeAsFlow()
        .mapNotNull {
            when (it) {
                is Frame.Text -> converter.deserialize<A>(it, charSet)
                is Frame.Binary -> converter.deserialize<A>(it, charSet)
                is Frame.Close -> throw SocketClosed
                is Frame.Ping,
                is Frame.Pong -> throw RuntimeException("We do not receive Ping/Pong frames from incoming")
            }
        }.catch { if (it is SocketClosed) Unit else throw it }
}

@Serializable
sealed interface WebSocketMessage {
    @Serializable
    data class PartialAnswer(val token: String) : WebSocketMessage

    @Serializable
    data object AnswerEnd : WebSocketMessage

    @Serializable
    data class Error(val text: String) : WebSocketMessage
}

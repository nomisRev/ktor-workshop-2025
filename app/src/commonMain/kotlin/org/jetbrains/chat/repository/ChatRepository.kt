package org.jetbrains.chat.repository

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface ChatRepository {
    fun connect(): Flow<String>

    fun sendMessage(message: String)
}

class WebSocketChatRepository(
    private val client: HttpClient,
    private val baseUrl: String,
    private val scope: CoroutineScope,
) : ChatRepository {
    private var session: DefaultClientWebSocketSession? = null

    override fun connect(): Flow<String> = flow {
        emit("Hello, I am a stub AI chat! I cannot really reply anything useful. I will ping you every second.")
        while (true) {
            delay(1000)
            emit("Ping")
        }
    }

    override fun sendMessage(message: String) {
        // TODO implement later using websockets
    }
}

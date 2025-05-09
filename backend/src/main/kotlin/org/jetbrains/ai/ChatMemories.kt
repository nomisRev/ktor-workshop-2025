package org.jetbrains.ai

import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.ChatMessageDeserializer
import dev.langchain4j.data.message.ChatMessageSerializer
import dev.langchain4j.store.memory.chat.ChatMemoryStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.upsert

class ExposedChatMemoryStore(private val database: R2dbcDatabase) : ChatMemoryStore {

    private object ChatMemories : LongIdTable("chat_memories", "memory_id") {
        val memoryKey = varchar("memory_key", 36).uniqueIndex()
        val messages = text("messages")
    }

    override fun getMessages(memoryId: Any): List<ChatMessage> = runBlocking {
        val key = memoryId.toString()
        val json =
            suspendTransaction(db = database) {
                ChatMemories.selectAll()
                    .where { ChatMemories.memoryKey eq key }
                    .map { it[ChatMemories.messages] }
                    .singleOrNull()
            }
        ChatMessageDeserializer.messagesFromJson(json)
    }

    override fun updateMessages(memoryId: Any?, messages: List<ChatMessage>): Unit = runBlocking {
        val key = memoryId.toString()
        val json = ChatMessageSerializer.messagesToJson(messages)
        suspendTransaction(db = database) {
            ChatMemories.upsert(ChatMemories.memoryKey) {
                it[memoryKey] = key
                it[ChatMemories.messages] = json
            }
        }
    }

    override fun deleteMessages(memoryId: Any?): Unit = runBlocking {
        val key = memoryId.toString()
        suspendTransaction(db = database) { ChatMemories.deleteWhere { memoryKey eq key } }
    }
}

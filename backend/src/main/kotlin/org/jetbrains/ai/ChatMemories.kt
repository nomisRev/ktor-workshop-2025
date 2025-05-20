package org.jetbrains.ai

import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.ChatMessageDeserializer
import dev.langchain4j.data.message.ChatMessageSerializer
import dev.langchain4j.store.memory.chat.ChatMemoryStore
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert

class ExposedChatMemoryStore(private val database: Database) : ChatMemoryStore {

    private object ChatMemories : LongIdTable("chat_memories", "memory_id") {
        val memoryKey = varchar("memory_key", 36).uniqueIndex()
        val messages = text("messages")
    }

    override fun getMessages(memoryId: Any): List<ChatMessage> {
        val key = memoryId.toString()
        val json =
            transaction(db = database) {
                ChatMemories.selectAll()
                    .where { ChatMemories.memoryKey eq key }
                    .map { it[ChatMemories.messages] }
                    .singleOrNull()
            }
        return ChatMessageDeserializer.messagesFromJson(json)
    }

    override fun updateMessages(memoryId: Any?, messages: List<ChatMessage>): Unit {
        val key = memoryId.toString()
        val json = ChatMessageSerializer.messagesToJson(messages)
        transaction(db = database) {
            ChatMemories.upsert(ChatMemories.memoryKey) {
                it[memoryKey] = key
                it[ChatMemories.messages] = json
            }
        }
    }

    override fun deleteMessages(memoryId: Any?): Unit {
        val key = memoryId.toString()
        transaction(db = database) { ChatMemories.deleteWhere { memoryKey eq key } }
    }
}

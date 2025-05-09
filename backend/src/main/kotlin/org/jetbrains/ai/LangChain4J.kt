package org.jetbrains.ai

import dev.langchain4j.memory.chat.ChatMemoryProvider
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever
import dev.langchain4j.service.AiServices
import kotlin.reflect.KClass

class LangChainFactory(
    private val model: StreamingChatModel,
    private val memory: ChatMemoryProvider,
    private val retriever: EmbeddingStoreContentRetriever,
) {
    fun <A : Any> service(kClass: KClass<A>): A =
        AiServices.builder<A>(kClass.java)
            .streamingChatModel(model)
            .chatMemoryProvider(memory)
            .contentRetriever(retriever)
            .build()

    inline fun <reified A : Any> service(): A = service(A::class)
}

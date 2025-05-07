package org.jetbrains.plugins

import dev.langchain4j.data.document.splitter.DocumentSplitters
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.memory.chat.ChatMemoryProvider
import dev.langchain4j.memory.chat.MessageWindowChatMemory
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel
import dev.langchain4j.model.openai.OpenAiStreamingChatModel
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever
import dev.langchain4j.service.AiServices
import dev.langchain4j.store.embedding.EmbeddingStore
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.di.invoke
import io.ktor.server.plugins.di.provide
import io.ktor.server.plugins.di.resolve
import kotlin.reflect.KClass
import kotlinx.serialization.Serializable
import org.jetbrains.ai.ExposedChatMemoryStore
import org.jetbrains.ai.LangChainFactory
import org.jetbrains.ai.TravelService
import kotlin.jvm.java

@Serializable
data class AIConfig(
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    val tokenizer: String,
    val maxSegmentSizeInTokens: Int,
    val maxOverlapSizeInTokens: Int,
)

fun Application.aiModule(config: AIConfig) {
    dependencies {
        provide { ExposedChatMemoryStore(resolve()) }
        provide<StreamingChatLanguageModel> {
            OpenAiStreamingChatModel.builder()
                .apiKey(config.apiKey)
                .modelName(config.model)
                .build()
        }
        provide<EmbeddingStore<TextSegment>> { InMemoryEmbeddingStore() }
        provide<EmbeddingModel> { AllMiniLmL6V2QuantizedEmbeddingModel() }
        provide { DocumentSplitters.recursive(config.maxSegmentSizeInTokens, config.maxOverlapSizeInTokens) }
        provide {
            EmbeddingStoreContentRetriever.builder()
                .embeddingStore(InMemoryEmbeddingStore())
                .embeddingModel(resolve())
                .maxResults(5)
                .minScore(0.5)
                .build()
        }
        provide {
            ChatMemoryProvider { memoryId: Any? ->
                MessageWindowChatMemory.builder()
                    .id(memoryId)
                    .maxMessages(10)
                    .chatMemoryStore(resolve())
                    .build()
            }
        }
        provide {
            EmbeddingStoreIngestor.builder()
                .embeddingStore(resolve())
                .embeddingModel(resolve())
                .documentSplitter(resolve())
                .build()
        }
        provide { LangChainFactory(resolve(), resolve(), resolve()) }
        provide { TravelService(resolve()) }
    }
}


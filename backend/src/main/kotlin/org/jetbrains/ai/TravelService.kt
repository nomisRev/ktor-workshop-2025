package org.jetbrains.ai

import dev.langchain4j.service.MemoryId
import dev.langchain4j.service.SystemMessage
import dev.langchain4j.service.UserMessage
import dev.langchain4j.service.V
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.singleOrNull
import java.net.ConnectException

private const val SYSTEM_MESSAGE: String =
    """You are an AI assistant for a travel agency. Your role is to provide helpful, accurate, 
and personalized travel information to customers. You should:

1. Provide detailed information about destinations, accommodations, transportation options, 
   and activities based on the customer's interests and preferences.
2. Consider factors like budget, travel dates, group size, and special requirements when 
   making recommendations.
3. Offer practical travel tips and advice relevant to the destinations being discussed.
4. Be knowledgeable about travel regulations, visa requirements, and safety considerations.
5. Maintain a friendly, professional tone that inspires confidence in your recommendations.
"""

interface TravelService {
    @SystemMessage(SYSTEM_MESSAGE)
    @UserMessage("{{question}}")
    fun answer(@MemoryId userId: String, @V("question") question: String): Flow<String>
}


class FallbackTravelService(private val original: TravelService) : TravelService {
    override fun answer(userId: String, question: String): Flow<String> =
        original.answer(userId, question)
            .catch {
                emit("Sorry, I tried connecting to Ollama but it was not found.")
                emit("I am a simple mock AI service.")
                emit("I will always respond these same messages but at least it's working?")
            }
}
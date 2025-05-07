package org.jetbrains.ai

import dev.langchain4j.model.chat.StreamingChatLanguageModelReply
import dev.langchain4j.service.MemoryId
import dev.langchain4j.service.SystemMessage
import dev.langchain4j.service.UserMessage
import dev.langchain4j.service.V
import kotlinx.coroutines.flow.Flow

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

class TravelService(factory: LangChainFactory) {
    private val chat: Chat = factory.service<Chat>()

    fun answer(userId: String, question: String): Flow<String> =
        chat.answer(userId, question)

    private interface Chat {
        @SystemMessage(SYSTEM_MESSAGE)
        @UserMessage("{{question}}")
        fun answer(@MemoryId userId: String, @V("question") question: String): Flow<String>

        @SystemMessage(SYSTEM_MESSAGE)
        @UserMessage("{{question}}")
        fun example(
            @MemoryId userId: Long,
            @V("question") question: String,
        ): Flow<StreamingChatLanguageModelReply>
    }
}

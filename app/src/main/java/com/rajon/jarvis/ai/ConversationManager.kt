package com.rajon.jarvis.ai

import com.rajon.jarvis.data.model.ChatMessage
import com.rajon.jarvis.data.model.MessageRole
import com.rajon.jarvis.repository.ChatRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationManager @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend fun getRecentContext(): List<ChatMessage> {
        return chatRepository.getRecentMessages(10).reversed()
    }

    suspend fun recordUserMessage(content: String): ChatMessage {
        val msg = ChatMessage(content = content, role = MessageRole.USER)
        chatRepository.insertMessage(msg)
        return msg
    }

    suspend fun recordAssistantMessage(content: String): ChatMessage {
        val msg = ChatMessage(content = content, role = MessageRole.ASSISTANT)
        chatRepository.insertMessage(msg)
        return msg
    }

    suspend fun clearHistory() {
        chatRepository.clearHistory()
    }
}

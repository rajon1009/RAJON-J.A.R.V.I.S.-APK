package com.rajon.jarvis.repository

import com.rajon.jarvis.data.local.ChatMessageDao
import com.rajon.jarvis.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val chatMessageDao: ChatMessageDao
) {
    suspend fun insertMessage(message: ChatMessage) {
        chatMessageDao.insert(message)
    }

    fun getConversation(conversationId: String = "default"): Flow<List<ChatMessage>> {
        return chatMessageDao.getByConversation(conversationId)
    }

    suspend fun getRecentMessages(limit: Int = 20): List<ChatMessage> {
        return chatMessageDao.getRecent(limit)
    }

    suspend fun clearHistory() {
        chatMessageDao.deleteAll()
    }

    suspend fun clearConversation(conversationId: String) {
        chatMessageDao.deleteConversation(conversationId)
    }
}

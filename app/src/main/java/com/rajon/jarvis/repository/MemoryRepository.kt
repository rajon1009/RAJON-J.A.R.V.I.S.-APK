package com.rajon.jarvis.repository

import com.rajon.jarvis.data.local.MemoryDao
import com.rajon.jarvis.data.model.Memory
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryRepository @Inject constructor(
    private val memoryDao: MemoryDao
) {
    suspend fun saveMemory(key: String, value: String, category: String = "general") {
        val existing = memoryDao.getByKey(key)
        val memory = existing?.copy(
            value = value,
            updatedAt = System.currentTimeMillis()
        ) ?: Memory(
            key = key,
            value = value,
            category = category
        )
        memoryDao.insert(memory)
    }

    suspend fun recall(key: String): String? {
        return memoryDao.getByKey(key)?.value
    }

    fun getByCategory(category: String): Flow<List<Memory>> {
        return memoryDao.getByCategory(category)
    }

    fun getAll(): Flow<List<Memory>> {
        return memoryDao.getAll()
    }

    suspend fun getAllOnce(): List<Memory> {
        return memoryDao.getAllOnce()
    }

    suspend fun forget(memory: Memory) {
        memoryDao.delete(memory)
    }

    suspend fun clearAll() {
        memoryDao.deleteAll()
    }
}

package com.rajon.jarvis.repository

import com.rajon.jarvis.data.local.ReminderDao
import com.rajon.jarvis.data.model.Reminder
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository @Inject constructor(
    private val reminderDao: ReminderDao
) {
    suspend fun insert(reminder: Reminder) {
        reminderDao.insert(reminder)
    }

    fun getAll(): Flow<List<Reminder>> {
        return reminderDao.getAll()
    }

    fun getUpcoming(): Flow<List<Reminder>> {
        return reminderDao.getUpcoming()
    }

    suspend fun markCompleted(id: String) {
        reminderDao.markCompleted(id)
    }

    suspend fun delete(reminder: Reminder) {
        reminderDao.delete(reminder)
    }

    suspend fun clearAll() {
        reminderDao.deleteAll()
    }
}

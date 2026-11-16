package com.rajon.jarvis.device

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.rajon.jarvis.data.model.Reminder
import com.rajon.jarvis.receiver.ReminderReceiver
import com.rajon.jarvis.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderManager @Inject constructor(
    private val app: Application,
    private val reminderRepository: ReminderRepository
) {
    companion object {
        private const val TAG = "JARVIS_Reminder"
    }

    private val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun scheduleReminder(title: String, description: String = "", triggerTime: Long): Reminder {
        val reminder = Reminder(
            title = title,
            description = description,
            triggerTime = triggerTime
        )
        reminderRepository.insert(reminder)

        val intent = Intent(app, ReminderReceiver::class.java).apply {
            putExtra("reminder_id", reminder.id)
            putExtra("reminder_title", reminder.title)
            putExtra("reminder_description", reminder.description)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            app,
            reminder.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
            Log.d(TAG, "Scheduled reminder: $title at $triggerTime")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm", e)
        }

        return reminder
    }

    suspend fun cancelReminder(reminder: Reminder) {
        val intent = Intent(app, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            app,
            reminder.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        reminderRepository.delete(reminder)
    }

    fun getUpcomingReminders(): Flow<List<Reminder>> {
        return reminderRepository.getUpcoming()
    }
}

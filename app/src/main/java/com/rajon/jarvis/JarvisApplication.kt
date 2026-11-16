package com.rajon.jarvis

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class JarvisApplication : Application() {

    companion object {
        const val CHANNEL_SERVICE = "jarvis_service_channel"
        const val CHANNEL_REMINDERS = "jarvis_reminder_channel"
        const val CHANNEL_ALERTS = "jarvis_alert_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val serviceChannel = NotificationChannel(
            CHANNEL_SERVICE,
            "J.A.R.V.I.S. Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "J.A.R.V.I.S. background service notification"
            setShowBadge(false)
        }

        val reminderChannel = NotificationChannel(
            CHANNEL_REMINDERS,
            "Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders from J.A.R.V.I.S."
            enableVibration(true)
        }

        val alertChannel = NotificationChannel(
            CHANNEL_ALERTS,
            "Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "System alerts from J.A.R.V.I.S."
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
        manager.createNotificationChannel(reminderChannel)
        manager.createNotificationChannel(alertChannel)
    }
}

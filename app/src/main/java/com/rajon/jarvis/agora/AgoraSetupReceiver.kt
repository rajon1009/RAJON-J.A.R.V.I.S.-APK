package com.rajon.jarvis.agora

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import com.rajon.jarvis.data.local.PreferencesManager
import com.rajon.jarvis.data.local.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Lets a credential be written into the app's private DataStore without putting
 * it in source code or in the APK. Only this app's package can be relied on to
 * receive the broadcast; no value is logged or echoed back.
 */
class AgoraSetupReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val appId = intent.getStringExtra(EXTRA_APP_ID).orEmpty().trim()
        val certificate = intent.getStringExtra(EXTRA_CERTIFICATE).orEmpty().trim()
        if (appId.isBlank() || certificate.isBlank()) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val store = context.applicationContext.dataStore
                val existingJson = store.data.first()[PreferencesManager.KEY_AGORA_CONFIG].orEmpty()
                val existing = runCatching {
                    Gson().fromJson(existingJson, AgoraConfig::class.java)
                }.getOrNull() ?: AgoraConfig()

                val updated = existing.copy(appId = appId, appCertificate = certificate)
                store.edit { prefs ->
                    prefs[PreferencesManager.KEY_AGORA_CONFIG] = Gson().toJson(updated)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION = "com.rajon.jarvis.SET_AGORA_CREDENTIALS"
        const val EXTRA_APP_ID = "appId"
        const val EXTRA_CERTIFICATE = "appCertificate"
    }
}

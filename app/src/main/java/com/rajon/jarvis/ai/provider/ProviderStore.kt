package com.rajon.jarvis.ai.provider

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rajon.jarvis.data.local.PreferencesManager
import com.rajon.jarvis.data.local.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistent store for provider credentials and model choices.
 *
 * The Gemini key keeps living in [PreferencesManager] so the original AI Settings
 * screen keeps working; every other provider is stored here as JSON.
 */
@Singleton
class ProviderStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson,
    private val preferencesManager: PreferencesManager
) {

    companion object {
        val KEY_PROVIDERS = stringPreferencesKey("ai_provider_configs")
    }

    private val dataStore = context.dataStore

    val configs: Flow<List<ProviderConfig>> = combine(
        dataStore.data,
        preferencesManager.geminiApiKey
    ) { prefs, geminiKey ->
        merge(prefs[KEY_PROVIDERS], geminiKey)
    }

    fun merge(storedJson: String?, geminiKey: String): List<ProviderConfig> {
        val stored: List<ProviderConfig> = storedJson?.let { json ->
            runCatching {
                val type = TypeToken.getParameterized(List::class.java, ProviderConfig::class.java).type
                gson.fromJson<List<ProviderConfig>>(json, type)
            }.getOrNull()
        } ?: emptyList()

        val merged = ProviderPresets.all.map { preset ->
            val saved = stored.firstOrNull { it.id == preset.id }
            if (saved == null) {
                if (preset.id == "gemini") preset.copy(apiKey = geminiKey) else preset
            } else {
                preset.copy(
                    apiKey = if (preset.id == "gemini") geminiKey else saved.apiKey,
                    model = saved.model.ifBlank { preset.model },
                    enabled = saved.enabled,
                    priority = saved.priority,
                    notes = saved.notes.ifBlank { preset.notes }
                )
            }
        }

        val extras = stored.filter { saved -> ProviderPresets.all.none { it.id == saved.id } }
        return merged + extras
    }

    suspend fun currentConfigs(): List<ProviderConfig> = configs.first()

    /** Stores the credential/model for one provider. */
    suspend fun saveProvider(config: ProviderConfig) {
        val list = currentConfigs().toMutableList()
        val index = list.indexOfFirst { it.id == config.id }
        if (index >= 0) {
            list[index] = list[index].copy(
                apiKey = config.apiKey,
                model = config.model,
                enabled = config.enabled,
                priority = config.priority
            )
        } else {
            list.add(config)
        }
        persist(list)
    }

    suspend fun setEnabled(id: String, enabled: Boolean) {
        val list = currentConfigs().map { if (it.id == id) it.copy(enabled = enabled) else it }
        persist(list)
    }

    suspend fun setPriority(id: String, priority: Int) {
        val list = currentConfigs().map { if (it.id == id) it.copy(priority = priority) else it }
        persist(list)
    }

    private suspend fun persist(list: List<ProviderConfig>) {
        // The Gemini credential lives in PreferencesManager - never duplicate it here.
        val sanitized = list.map { if (it.id == "gemini") it.copy(apiKey = "") else it }
        val json = gson.toJson(sanitized)
        dataStore.edit { it[KEY_PROVIDERS] = json }
    }
}

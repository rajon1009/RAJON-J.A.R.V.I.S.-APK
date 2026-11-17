package com.rajon.jarvis.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "jarvis_preferences")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val KEY_LANGUAGE = stringPreferencesKey("language")
        val KEY_GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val KEY_OWNER_NAME = stringPreferencesKey("owner_name")
        val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
        val KEY_TTS_ENABLED = booleanPreferencesKey("tts_enabled")
        val KEY_VOICE_INPUT_ENABLED = booleanPreferencesKey("voice_input_enabled")
        val KEY_TTS_VOICE = stringPreferencesKey("tts_voice")
    }

    val language: Flow<String> = dataStore.data.map { it[KEY_LANGUAGE] ?: "bn" }
    val geminiApiKey: Flow<String> = dataStore.data.map { it[KEY_GEMINI_API_KEY] ?: "" }
    val ownerName: Flow<String> = dataStore.data.map { it[KEY_OWNER_NAME] ?: "Rajon" }
    val isDarkMode: Flow<Boolean> = dataStore.data.map { it[KEY_DARK_MODE] ?: true }
    val isTtsEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_TTS_ENABLED] ?: true }
    val isVoiceInputEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_VOICE_INPUT_ENABLED] ?: true }
    val ttsVoice: Flow<String> = dataStore.data.map { it[KEY_TTS_VOICE] ?: "" }

    suspend fun setLanguage(lang: String) {
        dataStore.edit { it[KEY_LANGUAGE] = lang }
    }

    suspend fun setGeminiApiKey(key: String) {
        dataStore.edit { it[KEY_GEMINI_API_KEY] = key }
    }

    suspend fun setOwnerName(name: String) {
        dataStore.edit { it[KEY_OWNER_NAME] = name }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { it[KEY_DARK_MODE] = enabled }
    }

    suspend fun setTtsEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_TTS_ENABLED] = enabled }
    }

    suspend fun setVoiceInputEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_VOICE_INPUT_ENABLED] = enabled }
    }

    suspend fun setTtsVoice(name: String) {
        dataStore.edit { it[KEY_TTS_VOICE] = name }
    }

    suspend fun getApiKeyOnce(): String {
        var key = ""
        dataStore.data.collect { prefs ->
            key = prefs[KEY_GEMINI_API_KEY] ?: ""
            return@collect
        }
        return key
    }
}

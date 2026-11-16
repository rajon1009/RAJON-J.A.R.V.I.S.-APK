package com.rajon.jarvis.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajon.jarvis.data.local.PreferencesManager
import com.rajon.jarvis.repository.ChatRepository
import com.rajon.jarvis.repository.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val apiKey: String = "",
    val ownerName: String = "Rajon",
    val language: String = "bn",
    val isDarkMode: Boolean = true,
    val isTtsEnabled: Boolean = true,
    val isVoiceInputEnabled: Boolean = true,
    val feedbackMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val chatRepository: ChatRepository,
    private val memoryRepository: MemoryRepository
) : ViewModel() {

    private val _feedbackMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesManager.geminiApiKey,
        preferencesManager.ownerName,
        preferencesManager.language,
        preferencesManager.isDarkMode,
        preferencesManager.isTtsEnabled,
        preferencesManager.isVoiceInputEnabled,
        _feedbackMessage
    ) { key, owner, lang, dark, tts, voice, feedback ->
        SettingsUiState(
            apiKey = key,
            ownerName = owner,
            language = lang,
            isDarkMode = dark,
            isTtsEnabled = tts,
            isVoiceInputEnabled = voice,
            feedbackMessage = feedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun saveApiKey(key: String) {
        viewModelScope.launch {
            preferencesManager.setGeminiApiKey(key.trim())
            _feedbackMessage.value = "Gemini API Key updated successfully"
        }
    }

    fun saveOwnerName(name: String) {
        viewModelScope.launch {
            preferencesManager.setOwnerName(name.trim())
            _feedbackMessage.value = "Owner name updated to ${name.trim()}"
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            preferencesManager.setLanguage(lang)
            _feedbackMessage.value = if (lang == "bn") "ভাষা বাংলা নির্ধারণ করা হয়েছে" else "Language set to English"
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setDarkMode(enabled)
        }
    }

    fun setTtsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setTtsEnabled(enabled)
        }
    }

    fun setVoiceInputEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setVoiceInputEnabled(enabled)
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            chatRepository.clearHistory()
            _feedbackMessage.value = "All chat history cleared"
        }
    }

    fun clearMemories() {
        viewModelScope.launch {
            memoryRepository.clearAll()
            _feedbackMessage.value = "All persistent memories cleared"
        }
    }

    fun dismissFeedback() {
        _feedbackMessage.value = null
    }
}

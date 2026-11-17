package com.rajon.jarvis.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajon.jarvis.data.local.PreferencesManager
import com.rajon.jarvis.repository.ChatRepository
import com.rajon.jarvis.repository.MemoryRepository
import com.rajon.jarvis.voice.SpeechManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
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
    val ttsVoice: String = "",
    val feedbackMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val chatRepository: ChatRepository,
    private val memoryRepository: MemoryRepository,
    private val speechManager: SpeechManager
) : ViewModel() {

    private val _feedbackMessage = MutableStateFlow<String?>(null)

    private val _voices = MutableStateFlow<List<SpeechManager.TtsVoiceInfo>>(emptyList())
    val voices: StateFlow<List<SpeechManager.TtsVoiceInfo>> = _voices.asStateFlow()

    private data class CoreState(
        val apiKey: String,
        val ownerName: String,
        val language: String,
        val isDarkMode: Boolean,
        val isTtsEnabled: Boolean,
        val ttsVoice: String
    )

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(
            preferencesManager.geminiApiKey,
            preferencesManager.ownerName,
            preferencesManager.language,
            preferencesManager.isDarkMode,
            preferencesManager.isTtsEnabled,
            preferencesManager.ttsVoice
        ) { key, owner, lang, dark, tts, voice ->
            CoreState(key, owner, lang, dark, tts, voice)
        },
        combine(preferencesManager.isVoiceInputEnabled, _feedbackMessage) { voice, feedback ->
            voice to feedback
        }
    ) { core, (voice, feedback) ->
        SettingsUiState(
            apiKey = core.apiKey,
            ownerName = core.ownerName,
            language = core.language,
            isDarkMode = core.isDarkMode,
            isTtsEnabled = core.isTtsEnabled,
            isVoiceInputEnabled = voice,
            ttsVoice = core.ttsVoice,
            feedbackMessage = feedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    init {
        viewModelScope.launch {
            speechManager.isTtsReady.filter { it }.first()
            val saved = preferencesManager.ttsVoice.first()
            if (saved.isNotBlank()) {
                speechManager.setPreferredVoice(saved)
            }
            _voices.value = speechManager.availableVoices()
        }
    }

    fun refreshVoices() {
        viewModelScope.launch {
            _voices.value = speechManager.availableVoices()
        }
    }

    fun selectVoice(voice: SpeechManager.TtsVoiceInfo) {
        viewModelScope.launch {
            preferencesManager.setTtsVoice(voice.name)
            speechManager.setPreferredVoice(voice.name)
            _feedbackMessage.value = "Voice selected: ${voice.locale} (${voice.name})"
        }
    }

    /** Selects the best installed voice, preferring bn-BD male. */
    fun selectBestVoice() {
        viewModelScope.launch {
            val language = preferencesManager.language.first()
            val best = speechManager.bestVoiceFor(language)
            if (best == null) {
                _feedbackMessage.value =
                    "No matching TTS voice is installed for $language. Install Bengali voice data in Google TTS settings."
                return@launch
            }
            preferencesManager.setTtsVoice(best.name)
            speechManager.setPreferredVoice(best.name)
            _feedbackMessage.value = "Best voice selected: ${best.locale} (${best.name})"
        }
    }

    fun previewVoice(voice: SpeechManager.TtsVoiceInfo) {
        viewModelScope.launch {
            val language = preferencesManager.language.first()
            val sample = if (language == "bn") {
                "স্যার, আমি জে এ আর ভি আই এস। আপনার সহায়ক হিসেবে প্রস্তুত।"
            } else {
                "Sir, this is J.A.R.V.I.S. Your assistant is ready."
            }
            val spoken = speechManager.speakWithVoice(sample, language, voice.name)
            if (!spoken) {
                _feedbackMessage.value = "Could not play the preview on this device."
            }
        }
    }

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

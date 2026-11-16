package com.rajon.jarvis.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajon.jarvis.ai.JarvisAI
import com.rajon.jarvis.data.local.PreferencesManager
import com.rajon.jarvis.data.model.ChatMessage
import com.rajon.jarvis.repository.ChatRepository
import com.rajon.jarvis.voice.SpeechManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val jarvisAi: JarvisAI,
    private val speechManager: SpeechManager,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val messages: StateFlow<List<ChatMessage>> = chatRepository
        .getConversation("default")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val isListening: StateFlow<Boolean> = speechManager.isListening

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun sendMessage(text: String = _inputText.value) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || _isThinking.value) return

        _inputText.value = ""
        _isThinking.value = true

        viewModelScope.launch {
            try {
                val result = jarvisAi.processUserRequest(trimmed)
                val ttsEnabled = preferencesManager.isTtsEnabled.first()
                if (ttsEnabled && result.replyText.isNotBlank()) {
                    val lang = preferencesManager.language.first()
                    val spoken = speechManager.speak(result.replyText, lang)
                    if (!spoken && !result.isError) {
                        _errorMessage.value = "Speech output is unavailable on this device. Text reply shown above."
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Unexpected error: ${e.message ?: "unknown"}"
            } finally {
                _isThinking.value = false
            }
        }
    }

    fun toggleVoiceInput() {
        if (speechManager.isListening.value) {
            speechManager.stopListening()
            return
        }

        viewModelScope.launch {
            val voiceInputEnabled = preferencesManager.isVoiceInputEnabled.first()
            if (!voiceInputEnabled) {
                _errorMessage.value = "Voice input is disabled. Enable it in General Settings."
                return@launch
            }

            speechManager.stopSpeaking()
            val lang = preferencesManager.language.first()
            val localeStr = if (lang == "bn") "bn-BD" else "en-US"
            speechManager.startListening(
                language = localeStr,
                onResult = { recognizedText ->
                    if (recognizedText.isNotBlank()) {
                        sendMessage(recognizedText)
                    }
                },
                onError = { message ->
                    _errorMessage.value = message
                }
            )
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun clearChat() {
        viewModelScope.launch {
            chatRepository.clearHistory()
        }
    }
}

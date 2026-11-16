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
                    speechManager.speak(result.replyText, lang)
                }
            } catch (_: Exception) {
                // Handled gracefully inside JarvisAI
            } finally {
                _isThinking.value = false
            }
        }
    }

    fun toggleVoiceInput() {
        if (speechManager.isListening.value) {
            speechManager.stopListening()
        } else {
            viewModelScope.launch {
                val lang = preferencesManager.language.first()
                val localeStr = if (lang == "bn") "bn-BD" else "en-US"
                speechManager.startListening(
                    language = localeStr,
                    onResult = { recognizedText ->
                        if (recognizedText.isNotBlank()) {
                            sendMessage(recognizedText)
                        }
                    },
                    onError = {
                        // Voice error handled silently or logged
                    }
                )
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            chatRepository.clearHistory()
        }
    }
}

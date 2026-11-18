package com.rajon.jarvis.ui.screens.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajon.jarvis.agora.AgoraVoiceAgent
import com.rajon.jarvis.ai.JarvisAI
import com.rajon.jarvis.data.local.PreferencesManager
import com.rajon.jarvis.voice.SpeechManager
import com.rajon.jarvis.voice.SpeechPolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoiceAssistantViewModel @Inject constructor(
    private val jarvisAi: JarvisAI,
    private val speechManager: SpeechManager,
    private val preferencesManager: PreferencesManager,
    val agoraVoiceAgent: AgoraVoiceAgent
) : ViewModel() {

    val agoraState = agoraVoiceAgent.state
    val agoraTranscripts = agoraVoiceAgent.transcripts

    fun toggleAgoraVoice() {
        if (agoraState.value !is com.rajon.jarvis.agora.AgoraSessionState.Idle &&
            agoraState.value !is com.rajon.jarvis.agora.AgoraSessionState.Error
        ) {
            agoraVoiceAgent.stop()
            _error.value = "Agora voice session ended."
        } else {
            speechManager.stopListening()
            speechManager.stopSpeaking()
            viewModelScope.launch {
                _error.value = agoraVoiceAgent.start()
            }
        }
    }

    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript.asStateFlow()

    private val _reply = MutableStateFlow("")
    val reply: StateFlow<String> = _reply.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    val isListening: StateFlow<Boolean> = speechManager.isListening
    val isSpeaking: StateFlow<Boolean> = speechManager.isSpeaking

    fun startListening() {
        if (speechManager.isListening.value || _isProcessing.value) return

        _error.value = null
        _transcript.value = ""

        viewModelScope.launch {
            if (!preferencesManager.isVoiceInputEnabled.first()) {
                _error.value = "ভয়েস ইনপুট বন্ধ আছে। General Settings থেকে এটি চালু করুন।"
                return@launch
            }
            if (!speechManager.isRecognitionAvailable()) {
                _error.value = "এই ডিভাইসে speech recognition পাওয়া যায়নি।"
                return@launch
            }

            val language = preferencesManager.language.first()
            val locale = SpeechPolicy.sttPrimaryLocale(language)
            val fallbackLocale = SpeechPolicy.sttFallbackLocale(language)
            speechManager.stopSpeaking()

            speechManager.startListening(
                language = locale,
                fallbackLanguage = fallbackLocale,
                onPartialResult = { partial ->
                    _transcript.value = partial
                },
                onResult = { text ->
                    _transcript.value = text
                    if (text.isNotBlank()) {
                        submit(text)
                    } else {
                        _error.value = "কথা বোঝা যায়নি। আবার বলুন।"
                    }
                },
                onError = { message ->
                    _error.value = message
                }
            )
        }
    }

    private fun submit(text: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val result = jarvisAi.processUserRequest(text, fromVoice = true)
                _reply.value = result.replyText

                val ttsEnabled = preferencesManager.isTtsEnabled.first()
                if (ttsEnabled && result.replyText.isNotBlank()) {
                    val language = preferencesManager.language.first()
                    val savedVoice = preferencesManager.ttsVoice.first()
                    if (savedVoice.isNotBlank()) {
                        speechManager.setPreferredVoice(savedVoice)
                    }
                    val spoken = speechManager.speak(result.replyText, language)
                    if (!spoken) {
                        _error.value = "এই ডিভাইসে speech output পাওয়া যায়নি। উপরের উত্তরটি পড়ুন।"
                    }
                }
            } catch (e: Exception) {
                _error.value = "অপ্রত্যাশিত সমস্যা হয়েছে: ${e.message ?: "অজানা"}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun toggleListening() {
        if (speechManager.isListening.value) {
            speechManager.stopListening()
        } else {
            startListening()
        }
    }

    fun stopListening() {
        speechManager.stopListening()
    }

    fun stopSpeaking() {
        speechManager.stopSpeaking()
    }

    fun dismissError() {
        _error.value = null
    }
}

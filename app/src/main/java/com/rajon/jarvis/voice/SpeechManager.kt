package com.rajon.jarvis.voice

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeechManager @Inject constructor(
    private val app: Application
) {
    companion object {
        private const val TAG = "JARVIS_Speech"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady: StateFlow<Boolean> = _isTtsReady.asStateFlow()

    private val _preferredVoiceName = MutableStateFlow<String?>(null)
    val preferredVoiceName: StateFlow<String?> = _preferredVoiceName.asStateFlow()

    /** One installed TTS voice, normalised for the UI. */
    data class TtsVoiceInfo(
        val name: String,
        val locale: String,
        val quality: String,
        val networkRequired: Boolean,
        val isMale: Boolean
    )

    init {
        initTts()
    }

    fun isRecognitionAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(app)

    /** Every voice the installed TTS engine exposes. */
    fun availableVoices(): List<TtsVoiceInfo> {
        val tts = textToSpeech ?: return emptyList()
        if (!isTtsInitialized) return emptyList()
        return runCatching {
            tts.voices.orEmpty().map { voice ->
                val lower = voice.name.lowercase()
                TtsVoiceInfo(
                    name = voice.name,
                    locale = voice.locale.toLanguageTag(),
                    quality = when (voice.quality) {
                        TextToSpeech.Quality.VERY_HIGH -> "very high"
                        TextToSpeech.Quality.HIGH -> "high"
                        TextToSpeech.Quality.NORMAL -> "normal"
                        TextToSpeech.Quality.LOW -> "low"
                        else -> "unknown"
                    },
                    networkRequired = voice.isNetworkConnectionRequired,
                    isMale = lower.contains("male") && !lower.contains("female")
                )
            }.sortedWith(compareBy({ it.locale }, { it.name }))
        }.getOrDefault(emptyList())
    }

    /**
     * Picks the best installed voice for a language.
     * Bengali preference order: bn-BD male > bn-BD > bn-IN > any Bengali > English.
     */
    fun bestVoiceFor(language: String): TtsVoiceInfo? {
        val voices = availableVoices()
        if (voices.isEmpty()) return null

        _preferredVoiceName.value?.let { preferred ->
            voices.firstOrNull { it.name == preferred }?.let { return it }
        }

        val candidates: List<TtsVoiceInfo> = if (language == "bn") {
            voices.filter { it.locale.startsWith("bn-BD") } +
                voices.filter { it.locale.startsWith("bn-IN") } +
                voices.filter { it.locale.startsWith("bn") }
        } else {
            voices.filter { it.locale.startsWith("en-US") } +
                voices.filter { it.locale.startsWith("en") }
        }

        return candidates.firstOrNull { it.isMale } ?: candidates.firstOrNull()
    }

    fun setPreferredVoice(name: String?) {
        _preferredVoiceName.value = name
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(app) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true
                _isTtsReady.value = true

                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        _isSpeaking.value = false
                        Log.e(TAG, "TTS utterance error: $errorCode")
                    }
                })

                // Establish a working default until speak() selects the requested language.
                applyTtsLanguage(Locale.US)
            } else {
                isTtsInitialized = false
                _isTtsReady.value = false
                Log.e(TAG, "TTS initialization failed: $status")
            }
        }
    }

    fun startListening(
        language: String = "bn-BD",
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(app)) {
            onError("Speech recognition is not available on this device. Install or enable a speech recognition service.")
            return
        }

        stopListening()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(app).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _isListening.value = false
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    val errorMsg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Please try again."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Please try again."
                        SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition."
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition timed out. Check your connection."
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Check the microphone."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required for voice commands."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy. Wait a moment and try again."
                        SpeechRecognizer.ERROR_SERVER -> "Speech recognition server error. Try again."
                        SpeechRecognizer.ERROR_CLIENT -> "Speech recognition was interrupted."
                        else -> "Speech recognition error ($error)."
                    }
                    onError(errorMsg)
                    releaseRecognizer()
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        onResult(matches[0])
                    } else {
                        onError("Could not understand the audio. Please try again.")
                    }
                    releaseRecognizer()
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, language)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        val recognizer = speechRecognizer
        speechRecognizer = null
        try {
            recognizer?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Unable to cancel recognizer: ${e.message}")
        }
        try {
            recognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Unable to destroy recognizer: ${e.message}")
        }
        _isListening.value = false
    }

    private fun releaseRecognizer() {
        val recognizer = speechRecognizer
        speechRecognizer = null
        try {
            recognizer?.destroy()
        } catch (_: Exception) {
            // Already released.
        }
    }

    /**
     * Speaks [text]. Returns false when TTS is unavailable so callers can surface feedback.
     */
    fun speak(text: String, language: String = "bn"): Boolean {
        val tts = textToSpeech
        if (!isTtsInitialized || tts == null) {
            Log.e(TAG, "TTS not ready; cannot speak response")
            return false
        }
        if (text.isBlank()) return false

        applyLanguageFor(language)
        applyBestVoice(language)

        _isSpeaking.value = true
        val result = tts.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "JARVIS_${System.currentTimeMillis()}"
        )
        if (result != TextToSpeech.SUCCESS) {
            _isSpeaking.value = false
        }
        return result == TextToSpeech.SUCCESS
    }

    private fun applyLanguageFor(language: String) {
        if (language == "bn") {
            val candidates = listOf(
                Locale("bn", "BD"),
                Locale("bn", "IN"),
                Locale("bn")
            )
            for (locale in candidates) {
                if (applyTtsLanguage(locale)) return
            }
            // Bengali voice is unavailable on this device; fall back to English.
            applyTtsLanguage(Locale.US)
        } else {
            applyTtsLanguage(Locale.US)
        }
    }

    /** Applies the best installed voice (bn-BD male first) for the language. */
    private fun applyBestVoice(language: String) {
        val tts = textToSpeech ?: return
        val info = bestVoiceFor(language) ?: return
        val voice = runCatching { tts.voices?.firstOrNull { it.name == info.name } }.getOrNull()
            ?: return
        runCatching { tts.voice = voice }
            .onFailure { Log.w(TAG, "Unable to select voice ${info.name}: ${it.message}") }
    }

    /** Speaks with a specific voice, useful for the voice picker preview. */
    fun speakWithVoice(text: String, language: String, voiceName: String): Boolean {
        setPreferredVoice(voiceName)
        return speak(text, language)
    }

    private fun applyTtsLanguage(locale: Locale): Boolean {
        val tts = textToSpeech ?: return false
        return try {
            when (tts.setLanguage(locale)) {
                TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED -> false
                else -> true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to set TTS language ${locale}: ${e.message}")
            false
        }
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        stopListening()
        stopSpeaking()
        textToSpeech?.shutdown()
        textToSpeech = null
        isTtsInitialized = false
        _isTtsReady.value = false
    }
}

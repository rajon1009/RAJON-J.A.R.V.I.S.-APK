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
                    quality = voice.quality.toString(),
                    networkRequired = voice.isNetworkConnectionRequired,
                    isMale = lower.contains("male") && !lower.contains("female")
                )
            }.sortedWith(compareBy({ it.locale }, { it.name }))
        }.getOrDefault(emptyList())
    }

    /**
     * Picks the best installed voice for a language.
     * Bengali output uses Bangladesh Bengali only: bn-BD male > bn-BD.
     */
    fun bestVoiceFor(language: String): TtsVoiceInfo? {
        val voices = availableVoices()
        if (voices.isEmpty()) return null

        _preferredVoiceName.value?.let { preferred ->
            voices.firstOrNull { voice ->
                voice.name == preferred &&
                    (language != SpeechPolicy.BN || SpeechPolicy.isBangladeshBengaliLocale(voice.locale))
            }?.let { return it }
        }

        val candidates: List<TtsVoiceInfo> = if (language == SpeechPolicy.BN) {
            voices.filter { SpeechPolicy.isBangladeshBengaliLocale(it.locale) }
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
        fallbackLanguage: String? = null,
        onPartialResult: ((String) -> Unit)? = null,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(app)) {
            onError("এই ডিভাইসে speech recognition পাওয়া যায়নি। একটি speech recognition service চালু করুন।")
            return
        }

        stopListening()
        startRecognitionPass(language, fallbackLanguage, allowFallback = false, onPartialResult, onResult, onError)
    }

    /**
     * Runs one recognition pass with real-time partial results and fast silence detection.
     */
    private fun startRecognitionPass(
        language: String,
        fallbackLanguage: String?,
        allowFallback: Boolean,
        onPartialResult: ((String) -> Unit)?,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {

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
                    releaseRecognizer()
                    if (allowFallback && fallbackLanguage != null &&
                        (error == SpeechRecognizer.ERROR_NO_MATCH ||
                            error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
                    ) {
                        startRecognitionPass(fallbackLanguage, null, false, onPartialResult, onResult, onError)
                        return
                    }
                    val errorMsg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "কথা বোঝা যায়নি। আবার বলুন।"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "কোনো কথা শোনা যায়নি। আবার বলুন।"
                        SpeechRecognizer.ERROR_NETWORK -> "কথা শনাক্ত করার সময় নেটওয়ার্ক সমস্যা হয়েছে।"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "কথা শনাক্ত করার সময় শেষ হয়েছে। সংযোগ পরীক্ষা করুন।"
                        SpeechRecognizer.ERROR_AUDIO -> "অডিও রেকর্ডিংয়ে সমস্যা হয়েছে। মাইক্রোফোন পরীক্ষা করুন।"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "ভয়েস কমান্ডের জন্য মাইক্রোফোনের অনুমতি প্রয়োজন।"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer ব্যস্ত আছে। একটু পরে চেষ্টা করুন।"
                        SpeechRecognizer.ERROR_SERVER -> "Speech recognition server-এ সমস্যা হয়েছে। আবার চেষ্টা করুন।"
                        SpeechRecognizer.ERROR_CLIENT -> "কথা শনাক্তকরণ বন্ধ হয়ে গেছে।"
                        else -> "কথা শনাক্তকরণে সমস্যা হয়েছে ($error)।"
                    }
                    onError(errorMsg)
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    releaseRecognizer()
                    if (!matches.isNullOrEmpty() && matches[0].isNotBlank()) {
                        onResult(matches[0])
                    } else if (allowFallback && fallbackLanguage != null) {
                        startRecognitionPass(fallbackLanguage, null, false, onPartialResult, onResult, onError)
                    } else {
                        onError("কথা বোঝা যায়নি। আবার বলুন।")
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()?.trim()
                    if (!text.isNullOrBlank()) {
                        onPartialResult?.invoke(text)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, language)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 600L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 600L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 700L)
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

        val cleanSpeechText = text
            .replace(Regex("""[*#_`~]"""), "")
            .replace(Regex("""\{.*?\}"""), "")
            .replace(Regex("""\[(.*?)\]\(.*?\)"""), "$1")
            .replace(Regex("""\s+"""), " ")
            .trim()
        if (cleanSpeechText.isBlank()) return false

        val replyLanguage = SpeechPolicy.replyLanguage(language, SpeechPolicy.Detected.NONE)
        if (!applyLanguageFor(replyLanguage)) {
            Log.w(TAG, "No supported TTS locale for $replyLanguage")
            return false
        }
        applyBestVoice(replyLanguage)

        // J.A.R.V.I.S. signature calm, sophisticated resonance and pacing
        tts.setPitch(0.94f)
        tts.setSpeechRate(0.98f)

        _isSpeaking.value = true
        val result = tts.speak(
            cleanSpeechText,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "JARVIS_${System.currentTimeMillis()}"
        )
        if (result != TextToSpeech.SUCCESS) {
            _isSpeaking.value = false
        }
        return result == TextToSpeech.SUCCESS
    }

    private fun applyLanguageFor(language: String): Boolean {
        return if (language == SpeechPolicy.BN) {
            // Do not make a Bangladesh Bengali reply sound Indian, Hindi or English.
            applyTtsLanguage(Locale("bn", "BD"))
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
        if (SpeechPolicy.replyLanguage(language, SpeechPolicy.Detected.NONE) == SpeechPolicy.BN) {
            val voice = availableVoices().firstOrNull { it.name == voiceName }
            if (voice == null || !SpeechPolicy.isBangladeshBengaliLocale(voice.locale)) {
                Log.w(TAG, "Refusing non-bn-BD voice preview for Bangladesh Bengali output")
                return false
            }
        }
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

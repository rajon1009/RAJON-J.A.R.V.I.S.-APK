package com.rajon.jarvis.voice

/**
 * Language policy for J.A.R.V.I.S. speech. Pure Kotlin (no Android dependency)
 * so every branch can be verified with JVM unit tests.
 *
 * Policy (owner directive):
 *  - Replies are in natural, standard Bangladeshi Bengali by default — even when
 *    the command arrives in English. English and mixed commands are understood,
 *    then answered in Bengali.
 *  - An explicit "en" choice in Settings keeps English replies (existing feature).
 *  - Unclear / mixed input falls back to Bengali.
 *  - Recognition listens for bn-BD first and retries with en-US when the result
 *    is missing or low-confidence, so both languages are understood.
 */
object SpeechPolicy {

    const val BN = "bn"
    const val EN = "en"
    const val MIXED = "mixed"

    enum class Detected { BN, EN, MIXED, NONE }

    /** One complete, natural Bangladeshi Bengali sentence used for the voice check. */
    const val BENGALI_VOICE_CHECK_SENTENCE =
        "আমি জার্ভিস, আপনার ব্যক্তিগত সহকারী। আপনি বাংলায় বা ইংরেজিতে কথা বলুন, আমি স্বাভাবিক বাংলায় উত্তর দেব, স্যার।"

    /** Appended to every locally-routed provider instruction, including Agora's LLM. */
    const val BANGLADESHI_BENGALI_RESPONSE_INSTRUCTION =
        "Reply only in natural, standard Bangladeshi Bengali (Bangla). Understand English and mixed Bangla-English commands, but keep the reply in Bangla."

    fun normalize(mode: String?): String = when (mode?.lowercase()) {
        EN -> EN
        MIXED -> MIXED
        else -> BN
    }

    private fun isBengaliChar(c: Char): Boolean = c.code in 0x0980..0x09FF

    fun containsBengali(text: String): Boolean = text.any(::isBengaliChar)

    /** True when the text contains Latin script (an English command or word). */
    fun containsLatin(text: String): Boolean =
        text.any { it.isLetter && !isBengaliChar(it) }

    /** Detects which script(s) the user actually used. */
    fun detect(text: String): Detected {
        val bn = containsBengali(text)
        val latin = containsLatin(text)
        return when {
            bn && latin -> Detected.MIXED
            bn -> Detected.BN
            latin -> Detected.EN
            else -> Detected.NONE
        }
    }

    /**
     * Language the assistant should reply in. Bengali is the default regardless
     * of the command language; only an explicit English setting switches it.
     */
    fun replyLanguage(setting: String?, detected: Detected): String {
        val mode = normalize(setting)
        return if (mode == EN) EN else BN
    }

    /** Primary STT locale for the chosen mode. */
    fun sttPrimaryLocale(setting: String?): String =
        if (normalize(setting) == EN) "en-US" else "bn-BD"

    /** Secondary STT locale used when the first pass fails — keeps both languages understood. */
    fun sttFallbackLocale(setting: String?): String =
        if (normalize(setting) == EN) "bn-BD" else "en-US"

    /** Locale passed to TextToSpeech for a reply language. */
    fun ttsLocale(replyLanguage: String?): String =
        if (normalize(replyLanguage) == EN) "en-US" else "bn-BD"

    /**
     * Keeps Bangladesh Bengali as the only Bengali speech output choice.  We do
     * not silently substitute an Indian Bengali or Hindi voice.
     */
    fun isBangladeshBengaliLocale(locale: String): Boolean = locale.equals("bn-BD", ignoreCase = true)

    /** Scores come from SpeechRecognizer.CONFIDENCE_SCORES; null/unavailable means trust the result. */
    fun confidenceTooLow(scores: FloatArray?, threshold: Float = 0.45f): Boolean {
        if (scores == null || scores.isEmpty()) return false
        return scores.none { it >= threshold }
    }

    /**
     * Sanity check for a Bengali reply: it must contain at least two Bengali
     * letters and not be a single repeated character. Grammar itself is enforced
     * by the model prompt; this filter only rejects empty/garbled output.
     */
    fun hasMeaningfulBengaliContent(text: String): Boolean {
        val letters = text.count(::isBengaliChar)
        if (letters < 2) return false
        val distinct = text.filter(::isBengaliChar).toSet().size
        return distinct >= 2
    }
}

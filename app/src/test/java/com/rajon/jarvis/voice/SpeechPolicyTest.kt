package com.rajon.jarvis.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechPolicyTest {

    @Test
    fun `Bengali is the safe default reply language`() {
        assertEquals(SpeechPolicy.BN, SpeechPolicy.replyLanguage(null, SpeechPolicy.Detected.EN))
        assertEquals(SpeechPolicy.BN, SpeechPolicy.replyLanguage("mixed", SpeechPolicy.Detected.MIXED))
    }

    @Test
    fun `explicit English preference is preserved`() {
        assertEquals(SpeechPolicy.EN, SpeechPolicy.replyLanguage("en", SpeechPolicy.Detected.BN))
        assertEquals("en-US", SpeechPolicy.sttPrimaryLocale("en"))
        assertEquals("bn-BD", SpeechPolicy.sttFallbackLocale("en"))
    }

    @Test
    fun `Bangla default recognizer retries English`() {
        assertEquals("bn-BD", SpeechPolicy.sttPrimaryLocale("bn"))
        assertEquals("en-US", SpeechPolicy.sttFallbackLocale("bn"))
    }

    @Test
    fun `detects Bangla English and mixed scripts`() {
        assertEquals(SpeechPolicy.Detected.BN, SpeechPolicy.detect("আজ আবহাওয়া কেমন"))
        assertEquals(SpeechPolicy.Detected.EN, SpeechPolicy.detect("check battery"))
        assertEquals(SpeechPolicy.Detected.MIXED, SpeechPolicy.detect("YouTube এ গান চালাও"))
    }

    @Test
    fun `Bengali voice sample is meaningful and complete`() {
        assertTrue(SpeechPolicy.hasMeaningfulBengaliContent(SpeechPolicy.BENGALI_VOICE_CHECK_SENTENCE))
        assertFalse(SpeechPolicy.hasMeaningfulBengaliContent("আ"))
        assertTrue(SpeechPolicy.confidenceTooLow(floatArrayOf(0.20f, 0.44f)))
        assertFalse(SpeechPolicy.confidenceTooLow(floatArrayOf(0.46f)))
    }

    @Test
    fun `only Bangladesh Bengali is accepted for Bengali speech output`() {
        assertTrue(SpeechPolicy.isBangladeshBengaliLocale("bn-BD"))
        assertFalse(SpeechPolicy.isBangladeshBengaliLocale("bn-IN"))
        assertFalse(SpeechPolicy.isBangladeshBengaliLocale("hi-IN"))
    }

    @Test
    fun `mixed mode still resolves to Bangladesh Bengali output`() {
        assertEquals(SpeechPolicy.BN, SpeechPolicy.replyLanguage("mixed", SpeechPolicy.Detected.MIXED))
        assertEquals("bn-BD", SpeechPolicy.ttsLocale("mixed"))
    }
}

package com.rajon.jarvis.agora

import org.junit.Assert.assertEquals
import org.junit.Test

class AgoraConfigTest {

    @Test
    fun `Indian Bengali and Hindi Agora settings are safely replaced with Bangladesh Bengali`() {
        val legacy = AgoraConfig(asrLanguage = "bn-IN", ttsVoice = "hi-IN-SwaraNeural")
        assertEquals("bn-BD", legacy.effectiveAsrLanguage)
        assertEquals("bn-BD-NabanitaNeural", legacy.effectiveTtsVoice)
    }

    @Test
    fun `Bangladesh Bengali Agora settings are kept`() {
        val config = AgoraConfig(asrLanguage = "bn-BD", ttsVoice = "bn-BD-NabanitaNeural")
        assertEquals("bn-BD", config.effectiveAsrLanguage)
        assertEquals("bn-BD-NabanitaNeural", config.effectiveTtsVoice)
    }
}

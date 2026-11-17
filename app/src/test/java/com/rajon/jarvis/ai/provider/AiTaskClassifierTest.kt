package com.rajon.jarvis.ai.provider

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiTaskClassifierTest {

    @Test
    fun `bengali script is detected`() {
        assertEquals(AiTask.BENGALI, AiTaskClassifier.classify("আজ সকালে ভালো লাগছে"))
        assertTrue(AiTaskClassifier.containsBengali("বাংলা"))
        assertTrue(!AiTaskClassifier.containsBengali("english only"))
    }

    @Test
    fun `coding requests are routed to coding models`() {
        assertEquals(AiTask.CODING, AiTaskClassifier.classify("please debug this stack trace"))
        assertEquals(AiTask.CODING, AiTaskClassifier.classify("write a python function for sorting"))
    }

    @Test
    fun `research requests are routed to research models`() {
        assertEquals(AiTask.WEB_RESEARCH, AiTaskClassifier.classify("search the web for the latest news"))
    }

    @Test
    fun `plain conversation stays conversational`() {
        assertEquals(AiTask.CONVERSATION, AiTaskClassifier.classify("good morning"))
    }

    @Test
    fun `voice input prefers the voice model`() {
        assertEquals(AiTask.VOICE, AiTaskClassifier.classify("good morning", fromVoice = true))
    }
}

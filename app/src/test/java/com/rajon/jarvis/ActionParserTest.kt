package com.rajon.jarvis

import com.rajon.jarvis.ai.ActionParser
import com.rajon.jarvis.ai.JarvisAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionParserTest {

    @Test
    fun testExtractOpenAppAction() {
        val aiResponse = """
            Certainly, Sir. Opening WhatsApp right away.
            ```json
            {"action": "OPEN_APP", "params": {"app_name": "whatsapp"}}
            ```
        """.trimIndent()

        val action = ActionParser.extractAction(aiResponse)
        assertNotNull(action)
        assertTrue(action is JarvisAction.OpenApp)
        assertEquals("whatsapp", (action as JarvisAction.OpenApp).appName)
    }

    @Test
    fun testExtractCheckBatteryAction() {
        val aiResponse = """
            Checking battery telemetry for you, Sir.
            ```json
            {"action": "CHECK_BATTERY", "params": {}}
            ```
        """.trimIndent()

        val action = ActionParser.extractAction(aiResponse)
        assertNotNull(action)
        assertTrue(action is JarvisAction.CheckBattery)
    }

    @Test
    fun testExtractSetReminderAction() {
        val aiResponse = """
            Setting a reminder for your meeting, Sir.
            ```json
            {"action": "SET_REMINDER", "params": {"title": "Team Standup", "minutes": 15}}
            ```
        """.trimIndent()

        val action = ActionParser.extractAction(aiResponse)
        assertNotNull(action)
        assertTrue(action is JarvisAction.SetReminder)
        val reminder = action as JarvisAction.SetReminder
        assertEquals("Team Standup", reminder.title)
        assertEquals(15, reminder.minutesFromNow)
    }

    @Test
    fun testCleanResponseText() {
        val aiResponse = """
            Certainly, Sir. Opening YouTube.
            ```json
            {"action": "SEARCH_YOUTUBE", "params": {"query": "cyberpunk lofi"}}
            ```
        """.trimIndent()

        val cleaned = ActionParser.cleanResponseText(aiResponse)
        assertEquals("Certainly, Sir. Opening YouTube.", cleaned)
    }
}

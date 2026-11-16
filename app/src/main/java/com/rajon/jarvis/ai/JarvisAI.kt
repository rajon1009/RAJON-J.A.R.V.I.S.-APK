package com.rajon.jarvis.ai

import android.provider.Settings
import com.google.ai.client.generativeai.GenerativeModel
import com.rajon.jarvis.data.local.PreferencesManager
import com.rajon.jarvis.device.DeviceController
import com.rajon.jarvis.device.ReminderManager
import com.rajon.jarvis.repository.MemoryRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

data class JarvisAiResult(
    val replyText: String,
    val executedActionDescription: String? = null
)

@Singleton
class JarvisAI @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val deviceController: DeviceController,
    private val reminderManager: ReminderManager,
    private val memoryRepository: MemoryRepository,
    private val conversationManager: ConversationManager
) {
    suspend fun processUserRequest(userText: String): JarvisAiResult {
        conversationManager.recordUserMessage(userText)

        val apiKey = preferencesManager.geminiApiKey.first()
        val owner = preferencesManager.ownerName.first()
        val language = preferencesManager.language.first()

        if (apiKey.isBlank()) {
            val fallback = if (language == "bn") {
                "$owner স্যার, দয়া করে সেটিংস-এ গিয়ে আপনার Gemini API Key সেট করুন।"
            } else {
                "Sir, please provide your Gemini API Key in the Settings page to activate my brain."
            }
            conversationManager.recordAssistantMessage(fallback)
            return JarvisAiResult(fallback)
        }

        val generativeModel = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey
        )

        val prompt = buildPrompt(userText, owner, language)

        val rawResponse = try {
            val response = generativeModel.generateContent(prompt)
            response.text ?: "No response from brain."
        } catch (e: Exception) {
            "Sir, I encountered an issue accessing my neural core: ${e.localizedMessage}"
        }

        val action = ActionParser.extractAction(rawResponse)
        var cleanedText = ActionParser.cleanResponseText(rawResponse)
        var actionSummary: String? = null

        if (action != null) {
            val result = executeJarvisAction(action, language)
            actionSummary = result
            if (cleanedText.isBlank()) {
                cleanedText = result
            }
        }

        conversationManager.recordAssistantMessage(cleanedText)
        return JarvisAiResult(cleanedText, actionSummary)
    }

    private suspend fun buildPrompt(userText: String, owner: String, language: String): String {
        val deviceInfo = deviceController.getFullDeviceInfo()
        val memories = memoryRepository.getAllOnce().joinToString("\n") { "- ${it.key}: ${it.value}" }
        val recentChat = conversationManager.getRecentContext().joinToString("\n") {
            "${it.role.name}: ${it.content}"
        }

        return """
You are J.A.R.V.I.S., the ultimate personal Artificial Intelligence assistant created exclusively for your owner and master, $owner Sir.
Personality & Rules:
1. Always address the owner respectfully as "$owner Sir" or "Sir".
2. You speak natural fluent ${if (language == "bn") "Bengali (Bangla)" else "English"}. You can also seamlessly switch between Bangla and English based on the language Sir speaks to you.
3. You are confident, polite, proactive, highly intelligent, and loyal.
4. If Sir asks you to do an Android device task, you MUST append an executable JSON action block at the end of your response in this exact format:
```json
{"action": "ACTION_NAME", "params": {"key": "value"}}
```
Available actions:
- OPEN_APP: {"action": "OPEN_APP", "params": {"app_name": "whatsapp"}}
- CHECK_BATTERY: {"action": "CHECK_BATTERY", "params": {}}
- CHECK_STORAGE: {"action": "CHECK_STORAGE", "params": {}}
- CHECK_INTERNET: {"action": "CHECK_INTERNET", "params": {}}
- CHECK_PERFORMANCE: {"action": "CHECK_PERFORMANCE", "params": {}}
- OPEN_WHATSAPP: {"action": "OPEN_WHATSAPP", "params": {"phone": "optional", "message": "optional"}}
- SEARCH_YOUTUBE: {"action": "SEARCH_YOUTUBE", "params": {"query": "music name"}}
- SET_REMINDER: {"action": "SET_REMINDER", "params": {"title": "wake up", "minutes": 30}}
- OPEN_SETTINGS: {"action": "OPEN_SETTINGS", "params": {"section": "android.settings.SETTINGS"}}

Current Device State:
- Battery: ${deviceInfo.batteryLevel}% (Charging: ${deviceInfo.isCharging})
- Storage: Free ${"%.1f".format(deviceInfo.availableStorageGB)}GB / Total ${"%.1f".format(deviceInfo.totalStorageGB)}GB
- RAM: Available ${deviceInfo.availableRamMB}MB / Total ${deviceInfo.totalRamMB}MB
- Wi-Fi: ${deviceInfo.isWifiConnected}, Mobile Data: ${deviceInfo.isMobileDataConnected}
- Model: ${deviceInfo.deviceBrand} ${deviceInfo.deviceModel} (Android ${deviceInfo.androidVersion})

Owner Memories:
$memories

Recent Conversation:
$recentChat

Current Instruction from Sir:
$userText
""".trimIndent()
    }

    private suspend fun executeJarvisAction(action: JarvisAction, language: String): String {
        return when (action) {
            is JarvisAction.CheckBattery -> {
                val (level, charging) = deviceController.getBatteryInfo()
                if (language == "bn") "ব্যাটারি লেভেল: $level% ${if (charging) "(চার্জ হচ্ছে)" else ""}"
                else "Battery Level: $level% ${if (charging) "(Charging)" else ""}"
            }
            is JarvisAction.CheckStorage -> {
                val st = deviceController.getStorageInfo()
                if (language == "bn") "স্টোরেজ: ${"%.1f".format(st.availableGB)} GB খালি আছে (মোট ${"%.1f".format(st.totalGB)} GB)"
                else "Storage: ${"%.1f".format(st.availableGB)} GB free of ${"%.1f".format(st.totalGB)} GB total"
            }
            is JarvisAction.CheckInternet -> {
                val net = deviceController.getNetworkInfo()
                if (language == "bn") "ইন্টারনেট অবস্থা: ${net.networkType} (সংযুক্ত: ${net.isConnected})"
                else "Internet Status: ${net.networkType} (Connected: ${net.isConnected})"
            }
            is JarvisAction.CheckPerformance -> {
                deviceController.getPerformanceSummary()
            }
            is JarvisAction.OpenApp -> {
                val app = deviceController.searchApp(action.appName)
                if (app != null) {
                    val ok = deviceController.launchApp(app.packageName)
                    if (ok) "Opened ${app.appName}." else "Could not launch ${app.appName}."
                } else {
                    "App '${action.appName}' not found on device."
                }
            }
            is JarvisAction.OpenWhatsApp -> {
                if (!action.phone.isNullOrBlank() && !action.message.isNullOrBlank()) {
                    deviceController.sendWhatsAppMessage(action.phone, action.message)
                    "Drafting WhatsApp message for ${action.phone}."
                } else {
                    deviceController.openWhatsApp()
                    "Opening WhatsApp."
                }
            }
            is JarvisAction.SearchYouTube -> {
                deviceController.openYouTubeSearch(action.query)
                "Searching YouTube for '${action.query}'."
            }
            is JarvisAction.SetReminder -> {
                val triggerTime = System.currentTimeMillis() + (action.minutesFromNow * 60 * 1000L)
                reminderManager.scheduleReminder(action.title, "Scheduled by J.A.R.V.I.S.", triggerTime)
                "Reminder set for '${action.title}' in ${action.minutesFromNow} minutes."
            }
            is JarvisAction.OpenSettings -> {
                deviceController.openSettings(action.section ?: Settings.ACTION_SETTINGS)
                "Opening settings."
            }
            is JarvisAction.Unknown -> "Action unrecognized: ${action.raw}"
        }
    }
}

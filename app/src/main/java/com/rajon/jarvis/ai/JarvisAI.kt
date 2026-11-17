package com.rajon.jarvis.ai

import android.provider.Settings
import com.rajon.jarvis.ai.provider.AiRouter
import com.rajon.jarvis.ai.provider.AiTask
import com.rajon.jarvis.ai.provider.AiTaskClassifier
import com.rajon.jarvis.ai.provider.FailoverOutcome
import com.rajon.jarvis.data.local.PreferencesManager
import com.rajon.jarvis.data.model.MessageRole
import com.rajon.jarvis.device.DeviceController
import com.rajon.jarvis.device.ReminderManager
import com.rajon.jarvis.repository.MemoryRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

data class JarvisAiResult(
    val replyText: String,
    val executedActionDescription: String? = null,
    val isError: Boolean = false,
    /** Which provider produced the answer, e.g. "Google Gemini". */
    val providerLabel: String? = null,
    /** Human readable failover trail, empty when the first provider answered. */
    val failoverTrail: List<String> = emptyList()
)

@Singleton
class JarvisAI @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val deviceController: DeviceController,
    private val reminderManager: ReminderManager,
    private val memoryRepository: MemoryRepository,
    private val conversationManager: ConversationManager,
    private val geminiClient: GeminiClient,
    private val aiRouter: AiRouter
) {
    suspend fun processUserRequest(userText: String, fromVoice: Boolean = false): JarvisAiResult {
        conversationManager.recordUserMessage(userText)

        val owner = preferencesManager.ownerName.first()
        val language = preferencesManager.language.first()

        if (aiRouter.activeProviders().isEmpty()) {
            val fallback = if (language == "bn") {
                "$owner স্যার, দয়া করে AI Settings-এ গিয়ে আপনার Gemini API Key সেট করুন।"
            } else {
                "Sir, please provide your Gemini API Key in AI Settings to activate my neural core."
            }
            conversationManager.recordAssistantMessage(fallback)
            return JarvisAiResult(fallback, isError = true)
        }

        // History already contains the message we just stored; drop the trailing duplicate.
        val history = conversationManager.getRecentContext().let { messages ->
            val last = messages.lastOrNull()
            if (last != null && last.role == MessageRole.USER && last.content == userText) {
                messages.dropLast(1)
            } else {
                messages
            }
        }

        val task: AiTask = AiTaskClassifier.classify(userText, fromVoice)

        val outcome = aiRouter.generate(
            systemInstruction = buildSystemInstruction(owner, language),
            history = history,
            userText = userText,
            task = task
        )

        val rawResponse: String
        var providerLabel: String? = null
        var failoverTrail: List<String> = emptyList()

        when (outcome) {
            is FailoverOutcome.Success -> {
                rawResponse = outcome.text
                providerLabel = "${outcome.displayName} - ${outcome.model}"
                failoverTrail = outcome.attempts
                    .filter { !it.success }
                    .map { "${it.displayName}: ${it.message ?: "failed"}" }
            }

            is FailoverOutcome.Failure -> {
                val detail = outcome.message
                val message = if (language == "bn") {
                    "স্যার, নিউরাল কোরের সাথে সংযোগ করা যায়নি। $detail"
                } else {
                    "Sir, I could not reach my neural core. $detail"
                }
                conversationManager.recordAssistantMessage(message)
                return JarvisAiResult(
                    replyText = message,
                    isError = true,
                    providerLabel = null,
                    failoverTrail = outcome.attempts.map { "${it.displayName}: ${it.message ?: "failed"}" }
                )
            }
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

        if (cleanedText.isBlank()) {
            cleanedText = "No response from neural core."
        }

        conversationManager.recordAssistantMessage(cleanedText)
        return JarvisAiResult(
            replyText = cleanedText,
            executedActionDescription = actionSummary,
            providerLabel = providerLabel,
            failoverTrail = failoverTrail
        )
    }

    suspend fun testConnection(apiKey: String): ConnectionTestResult {
        return geminiClient.testConnection(apiKey)
    }

    private suspend fun buildSystemInstruction(owner: String, language: String): String {
        val deviceInfo = deviceController.getFullDeviceInfo()
        val memories = memoryRepository.getAllOnce().joinToString("\n") { "- ${it.key}: ${it.value}" }

        return """
You are J.A.R.V.I.S., the ultimate personal Artificial Intelligence assistant created exclusively for your owner and master, $owner Sir.
Personality & Rules:
1. Always address the owner respectfully as "$owner Sir" or "Sir".
2. You speak natural fluent ${if (language == "bn") "Bengali (Bangla)" else "English"}. You can also seamlessly switch between Bangla and English based on the language Sir speaks to you.
3. You are confident, polite, proactive, highly intelligent, and loyal.
4. Keep spoken answers concise (2-4 sentences) unless Sir asks for detail, because replies are read aloud.
5. If Sir asks you to do an Android device task, you MUST append an executable JSON action block at the end of your response in this exact format:
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
${memories.ifBlank { "- none yet" }}
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
                    "Drafting WhatsApp message for ${action.phone}. Press send in WhatsApp to confirm."
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

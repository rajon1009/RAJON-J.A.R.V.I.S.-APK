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
import com.rajon.jarvis.voice.SpeechPolicy
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
    private val aiRouter: AiRouter,
    private val localCommandRecognizer: LocalCommandRecognizer
) {
    suspend fun processUserRequest(userText: String, fromVoice: Boolean = false): JarvisAiResult {
        conversationManager.recordUserMessage(userText)

        val owner = preferencesManager.ownerName.first()
        val language = SpeechPolicy.replyLanguage(
            preferencesManager.language.first(),
            SpeechPolicy.detect(userText)
        )

        // 1. Instant Local Device Command Check (Instant execution in < 10ms, no network delay)
        val localMatch = localCommandRecognizer.match(userText, language, owner)
        if (localMatch != null) {
            val actionResult = executeJarvisAction(localMatch.action, language)
            conversationManager.recordAssistantMessage(localMatch.immediateSpokenResponse)
            return JarvisAiResult(
                replyText = localMatch.immediateSpokenResponse,
                executedActionDescription = actionResult,
                providerLabel = "J.A.R.V.I.S. Local Core"
            )
        }

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
            systemInstruction = buildSystemInstruction(owner, language, fromVoice),
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
            cleanedText = if (language == SpeechPolicy.BN) {
                "নিউরাল কোর থেকে কোনো উত্তর পাওয়া যায়নি।"
            } else {
                "No response from neural core."
            }
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

    private suspend fun buildSystemInstruction(owner: String, language: String, fromVoice: Boolean = false): String {
        val deviceInfo = deviceController.getFullDeviceInfo()
        val memories = memoryRepository.getAllOnce().joinToString("\n") { "- ${it.key}: ${it.value}" }

        val voiceRule = if (fromVoice) {
            "4. This is a real-time voice query. Keep answers very concise, direct, and quick (1-2 short sentences maximum), because replies are read aloud immediately."
        } else {
            "4. Keep spoken answers concise (2-4 sentences) unless Sir asks for detail, because replies are read aloud."
        }

        return """
You are J.A.R.V.I.S., the ultimate personal Artificial Intelligence assistant created exclusively for your owner and master, $owner Sir.
Personality & Rules:
1. Always address the owner respectfully as "$owner Sir" or "Sir".
2. You speak ${if (language == SpeechPolicy.BN) "natural, standard Bangladeshi Bengali (Bangla)" else "natural English"}. ${if (language == SpeechPolicy.BN) SpeechPolicy.BANGLADESHI_BENGALI_RESPONSE_INSTRUCTION else "An explicit English preference is enabled."}
3. You are confident, polite, proactive, highly intelligent, and loyal.
$voiceRule
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
                if (language == SpeechPolicy.BN) "ব্যাটারির চার্জ: $level% ${if (charging) "(চার্জ হচ্ছে)" else ""}"
                else "Battery Level: $level% ${if (charging) "(Charging)" else ""}"
            }
            is JarvisAction.CheckStorage -> {
                val st = deviceController.getStorageInfo()
                if (language == SpeechPolicy.BN) "স্টোরেজে ${"%.1f".format(st.availableGB)} GB খালি আছে (মোট ${"%.1f".format(st.totalGB)} GB)।"
                else "Storage: ${"%.1f".format(st.availableGB)} GB free of ${"%.1f".format(st.totalGB)} GB total"
            }
            is JarvisAction.CheckInternet -> {
                val net = deviceController.getNetworkInfo()
                if (language == SpeechPolicy.BN) "ইন্টারনেটের অবস্থা: ${net.networkType} (সংযুক্ত: ${net.isConnected})"
                else "Internet Status: ${net.networkType} (Connected: ${net.isConnected})"
            }
            is JarvisAction.CheckPerformance -> {
                deviceController.getPerformanceSummary()
            }
            is JarvisAction.OpenApp -> {
                val app = deviceController.searchApp(action.appName)
                if (app != null) {
                    val ok = deviceController.launchApp(app.packageName)
                    if (language == SpeechPolicy.BN) {
                        if (ok) "অবশ্যই স্যার, ${app.appName} চালু করছি।" else "স্যার, ${app.appName} চালু করা যায়নি।"
                    } else {
                        if (ok) "Opening ${app.appName}, Sir." else "Could not launch ${app.appName}, Sir."
                    }
                } else {
                    if (language == SpeechPolicy.BN) "স্যার, ফোনে '${action.appName}' অ্যাপটি পাওয়া যায়নি।"
                    else "App '${action.appName}' not found on device, Sir."
                }
            }
            is JarvisAction.OpenWhatsApp -> {
                if (!action.phone.isNullOrBlank() && !action.message.isNullOrBlank()) {
                    deviceController.sendWhatsAppMessage(action.phone, action.message)
                    if (language == SpeechPolicy.BN) "স্যার, ${action.phone} নম্বরে হোয়াটসঅ্যাপ মেসেজ প্রস্তুত করা হয়েছে।"
                    else "Drafting WhatsApp message for ${action.phone}, Sir."
                } else {
                    val ok = deviceController.openWhatsApp()
                    if (language == SpeechPolicy.BN) {
                        if (ok) "অবশ্যই স্যার, হোয়াটসঅ্যাপ চালু করছি।" else "স্যার, হোয়াটসঅ্যাপ চালু করা যায়নি।"
                    } else {
                        if (ok) "Opening WhatsApp, Sir." else "Could not open WhatsApp, Sir."
                    }
                }
            }
            is JarvisAction.SearchYouTube -> {
                val ok = deviceController.openYouTubeSearch(action.query)
                if (language == SpeechPolicy.BN) {
                    if (action.query.isNotBlank()) "অবশ্যই স্যার, ইউটিউবে '${action.query}' সার্চ করছি।"
                    else "অবশ্যই স্যার, ইউটিউব চালু করছি।"
                } else {
                    if (action.query.isNotBlank()) "Searching YouTube for '${action.query}', Sir."
                    else "Opening YouTube, Sir."
                }
            }
            is JarvisAction.SetReminder -> {
                val triggerTime = System.currentTimeMillis() + (action.minutesFromNow * 60 * 1000L)
                reminderManager.scheduleReminder(action.title, "Scheduled by J.A.R.V.I.S.", triggerTime)
                if (language == SpeechPolicy.BN) "স্যার, '${action.title}'-এর জন্য ${action.minutesFromNow} মিনিটের রিমাইন্ডার সেট করা হয়েছে।"
                else "Reminder set for '${action.title}' in ${action.minutesFromNow} minutes, Sir."
            }
            is JarvisAction.OpenSettings -> {
                deviceController.openSettings(action.section ?: Settings.ACTION_SETTINGS)
                if (language == SpeechPolicy.BN) "অবশ্যই স্যার, সেটিংস ওপেন করছি।"
                else "Opening settings, Sir."
            }
            is JarvisAction.Unknown -> "Action unrecognized: ${action.raw}"
        }
    }
}

package com.rajon.jarvis.agora

/**
 * Configuration for the Agora Conversational Voice Agent.
 *
 * Stored as JSON in the app's private DataStore. No value is compiled into the
 * APK. Two modes are supported:
 *
 *  - BACKEND: set [backendUrl] to a token server (recommended by Agora). The
 *    server keeps the App Certificate and exposes /get_config, /startAgent and
 *    /stopAgent. Nothing secret lives on the device.
 *  - MANUAL: set [appId], [rtcToken], [channel] directly (a temporary RTC token
 *    from the Agora Console is enough for testing).
 */
data class AgoraConfig(
    val backendUrl: String = "",
    val appId: String = "",
    val rtcToken: String = "",
    val restToken: String = "",
    val customerId: String = "",
    val customerSecret: String = "",
    val channel: String = "jarvis-voice",
    val rtcUid: Int = 1002,
    val agentUid: Int = 1001,
    val agentName: String = "jarvis-agent",
    val enableManagedCredentials: Boolean = true,
    val llmUrl: String = "api.openai.com/v1/chat/completions",
    val llmApiKey: String = "",
    val llmModel: String = "gpt-4o-mini",
    val systemPrompt: String = "You are J.A.R.V.I.S., a loyal and concise personal assistant. Address the user as Sir.",
    val greetingMessage: String = "Sir, J.A.R.V.I.S. is online and listening.",
    val asrLanguage: String = "bn-BD",
    val ttsVendor: String = "microsoft",
    val ttsVoice: String = "bn-BD-NabanitaNeural",
    val idleTimeoutSeconds: Int = 120
) {
    val isReady: Boolean
        get() = backendUrl.isNotBlank() ||
            (appId.isNotBlank() && rtcToken.isNotBlank() && channel.isNotBlank())

    /** REST auth header for the Conversational AI endpoints. */
    fun restAuthHeader(): String = when {
        restToken.isNotBlank() -> "agora token=\"$restToken\""
        customerId.isNotBlank() && customerSecret.isNotBlank() ->
            "Basic ${java.util.Base64.getEncoder().encodeToString("$customerId:$customerSecret".toByteArray())}"
        else -> ""
    }
}

data class AgoraTranscript(
    val turnId: String,
    val type: String,
    val text: String
)

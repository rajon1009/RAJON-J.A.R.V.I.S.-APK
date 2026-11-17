package com.rajon.jarvis.agora

import android.content.Context
import android.util.Log
import com.google.gson.JsonParser
import com.rajon.jarvis.ai.provider.HttpTransport
import com.rajon.jarvis.data.local.PreferencesManager
import com.rajon.jarvis.voice.SpeechPolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import io.agora.conversational.api.ConversationalAIAPIConfig
import io.agora.conversational.api.ConversationalAIAPIImpl
import io.agora.conversational.api.IConversationalAIAPI
import io.agora.conversational.api.IConversationalAIAPIEventHandler
import io.agora.conversational.api.InterruptEvent
import io.agora.conversational.api.MessageError
import io.agora.conversational.api.MessageReceipt
import io.agora.conversational.api.Metric
import io.agora.conversational.api.ModuleError
import io.agora.conversational.api.StateChangeEvent
import io.agora.conversational.api.Transcript
import io.agora.conversational.api.TranscriptRenderMode
import io.agora.conversational.api.Turn
import io.agora.conversational.api.VoiceprintStateChangeEvent
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.RtcEngineEx
import io.agora.rtm.ErrorInfo
import io.agora.rtm.ResultCallback
import io.agora.rtm.RtmClient
import io.agora.rtm.RtmConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Live state of the Agora voice session. */
sealed class AgoraSessionState {
    data object Idle : AgoraSessionState()
    data object Connecting : AgoraSessionState()
    data object Listening : AgoraSessionState()
    data object Speaking : AgoraSessionState()
    data class Error(val message: String) : AgoraSessionState()
}

/**
 * Owns the RTC engine, RTM client and the official Agora Conversational AI
 * client toolkit. Keeps every credential in [PreferencesManager] DataStore —
 * nothing is compiled into the APK or written to logs.
 */
@Singleton
class AgoraVoiceAgent @Inject constructor(
    @ApplicationContext private val app: Context,
    private val preferencesManager: PreferencesManager,
    private val transport: HttpTransport
) : IConversationalAIAPIEventHandler {

    companion object {
        private const val TAG = "JARVIS_Agora"
    }

    private var rtcEngine: RtcEngineEx? = null
    private var rtmClient: RtmClient? = null
    private var convoApi: IConversationalAIAPI? = null
    private var channelName: String = ""
    private var agentId: String? = null
    private var config: AgoraConfig = AgoraConfig()

    private val _state = MutableStateFlow<AgoraSessionState>(AgoraSessionState.Idle)
    val state: StateFlow<AgoraSessionState> = _state.asStateFlow()

    private val _transcripts = MutableStateFlow<List<AgoraTranscript>>(emptyList())
    val transcripts: StateFlow<List<AgoraTranscript>> = _transcripts.asStateFlow()

    private val rtcHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            Log.i(TAG, "RTC joined channel=$channel uid=$uid")
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.i(TAG, "RTC user joined uid=$uid")
        }

        override fun onError(err: Int) {
            Log.e(TAG, "RTC error $err")
            _state.value = AgoraSessionState.Error("RTC error $err")
        }
    }

    /** Starts the live voice session. Resolves config from DataStore on every call. */
    suspend fun start(): String? {
        if (_state.value !is AgoraSessionState.Idle && _state.value !is AgoraSessionState.Error) {
            return "A voice session is already running."
        }

        config = runCatching {
            com.google.gson.Gson().fromJson(
                preferencesManager.agoraConfig.first(),
                AgoraConfig::class.java
            )
        }.getOrNull() ?: AgoraConfig()

        if (!config.isReady) {
            return "Agora is not configured. Add a backend URL, or an App ID and RTC token in AI Settings."
        }

        _state.value = AgoraSessionState.Connecting
        _transcripts.value = emptyList()

        return try {
            val resolved = resolveConfig(config)
            initRtcAndRtm(resolved)
            null
        } catch (e: Exception) {
            release()
            val message = "Could not start Agora voice: ${e.message ?: "unknown"}"
            _state.value = AgoraSessionState.Error(message)
            Log.e(TAG, message, e)
            message
        }
    }

    private data class ResolvedConfig(
        val appId: String,
        val token: String,
        val channel: String,
        val rtcUid: Int,
        val agentUid: Int,
        val restAuth: String
    )

    private suspend fun resolveConfig(source: AgoraConfig): ResolvedConfig {
        if (source.backendUrl.isNotBlank()) {
            // Official zero-key pattern: the server holds the App Certificate.
            val url = "${source.backendUrl.trimEnd('/')}/get_config"
            val response = transport.get(url, emptyMap())
            if (response.code !in 200..299) {
                throw IllegalStateException("Token server returned HTTP ${response.code}")
            }
            val root = JsonParser.parseString(response.body).asJsonObject
            fun string(name: String) = root.get(name)?.takeIf { !it.isJsonNull }?.asString ?: ""
            return ResolvedConfig(
                appId = string("app_id").ifBlank { string("appId") },
                token = string("token"),
                channel = string("channel_name").ifBlank { string("channel") }.ifBlank { source.channel },
                rtcUid = runCatching { string("uid").toInt() }.getOrDefault(source.rtcUid),
                agentUid = runCatching { string("agent_uid").toInt() }.getOrDefault(source.agentUid),
                restAuth = "" // backend starts/stops the agent itself
            )
        }

        return ResolvedConfig(
            appId = source.appId,
            token = source.rtcToken,
            channel = source.channel,
            rtcUid = source.rtcUid,
            agentUid = source.agentUid,
            restAuth = source.restAuthHeader()
        )
    }

    private fun initRtcAndRtm(resolved: ResolvedConfig) {
        channelName = resolved.channel

        val rtcConfig = RtcEngineConfig().apply {
            mContext = app.applicationContext
            mAppId = resolved.appId
            mChannelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
            mAudioScenario = Constants.AUDIO_SCENARIO_AI_CLIENT
            mEventHandler = rtcHandler
        }
        val rtc = RtcEngine.create(rtcConfig) as RtcEngineEx
        rtcEngine = rtc

        val rtmConfig = RtmConfig.Builder(resolved.appId, resolved.rtcUid.toString()).build()
        val rtm = RtmClient.create(rtmConfig)
        rtmClient = rtm

        val api = ConversationalAIAPIImpl(
            ConversationalAIAPIConfig(
                rtcEngine = rtc,
                rtmClient = rtm,
                renderMode = TranscriptRenderMode.Text,
                enableLog = false
            )
        )
        convoApi = api
        api.addHandler(this)

        rtm.login(resolved.token, object : ResultCallback<Void> {
            override fun onSuccess(responseInfo: Void?) {
                api.loadAudioSettings(Constants.AUDIO_SCENARIO_AI_CLIENT)
                val options = ChannelMediaOptions().apply {
                    clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                    publishMicrophoneTrack = true
                    autoSubscribeAudio = true
                }
                rtc.joinChannel(resolved.token, resolved.channel, resolved.rtcUid, options)

                // Subscribe to agent messages BEFORE starting the agent.
                api.subscribeMessage(resolved.channel) { error ->
                    if (error == null) {
                        _state.value = AgoraSessionState.Listening
                        startAgent(resolved)
                    } else {
                        _state.value = AgoraSessionState.Error(
                            "subscribeMessage failed: ${error.errorMessage}"
                        )
                    }
                }
            }

            override fun onFailure(errorInfo: ErrorInfo?) {
                val message = errorInfo?.errorReason ?: "unknown RTM login error"
                _state.value = AgoraSessionState.Error("RTM login failed: $message")
            }
        })
    }

    /** Starts the agent through the token server, or directly through Agora REST. */
    private fun startAgent(resolved: ResolvedConfig) {
        if (config.backendUrl.isNotBlank()) {
            val body = """
                {"rtcUid": ${config.agentUid}, "userUid": ${config.rtcUid}}
            """.trimIndent()
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                val response = transport.postJson(
                    url = "${config.backendUrl.trimEnd('/')}/startAgent",
                    headers = mapOf("Content-Type" to "application/json"),
                    body = body
                )
                if (response.code !in 200..299) {
                    _state.value = AgoraSessionState.Error(
                        "Agent start failed: HTTP ${response.code}"
                    )
                }
            }
            return
        }

        if (resolved.restAuth.isBlank()) {
            _state.value = AgoraSessionState.Error("No Agora REST credential configured.")
            return
        }

        val asr = buildAsrConfig()
        val llm = buildLlmConfig()
        val tts = buildTtsConfig()
        val body = buildAgentStartBody(resolved, asr, llm, tts)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val response = transport.postJson(
                url = "https://api.agora.io/api/conversational-ai-agent/v2/projects/${resolved.appId}/join",
                headers = mapOf(
                    "Content-Type" to "application/json",
                    "Authorization" to resolved.restAuth
                ),
                body = body
            )
            if (response.code in 200..299) {
                agentId = runCatching {
                    JsonParser.parseString(response.body).asJsonObject.get("agent_id")?.asString
                }.getOrNull()
            } else {
                _state.value = AgoraSessionState.Error(
                    "Agent start failed: HTTP ${response.code} ${response.body.take(200)}"
                )
            }
        }
    }

    private fun buildAgentStartBody(
        resolved: ResolvedConfig,
        asr: String,
        llm: String,
        tts: String
    ): String {
        val credentialMode = if (config.enableManagedCredentials) {
            "\"credential_mode\": \"managed\","
        } else {
            ""
        }
        return """
            {
              "name": "${config.agentName}",
              "properties": {
                "channel": "${resolved.channel}",
                "token": "${resolved.token}",
                "agent_rtc_uid": "${resolved.agentUid}",
                "remote_rtc_uids": ["${resolved.rtcUid}"],
                "enable_string_uid": false,
                "idle_timeout": ${config.idleTimeoutSeconds},
                $asr,
                $llm,
                $tts
              }
            }
        """.trimIndent()
    }

    private fun buildAsrConfig(): String {
        val mode = if (config.enableManagedCredentials) "\"credential_mode\": \"managed\"," else ""
        return """
            "asr": {
              $mode
              "vendor": "microsoft",
              "params": {"language": "${config.effectiveAsrLanguage}"}
            }
        """.trimIndent()
    }

    private fun buildLlmConfig(): String {
        val mode = if (config.enableManagedCredentials) "\"credential_mode\": \"managed\"," else ""
        val apiKey = if (config.enableManagedCredentials) "" else
            "\"api_key\": \"${config.llmApiKey}\","
        return """
            "llm": {
              $mode
              $apiKey
              "url": "${config.llmUrl}",
              "style": "openai",
              "system_messages": [{"role": "system", "content": "${(config.systemPrompt + " " + SpeechPolicy.BANGLADESHI_BENGALI_RESPONSE_INSTRUCTION).replace("\"", "\\\"")}"}],
              "greeting_message": "${config.greetingMessage.replace("\"", "\\\"")}",
              "failure_message": "দুঃখিত স্যার, এই মুহূর্তে উত্তর দিতে পারছি না।",
              "max_history": 10,
              "params": {"model": "${config.llmModel}"}
            }
        """.trimIndent()
    }

    private fun buildTtsConfig(): String {
        val mode = if (config.enableManagedCredentials) "\"credential_mode\": \"managed\"," else ""
        return """
            "tts": {
              $mode
              "vendor": "${config.ttsVendor}",
              "params": {"voice_name": "${config.effectiveTtsVoice}"}
            }
        """.trimIndent()
    }

    fun setMuted(muted: Boolean) {
        rtcEngine?.adjustRecordingSignalVolume(if (muted) 0 else 100)
    }

    /** Stops the agent and releases all Agora resources. */
    fun stop() {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            stopAgentOnServer()
        }
        release()
        _state.value = AgoraSessionState.Idle
    }

    private suspend fun stopAgentOnServer() {
        try {
            if (config.backendUrl.isNotBlank()) {
                transport.postJson(
                    url = "${config.backendUrl.trimEnd('/')}/stopAgent",
                    headers = mapOf("Content-Type" to "application/json"),
                    body = "{}"
                )
            } else if (!agentId.isNullOrBlank() && config.appId.isNotBlank() && config.restAuthHeader().isNotBlank()) {
                transport.postJson(
                    url = "https://api.agora.io/api/conversational-ai-agent/v2/projects/${config.appId}/agents/$agentId/leave",
                    headers = mapOf(
                        "Content-Type" to "application/json",
                        "Authorization" to config.restAuthHeader()
                    ),
                    body = "{}"
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to stop agent on server: ${e.message}")
        }
    }

    private fun release() {
        convoApi?.unsubscribeMessage(channelName) { }
        convoApi?.removeHandler(this)
        convoApi?.destroy()
        convoApi = null
        rtcEngine?.leaveChannel()
        rtcEngine = null
        RtcEngine.destroy()
        rtmClient?.logout(object : ResultCallback<Void> {
            override fun onSuccess(responseInfo: Void?) {}
            override fun onFailure(errorInfo: ErrorInfo?) {}
        })
        rtmClient = null
        RtmClient.release()
        agentId = null
    }

    // ── Toolkit event handler ────────────────────────────────────────────────

    override fun onAgentStateChanged(agentUserId: String, event: StateChangeEvent) {
        Log.i(TAG, "agent state=${event.state} turn=${event.turnId}")
    }

    override fun onAgentListeningChanged(agentUserId: String, isListening: Boolean) {
        if (isListening) _state.value = AgoraSessionState.Listening
    }

    override fun onAgentThinkingChanged(agentUserId: String, isThinking: Boolean) {
        // no-op
    }

    override fun onAgentSpeakingChanged(agentUserId: String, isSpeaking: Boolean) {
        if (isSpeaking) _state.value = AgoraSessionState.Speaking
    }

    override fun onAgentInterrupted(agentUserId: String, event: InterruptEvent) {}

    override fun onAgentMetrics(agentUserId: String, metric: Metric) {}

    override fun onTurnFinished(agentUserId: String, turn: Turn) {}

    override fun onAgentError(agentUserId: String, error: ModuleError) {
        _state.value = AgoraSessionState.Error("Agent error ${error.code}: ${error.message}")
    }

    override fun onMessageError(agentUserId: String, error: MessageError) {
        _state.value = AgoraSessionState.Error("Message error ${error.code}: ${error.message}")
    }

    override fun onMessageReceiptUpdated(agentUserId: String, receipt: MessageReceipt) {}

    override fun onAgentVoiceprintStateChanged(agentUserId: String, event: VoiceprintStateChangeEvent) {}

    override fun onTranscriptUpdated(agentUserId: String, transcript: Transcript) {
        val text = transcript.text ?: return
        if (text.isBlank()) return
        val entry = AgoraTranscript(
            turnId = transcript.turnId?.toString() ?: "",
            type = transcript.type?.toString() ?: "",
            text = text
        )
        _transcripts.value = (_transcripts.value + entry).takeLast(50)
    }

    override fun onDebugLog(log: String) {
        Log.d(TAG, log)
    }
}

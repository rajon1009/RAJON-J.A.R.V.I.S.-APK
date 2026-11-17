package com.rajon.jarvis.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajon.jarvis.ai.ConnectionTestResult
import com.rajon.jarvis.ai.GeminiClient
import com.rajon.jarvis.ai.JarvisAI
import com.rajon.jarvis.ai.provider.AiRouter
import com.rajon.jarvis.ai.provider.ProviderConfig
import com.rajon.jarvis.ai.provider.ProviderStore
import com.rajon.jarvis.data.local.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProviderUiState(
    val id: String,
    val isBusy: Boolean = false,
    val isSuccess: Boolean? = null,
    val message: String? = null,
    val discoveredModels: List<String> = emptyList()
)

@HiltViewModel
class AiSettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val jarvisAi: JarvisAI,
    private val providerStore: ProviderStore,
    private val aiRouter: AiRouter
) : ViewModel() {

    val apiKey: StateFlow<String> = preferencesManager.geminiApiKey
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    val modelName: String = GeminiClient.MODEL_NAME
    val apiVersion: String = GeminiClient.API_VERSION

    /** Every known provider with its stored credential/model state. */
    val providerConfigs: StateFlow<List<ProviderConfig>> = providerStore.configs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** The automatic failover chain, in the order the assistant will try them. */
    private val _failoverChain = MutableStateFlow<List<ProviderConfig>>(emptyList())
    val failoverChain: StateFlow<List<ProviderConfig>> = _failoverChain.asStateFlow()

    private val _providerStates = MutableStateFlow<Map<String, ProviderUiState>>(emptyMap())
    val providerStates: StateFlow<Map<String, ProviderUiState>> = _providerStates.asStateFlow()

    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting.asStateFlow()

    private val _testResult = MutableStateFlow<ConnectionTestResult?>(null)
    val testResult: StateFlow<ConnectionTestResult?> = _testResult.asStateFlow()

    private val _feedback = MutableStateFlow<String?>(null)
    val feedback: StateFlow<String?> = _feedback.asStateFlow()

    init {
        viewModelScope.launch {
            providerStore.configs.collect { configs ->
                _failoverChain.value = configs
                    .filter { it.enabled && it.isConfigured }
                    .sortedBy { it.priority }
            }
        }
    }

    fun saveApiKey(key: String) {
        viewModelScope.launch {
            preferencesManager.setGeminiApiKey(key.trim())
            _testResult.value = null
            _feedback.value = if (key.isBlank()) {
                "API key cleared"
            } else {
                "API key saved securely on this device"
            }
        }
    }

    fun testConnection(apiKeyInput: String) {
        if (_isTesting.value) return
        viewModelScope.launch {
            val key = apiKeyInput.trim()
            if (key.isBlank()) {
                _testResult.value = ConnectionTestResult(false, "Enter a Gemini API key first.")
                return@launch
            }
            _isTesting.value = true
            _testResult.value = null
            // Save exactly what is tested. Previously the screen tested the
            // previously saved key, so a newly typed key was silently ignored.
            preferencesManager.setGeminiApiKey(key)
            _feedback.value = "API key saved securely on this device"
            _testResult.value = jarvisAi.testConnection(key)
            _isTesting.value = false
        }
    }

    /** Stores the credential and model of one provider. */
    fun saveProvider(id: String, model: String, apiKey: String) {
        viewModelScope.launch {
            val configs = providerStore.currentConfigs()
            val config = configs.firstOrNull { it.id == id } ?: return@launch
            if (id == "gemini") {
                preferencesManager.setGeminiApiKey(apiKey.trim())
            } else {
                providerStore.saveProvider(
                    config.copy(model = model.trim(), apiKey = apiKey.trim())
                )
            }
            updateState(id) { it.copy(message = "Saved", isSuccess = null) }
        }
    }

    /** Tests a provider with the values currently shown in the UI. */
    fun testProvider(id: String, model: String, apiKey: String) {
        viewModelScope.launch {
            val configs = providerStore.currentConfigs()
            val base = configs.firstOrNull { it.id == id } ?: return@launch
            val candidate = base.copy(
                model = model.trim().ifBlank { base.model },
                apiKey = apiKey.trim().ifBlank { base.apiKey }
            )
            updateState(id) { it.copy(isBusy = true, message = null, isSuccess = null) }
            val result = aiRouter.testProvider(candidate)
            updateState(id) {
                it.copy(
                    isBusy = false,
                    isSuccess = result.isSuccess,
                    message = result.message,
                    discoveredModels = result.discoveredModels.ifEmpty { it.discoveredModels }
                )
            }
            if (result.isSuccess && candidate.apiKey.isNotBlank()) {
                if (id == "gemini") {
                    preferencesManager.setGeminiApiKey(candidate.apiKey)
                } else {
                    providerStore.saveProvider(candidate)
                }
                updateState(id) { it.copy(message = "${result.message} (saved)") }
            }
        }
    }

    /** Asks the provider which models the credential may use. */
    fun discoverModels(id: String, apiKey: String) {
        viewModelScope.launch {
            val configs = providerStore.currentConfigs()
            val base = configs.firstOrNull { it.id == id } ?: return@launch
            val candidate = base.copy(apiKey = apiKey.trim().ifBlank { base.apiKey })
            updateState(id) { it.copy(isBusy = true, message = "Discovering models…") }
            val models = aiRouter.discoverModels(candidate)
            if (models.isEmpty()) {
                updateState(id) {
                    it.copy(isBusy = false, isSuccess = false, message = "Model list unavailable for this provider.")
                }
                return@launch
            }
            val best = pickBestModel(id, models)
            updateState(id) {
                it.copy(isBusy = false, isSuccess = null, message = "${models.size} models available.", discoveredModels = models)
            }
            if (best != null) {
                providerStore.saveProvider(candidate.copy(model = best))
                updateState(id) { it.copy(message = "${models.size} models available. Selected '$best'.") }
            }
        }
    }

    fun setProviderEnabled(id: String, enabled: Boolean) {
        viewModelScope.launch {
            providerStore.setEnabled(id, enabled)
        }
    }

    private fun updateState(id: String, transform: (ProviderUiState) -> ProviderUiState) {
        _providerStates.value = _providerStates.value.toMutableMap().apply {
            this[id] = transform(this[id] ?: ProviderUiState(id = id))
        }
    }

    private fun pickBestModel(providerId: String, models: List<String>): String? {
        val blocked = listOf("embedding", "image", "tts", "audio", "whisper", "modality")
        val usable = models.filter { model ->
            blocked.none { model.lowercase().contains(it) }
        }
        if (usable.isEmpty()) return models.firstOrNull()

        val preferred = when (providerId) {
            "gemini" -> listOf("flash")
            "openai" -> listOf("gpt-5", "gpt-4.1", "gpt-4o", "o3", "o4")
            "anthropic" -> listOf("claude")
            "openrouter" -> listOf("claude", "gpt-4", "gpt-5", "gemini", "deepseek", "llama", "qwen")
            "groq" -> listOf("llama-3.3", "llama-3.1", "qwen", "deepseek")
            "deepseek" -> listOf("deepseek-reasoner", "deepseek-chat")
            "xai" -> listOf("grok")
            "mistral" -> listOf("mistral-large", "magistral", "codestral")
            "qwen" -> listOf("qwen3", "qwen-max", "qwen-plus")
            "moonshot" -> listOf("kimi")
            "zai" -> listOf("glm-4.6", "glm-4.5", "glm")
            "cohere" -> listOf("command-a", "command-r", "command")
            "together" -> listOf("llama-3.3", "llama", "qwen")
            else -> emptyList()
        }

        for (token in preferred) {
            val match = usable.firstOrNull { it.lowercase().contains(token) }
            if (match != null) return match
        }
        return usable.firstOrNull()
    }
}

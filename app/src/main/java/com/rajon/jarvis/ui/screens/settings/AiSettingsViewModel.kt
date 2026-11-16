package com.rajon.jarvis.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajon.jarvis.ai.ConnectionTestResult
import com.rajon.jarvis.ai.GeminiClient
import com.rajon.jarvis.ai.JarvisAI
import com.rajon.jarvis.data.local.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiSettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val jarvisAi: JarvisAI
) : ViewModel() {

    val apiKey: StateFlow<String> = preferencesManager.geminiApiKey
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    val modelName: String = GeminiClient.MODEL_NAME
    val apiVersion: String = GeminiClient.API_VERSION

    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting.asStateFlow()

    private val _testResult = MutableStateFlow<ConnectionTestResult?>(null)
    val testResult: StateFlow<ConnectionTestResult?> = _testResult.asStateFlow()

    private val _feedback = MutableStateFlow<String?>(null)
    val feedback: StateFlow<String?> = _feedback.asStateFlow()

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

    fun dismissFeedback() {
        _feedback.value = null
    }
}

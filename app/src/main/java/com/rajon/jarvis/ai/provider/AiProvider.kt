package com.rajon.jarvis.ai.provider

import com.rajon.jarvis.data.model.ChatMessage

/**
 * Transport style used to reach a provider.
 *
 *  - [GEMINI]             native Google Generative Language REST API
 *  - [OPENAI_COMPATIBLE]  the de-facto standard used by OpenAI, OpenRouter, Groq,
 *                          DeepSeek, xAI, Mistral, DashScope/Qwen, Moonshot/Kimi,
 *                          Z.ai/GLM, Cohere compatibility and Together AI
 *  - [ANTHROPIC]          Anthropic Messages API (Claude)
 */
enum class ProviderType {
    GEMINI,
    OPENAI_COMPATIBLE,
    ANTHROPIC
}

/** Task classes the router can optimise for. */
enum class AiTask {
    CONVERSATION,
    CODING,
    REASONING,
    WEB_RESEARCH,
    BENGALI,
    VOICE,
    AUTOMATION
}

data class ProviderConfig(
    val id: String,
    val displayName: String,
    val type: ProviderType,
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    val enabled: Boolean = true,
    val priority: Int = 100,
    val taskTags: Set<AiTask> = emptySet(),
    val notes: String = ""
) {
    /** A provider can only be used once it has a credential and a model. */
    val isConfigured: Boolean
        get() = apiKey.isNotBlank() && model.isNotBlank()
}

sealed class ProviderOutcome {
    data class Success(val text: String) : ProviderOutcome()

    data class Failure(
        val message: String,
        val httpCode: Int? = null,
        /** True when another provider is worth trying (429/5xx/timeout/network). */
        val retryable: Boolean = true
    ) : ProviderOutcome()
}

data class ProviderTestResult(
    val providerId: String,
    val displayName: String,
    val isSuccess: Boolean,
    val message: String,
    val latencyMs: Long? = null,
    val httpCode: Int? = null,
    val discoveredModels: List<String> = emptyList()
)

interface AiProvider {
    val config: ProviderConfig

    suspend fun generate(
        systemInstruction: String,
        history: List<ChatMessage>,
        userText: String
    ): ProviderOutcome

    suspend fun testConnection(): ProviderTestResult

    /** Model ids the credential is allowed to use, empty when discovery is unsupported. */
    suspend fun discoverModels(): List<String>
}

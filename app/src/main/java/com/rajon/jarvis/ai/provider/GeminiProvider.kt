package com.rajon.jarvis.ai.provider

import com.google.gson.JsonParser
import com.rajon.jarvis.ai.GeminiClient
import com.rajon.jarvis.ai.GeminiOutcome
import com.rajon.jarvis.data.model.ChatMessage

/** Wraps the existing Gemini REST client behind the common provider interface. */
class GeminiProvider(
    override val config: ProviderConfig,
    private val geminiClient: GeminiClient,
    private val transport: HttpTransport
) : AiProvider {

    override suspend fun generate(
        systemInstruction: String,
        history: List<ChatMessage>,
        userText: String
    ): ProviderOutcome {
        val outcome = geminiClient.generate(
            apiKey = config.apiKey,
            systemInstruction = systemInstruction,
            history = history,
            userText = userText
        )
        return when (outcome) {
            is GeminiOutcome.Success -> ProviderOutcome.Success(outcome.text)
            is GeminiOutcome.Failure -> ProviderOutcome.Failure(
                message = outcome.userMessage,
                httpCode = outcome.httpCode,
                retryable = ProviderErrors.isRetryable(outcome.httpCode, outcome.technicalDetail ?: "")
            )
        }
    }

    override suspend fun testConnection(): ProviderTestResult {
        if (config.apiKey.isBlank()) {
            return ProviderTestResult(
                config.id,
                config.displayName,
                false,
                "No API key stored. Add one to activate this provider."
            )
        }
        val result = geminiClient.testConnection(config.apiKey)
        return ProviderTestResult(
            providerId = config.id,
            displayName = config.displayName,
            isSuccess = result.isSuccess,
            message = result.message,
            latencyMs = result.latencyMs,
            httpCode = result.httpCode,
            discoveredModels = runCatching { discoverModels() }.getOrDefault(emptyList())
        )
    }

    override suspend fun discoverModels(): List<String> {
        if (config.apiKey.isBlank()) return emptyList()
        val response = transport.get(
            url = "${config.baseUrl.trimEnd('/')}/models?pageSize=100",
            headers = mapOf("x-goog-api-key" to config.apiKey)
        )
        if (response.code !in 200..299) return emptyList()
        return runCatching {
            val root = JsonParser.parseString(response.body).asJsonObject
            val models = root.getAsJsonArray("models") ?: return emptyList()
            models.mapNotNull { element ->
                element.asJsonObject.get("name")?.takeIf { !it.isJsonNull }?.asString
            }.map { it.removePrefix("models/") }.distinct()
        }.getOrElse { emptyList() }
    }
}

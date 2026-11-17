package com.rajon.jarvis.ai.provider

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.rajon.jarvis.data.model.ChatMessage
import com.rajon.jarvis.data.model.MessageRole

/**
 * Talks to any OpenAI-compatible `/chat/completions` endpoint.
 *
 * One implementation covers OpenAI, OpenRouter, Groq, DeepSeek, xAI, Mistral,
 * DashScope (Qwen), Moonshot (Kimi), Z.ai (GLM), Cohere compatibility and Together AI.
 */
class OpenAiCompatibleProvider(
    override val config: ProviderConfig,
    private val transport: HttpTransport
) : AiProvider {

    override suspend fun generate(
        systemInstruction: String,
        history: List<ChatMessage>,
        userText: String
    ): ProviderOutcome {
        if (config.apiKey.isBlank()) {
            return ProviderOutcome.Failure(
                "No API key stored for ${config.displayName}.",
                httpCode = null,
                retryable = false
            )
        }
        if (config.model.isBlank()) {
            return ProviderOutcome.Failure(
                "No model selected for ${config.displayName}. Open the provider in AI Settings and pick one.",
                httpCode = null,
                retryable = false
            )
        }

        val messages = JsonArray()
        messages.add(
            JsonObject().apply {
                addProperty("role", "system")
                addProperty("content", systemInstruction)
            }
        )
        history.takeLast(12).forEach { message ->
            if (message.role == MessageRole.SYSTEM) return@forEach
            messages.add(
                JsonObject().apply {
                    addProperty("role", if (message.role == MessageRole.USER) "user" else "assistant")
                    addProperty("content", message.content)
                }
            )
        }
        messages.add(
            JsonObject().apply {
                addProperty("role", "user")
                addProperty("content", userText)
            }
        )

        val payload = JsonObject().apply {
            addProperty("model", config.model)
            add("messages", messages)
            addProperty("max_tokens", 1024)
            addProperty("stream", false)
        }

        return try {
            val response = transport.postJson(
                url = "${config.baseUrl.trimEnd('/')}/chat/completions",
                headers = mapOf(
                    "Content-Type" to "application/json",
                    "Authorization" to "Bearer ${config.apiKey}"
                ),
                body = payload.toString()
            )
            if (response.code in 200..299) {
                val text = extractText(response.body)
                if (text.isNullOrBlank()) {
                    ProviderOutcome.Failure(
                        "${config.displayName} returned an empty answer.",
                        response.code,
                        retryable = true
                    )
                } else {
                    ProviderOutcome.Success(text)
                }
            } else {
                ProviderOutcome.Failure(
                    ProviderErrors.message(
                        response.code,
                        response.body,
                        config.displayName,
                        config.model
                    ),
                    response.code,
                    ProviderErrors.isRetryable(response.code, response.body)
                )
            }
        } catch (cancel: kotlinx.coroutines.CancellationException) {
            throw cancel
        } catch (e: Exception) {
            ProviderOutcome.Failure(
                "Network error while contacting ${config.displayName}: ${e.message ?: "unknown"}",
                httpCode = null,
                retryable = true
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

        val startedAt = System.currentTimeMillis()
        val models = try {
            discoverModels()
        } catch (e: Exception) {
            emptyList()
        }

        return try {
            val payload = JsonObject().apply {
                addProperty("model", config.model.ifBlank { models.firstOrNull() ?: "" })
                add(
                    "messages",
                    JsonArray().apply {
                        add(
                            JsonObject().apply {
                                addProperty("role", "user")
                                addProperty("content", "Reply with exactly: OK")
                            }
                        )
                    }
                )
                addProperty("max_tokens", 16)
            }
            val response = transport.postJson(
                url = "${config.baseUrl.trimEnd('/')}/chat/completions",
                headers = mapOf(
                    "Content-Type" to "application/json",
                    "Authorization" to "Bearer ${config.apiKey}"
                ),
                body = payload.toString()
            )
            val latency = System.currentTimeMillis() - startedAt
            if (response.code in 200..299) {
                ProviderTestResult(
                    providerId = config.id,
                    displayName = config.displayName,
                    isSuccess = true,
                    message = "Connected to ${config.displayName} - live reply received.",
                    latencyMs = latency,
                    httpCode = response.code,
                    discoveredModels = models
                )
            } else {
                ProviderTestResult(
                    providerId = config.id,
                    displayName = config.displayName,
                    isSuccess = false,
                    message = ProviderErrors.message(
                        response.code,
                        response.body,
                        config.displayName,
                        config.model
                    ),
                    latencyMs = latency,
                    httpCode = response.code,
                    discoveredModels = models
                )
            }
        } catch (cancel: kotlinx.coroutines.CancellationException) {
            throw cancel
        } catch (e: Exception) {
            ProviderTestResult(
                providerId = config.id,
                displayName = config.displayName,
                isSuccess = false,
                message = "Network error while contacting ${config.displayName}: ${e.message ?: "unknown"}",
                latencyMs = System.currentTimeMillis() - startedAt,
                discoveredModels = models
            )
        }
    }

    override suspend fun discoverModels(): List<String> {
        if (config.apiKey.isBlank()) return emptyList()
        val response = transport.get(
            url = "${config.baseUrl.trimEnd('/')}/models",
            headers = mapOf("Authorization" to "Bearer ${config.apiKey}")
        )
        if (response.code !in 200..299) return emptyList()
        return runCatching {
            val root = com.google.gson.JsonParser.parseString(response.body).asJsonObject
            val array = root.getAsJsonArray("data")
                ?: root.getAsJsonArray("models")
                ?: return emptyList()
            array.mapNotNull { element ->
                val obj = element.asJsonObject
                obj.get("id")?.takeIf { !it.isJsonNull }?.asString
                    ?: obj.get("name")?.takeIf { !it.isJsonNull }?.asString
            }.distinct()
        }.getOrElse { emptyList() }
    }

    private fun extractText(body: String): String? = runCatching {
        val root = com.google.gson.JsonParser.parseString(body).asJsonObject
        val choices = root.getAsJsonArray("choices") ?: return null
        if (choices.size() == 0) return null
        val message = choices.get(0).asJsonObject.getAsJsonObject("message") ?: return null
        val content = message.get("content")
        if (content == null || content.isJsonNull) {
            // Some providers return an array of content parts.
            val parts = message.getAsJsonArray("content") ?: return null
            parts.joinToString("") { part ->
                part.asJsonObject.get("text")?.takeIf { !it.isJsonNull }?.asString ?: ""
            }.takeIf { it.isNotBlank() }
        } else {
            content.asString
        }
    }.getOrNull()
}

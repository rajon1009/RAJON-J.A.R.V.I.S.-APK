package com.rajon.jarvis.ai.provider

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.rajon.jarvis.data.model.ChatMessage
import com.rajon.jarvis.data.model.MessageRole

/** Anthropic Messages API (Claude). */
class AnthropicProvider(
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
            add("model", config.model)
            add("max_tokens", 1024)
            add("system", systemInstruction)
            add("messages", messages)
        }

        return try {
            val response = transport.postJson(
                url = "${config.baseUrl.trimEnd('/')}/messages",
                headers = headers(),
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
        val models = runCatching { discoverModels() }.getOrDefault(emptyList())

        return try {
            val payload = JsonObject().apply {
                add("model", config.model.ifBlank { models.firstOrNull() ?: "" })
                add("max_tokens", 16)
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
            }
            val response = transport.postJson(
                url = "${config.baseUrl.trimEnd('/')}/messages",
                headers = headers(),
                body = payload.toString()
            )
            val latency = System.currentTimeMillis() - startedAt
            val success = response.code in 200..299 && !extractText(response.body).isNullOrBlank()
            ProviderTestResult(
                providerId = config.id,
                displayName = config.displayName,
                isSuccess = success,
                message = if (success) {
                    "Connected to ${config.displayName} - live reply received."
                } else {
                    ProviderErrors.message(
                        response.code,
                        response.body,
                        config.displayName,
                        config.model
                    )
                },
                latencyMs = latency,
                httpCode = response.code,
                discoveredModels = models
            )
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
            headers = headers()
        )
        if (response.code !in 200..299) return emptyList()
        return runCatching {
            val root = JsonParser.parseString(response.body).asJsonObject
            val data = root.getAsJsonArray("data") ?: return emptyList()
            data.mapNotNull { element ->
                element.asJsonObject.get("id")?.takeIf { !it.isJsonNull }?.asString
            }.distinct()
        }.getOrElse { emptyList() }
    }

    private fun headers(): Map<String, String> = mapOf(
        "Content-Type" to "application/json",
        "x-api-key" to config.apiKey,
        "anthropic-version" to "2023-06-01"
    )

    private fun extractText(body: String): String? = runCatching {
        val root = JsonParser.parseString(body).asJsonObject
        val content = root.getAsJsonArray("content") ?: return null
        content.joinToString("") { element ->
            val part = element.asJsonObject
            part.get("text")?.takeIf { !it.isJsonNull }?.asString ?: ""
        }.takeIf { it.isNotBlank() }
    }.getOrNull()
}

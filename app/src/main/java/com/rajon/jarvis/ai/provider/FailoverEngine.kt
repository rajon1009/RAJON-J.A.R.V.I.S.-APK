package com.rajon.jarvis.ai.provider

import com.rajon.jarvis.data.model.ChatMessage

data class ProviderAttempt(
    val providerId: String,
    val displayName: String,
    val model: String,
    val success: Boolean,
    val message: String? = null,
    val httpCode: Int? = null
)

sealed class FailoverOutcome {
    data class Success(
        val text: String,
        val providerId: String,
        val displayName: String,
        val model: String,
        val attempts: List<ProviderAttempt>
    ) : FailoverOutcome()

    data class Failure(
        val message: String,
        val attempts: List<ProviderAttempt>
    ) : FailoverOutcome()
}

/**
 * Tries providers one after another until one answers.
 *
 * A provider is handed over when it returns 429 (quota / rate limit), any 5xx,
 * a timeout, a dropped connection or an empty 404 from the network. The same
 * conversation history is handed to the next provider so the conversation stays
 * continuous across a switch.
 */
class FailoverEngine(
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val cooldownMillis: Long = 60_000L
) {

    private val cooldowns = mutableMapOf<String, Long>()

    fun isCoolingDown(providerId: String): Boolean =
        (cooldowns[providerId] ?: 0L) > clock()

    private fun startCooldown(providerId: String) {
        cooldowns[providerId] = clock() + cooldownMillis
    }

    suspend fun execute(
        providers: List<AiProvider>,
        task: AiTask,
        systemInstruction: String,
        history: List<ChatMessage>,
        userText: String
    ): FailoverOutcome {
        val attempts = mutableListOf<ProviderAttempt>()
        val ordered = order(providers, task)

        if (ordered.isEmpty()) {
            return FailoverOutcome.Failure(
                "No AI provider is configured. Add an API key in More > AI Settings.",
                attempts
            )
        }

        for (provider in ordered) {
            val config = provider.config
            if (isCoolingDown(config.id)) {
                attempts.add(
                    ProviderAttempt(
                        providerId = config.id,
                        displayName = config.displayName,
                        model = config.model,
                        success = false,
                        message = "Skipped, cooling down after the last failure."
                    )
                )
                continue
            }

            val outcome = provider.generate(systemInstruction, history, userText)
            when (outcome) {
                is ProviderOutcome.Success -> {
                    attempts.add(
                        ProviderAttempt(
                            providerId = config.id,
                            displayName = config.displayName,
                            model = config.model,
                            success = true
                        )
                    )
                    return FailoverOutcome.Success(
                        text = outcome.text,
                        providerId = config.id,
                        displayName = config.displayName,
                        model = config.model,
                        attempts = attempts
                    )
                }

                is ProviderOutcome.Failure -> {
                    attempts.add(
                        ProviderAttempt(
                            providerId = config.id,
                            displayName = config.displayName,
                            model = config.model,
                            success = false,
                            message = outcome.message,
                            httpCode = outcome.httpCode
                        )
                    )
                    if (outcome.retryable) {
                        startCooldown(config.id)
                    }
                }
            }
        }

        val summary = attempts
            .filter { !it.success && it.message != null }
            .joinToString(" | ") { "${it.displayName}: ${it.message}" }

        return FailoverOutcome.Failure(
            message = summary.ifBlank { "Every AI provider failed to answer." },
            attempts = attempts
        )
    }

    private fun order(providers: List<AiProvider>, task: AiTask): List<AiProvider> =
        providers.sortedWith(
            compareBy<AiProvider>(
                { if (it.config.taskTags.contains(task)) 0 else 1 },
                { it.config.priority }
            )
        )
}

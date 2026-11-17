package com.rajon.jarvis.ai.provider

import com.rajon.jarvis.data.model.ChatMessage
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Entry point used by the assistant: resolves the configured providers, picks the
 * best one for the task and transparently hands over to the next provider when a
 * provider is exhausted, failing or unavailable.
 */
@Singleton
class AiRouter @Inject constructor(
    private val providerStore: ProviderStore,
    private val providerFactory: ProviderFactory,
    private val failoverEngine: FailoverEngine
) {

    /** Providers that are enabled and have both a credential and a model. */
    suspend fun activeProviders(): List<AiProvider> {
        val configs = providerStore.configs.first()
        return configs
            .filter { it.enabled && it.isConfigured }
            .map { providerFactory.create(it) }
    }

    suspend fun allConfigs(): List<ProviderConfig> = providerStore.configs.first()

    suspend fun generate(
        systemInstruction: String,
        history: List<ChatMessage>,
        userText: String,
        task: AiTask
    ): FailoverOutcome = failoverEngine.execute(
        providers = activeProviders(),
        task = task,
        systemInstruction = systemInstruction,
        history = history,
        userText = userText
    )

    suspend fun testProvider(config: ProviderConfig): ProviderTestResult =
        providerFactory.create(config).testConnection()

    suspend fun discoverModels(config: ProviderConfig): List<String> =
        providerFactory.create(config).discoverModels()

    fun isCoolingDown(providerId: String): Boolean = failoverEngine.isCoolingDown(providerId)
}

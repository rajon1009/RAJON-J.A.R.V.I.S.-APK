package com.rajon.jarvis.ai.provider

import com.rajon.jarvis.ai.GeminiClient
import javax.inject.Inject
import javax.inject.Singleton

/** Creates the concrete provider implementation for a stored configuration. */
@Singleton
class ProviderFactory @Inject constructor(
    private val geminiClient: GeminiClient,
    private val transport: HttpTransport
) {
    fun create(config: ProviderConfig): AiProvider = when (config.type) {
        ProviderType.GEMINI -> GeminiProvider(config, geminiClient, transport)
        ProviderType.OPENAI_COMPATIBLE -> OpenAiCompatibleProvider(config, transport)
        ProviderType.ANTHROPIC -> AnthropicProvider(config, transport)
    }
}

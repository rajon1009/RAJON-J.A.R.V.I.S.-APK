package com.rajon.jarvis.ai.provider

/**
 * Built-in provider catalogue.
 *
 * Every entry is a well-known public REST endpoint; no credential is bundled here.
 * A provider only becomes active once the user stores a key (and a model id, either
 * typed in or discovered from the provider's own /models endpoint).
 *
 * The priority value orders the automatic failover chain: lower runs first.
 */
object ProviderPresets {

    val all: List<ProviderConfig> = listOf(
        ProviderConfig(
            id = "gemini",
            displayName = "Google Gemini",
            type = ProviderType.GEMINI,
            baseUrl = "https://generativelanguage.googleapis.com/v1beta",
            apiKey = "",
            model = "gemini-3.8-flash",
            priority = 10,
            taskTags = setOf(AiTask.CONVERSATION, AiTask.BENGALI, AiTask.REASONING, AiTask.VOICE, AiTask.AUTOMATION),
            notes = "Native Gemini API. Works with AQ... auth keys and AIza... keys."
        ),
        ProviderConfig(
            id = "openrouter",
            displayName = "OpenRouter (Claude + GPT + Gemini + Llama + Qwen + DeepSeek + Grok)",
            type = ProviderType.OPENAI_COMPATIBLE,
            baseUrl = "https://openrouter.ai/api/v1",
            apiKey = "",
            model = "",
            priority = 20,
            taskTags = setOf(AiTask.CONVERSATION, AiTask.CODING, AiTask.REASONING, AiTask.WEB_RESEARCH, AiTask.BENGALI),
            notes = "One key unlocks many vendors through one OpenAI-compatible endpoint."
        ),
        ProviderConfig(
            id = "anthropic",
            displayName = "Anthropic Claude",
            type = ProviderType.ANTHROPIC,
            baseUrl = "https://api.anthropic.com/v1",
            apiKey = "",
            model = "",
            priority = 25,
            taskTags = setOf(AiTask.CODING, AiTask.REASONING, AiTask.WEB_RESEARCH),
            notes = "Anthropic Messages API (x-api-key + anthropic-version)."
        ),
        ProviderConfig(
            id = "openai",
            displayName = "OpenAI",
            type = ProviderType.OPENAI_COMPATIBLE,
            baseUrl = "https://api.openai.com/v1",
            apiKey = "",
            model = "",
            priority = 30,
            taskTags = setOf(AiTask.CODING, AiTask.REASONING, AiTask.WEB_RESEARCH),
            notes = "GPT family via /v1/chat/completions."
        ),
        ProviderConfig(
            id = "groq",
            displayName = "Groq (Llama, Qwen, DeepSeek)",
            type = ProviderType.OPENAI_COMPATIBLE,
            baseUrl = "https://api.groq.com/openai/v1",
            apiKey = "",
            model = "",
            priority = 40,
            taskTags = setOf(AiTask.CONVERSATION, AiTask.VOICE, AiTask.AUTOMATION),
            notes = "Very low latency inference, good for spoken replies."
        ),
        ProviderConfig(
            id = "deepseek",
            displayName = "DeepSeek",
            type = ProviderType.OPENAI_COMPATIBLE,
            baseUrl = "https://api.deepseek.com/v1",
            apiKey = "",
            model = "",
            priority = 45,
            taskTags = setOf(AiTask.CODING, AiTask.REASONING),
            notes = "Strong coding/reasoning models."
        ),
        ProviderConfig(
            id = "xai",
            displayName = "xAI Grok",
            type = ProviderType.OPENAI_COMPATIBLE,
            baseUrl = "https://api.x.ai/v1",
            apiKey = "",
            model = "",
            priority = 50,
            taskTags = setOf(AiTask.CONVERSATION, AiTask.WEB_RESEARCH, AiTask.REASONING),
            notes = "Grok family via /v1/chat/completions."
        ),
        ProviderConfig(
            id = "mistral",
            displayName = "Mistral AI",
            type = ProviderType.OPENAI_COMPATIBLE,
            baseUrl = "https://api.mistral.ai/v1",
            apiKey = "",
            model = "",
            priority = 55,
            taskTags = setOf(AiTask.CONVERSATION, AiTask.CODING),
            notes = "Mistral Large / Codestral etc."
        ),
        ProviderConfig(
            id = "qwen",
            displayName = "Alibaba Qwen (DashScope)",
            type = ProviderType.OPENAI_COMPATIBLE,
            baseUrl = "https://dashscope-intl.aliyuncs.com/compatible-mode/v1",
            apiKey = "",
            model = "",
            priority = 60,
            taskTags = setOf(AiTask.BENGALI, AiTask.CODING, AiTask.CONVERSATION),
            notes = "Strong multilingual/Bengali support in compatible mode."
        ),
        ProviderConfig(
            id = "moonshot",
            displayName = "Moonshot Kimi",
            type = ProviderType.OPENAI_COMPATIBLE,
            baseUrl = "https://api.moonshot.ai/v1",
            apiKey = "",
            model = "",
            priority = 65,
            taskTags = setOf(AiTask.CODING, AiTask.REASONING),
            notes = "Kimi long-context models."
        ),
        ProviderConfig(
            id = "zai",
            displayName = "Z.ai GLM",
            type = ProviderType.OPENAI_COMPATIBLE,
            baseUrl = "https://api.z.ai/api/paas/v4",
            apiKey = "",
            model = "",
            priority = 70,
            taskTags = setOf(AiTask.CODING, AiTask.BENGALI, AiTask.CONVERSATION),
            notes = "GLM family via the OpenAI-compatible route."
        ),
        ProviderConfig(
            id = "cohere",
            displayName = "Cohere Command",
            type = ProviderType.OPENAI_COMPATIBLE,
            baseUrl = "https://api.cohere.com/compatibility/v1",
            apiKey = "",
            model = "",
            priority = 75,
            taskTags = setOf(AiTask.WEB_RESEARCH, AiTask.REASONING),
            notes = "Command family via Cohere's compatibility API."
        ),
        ProviderConfig(
            id = "together",
            displayName = "Together AI (Llama, Muse, Qwen)",
            type = ProviderType.OPENAI_COMPATIBLE,
            baseUrl = "https://api.together.xyz/v1",
            apiKey = "",
            model = "",
            priority = 80,
            taskTags = setOf(AiTask.CODING, AiTask.CONVERSATION),
            notes = "Open-weight models (Llama, Muse, Qwen, DeepSeek)."
        )
    )

    fun byId(id: String): ProviderConfig? = all.firstOrNull { it.id == id }
}

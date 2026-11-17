package com.rajon.jarvis.ai.provider

/**
 * Lightweight, offline task classifier. It decides which provider specialism
 * should be preferred for a request (coding model for code, research model for
 * research, and so on) without spending another API call.
 */
object AiTaskClassifier {

    private val codingWords = listOf(
        "code", "function", "bug", "debug", "stacktrace", "stack trace", "compile",
        "exception", "null pointer", "refactor", "unit test", "algorithm", "regex",
        "kotlin", "java", "python", "javascript", "typescript", "sql", "api", "sdk",
        "class", "function", "script", "program", "gradle", "build failed"
    )

    private val researchWords = listOf(
        "research", "search the web", "latest news", "news about", "compare prices",
        "price of", "review of", "wikipedia", "who won", "weather in", "latest version",
        "documentation for", "how does it work today"
    )

    private val reasoningWords = listOf(
        "why", "explain", "analyze", "analyse", "step by step", "strategy", "plan",
        "compare", "difference between", "reason", "prove", "evaluate", "trade-off",
        "tradeoff", "root cause", "design a"
    )

    private val automationWords = listOf(
        "open ", "launch ", "set a reminder", "remind me", "play ", "install ",
        "turn on", "turn off", "send a message", "search youtube", "check battery",
        "check storage", "battery", "storage", "whatsapp"
    )

    fun classify(text: String, fromVoice: Boolean = false): AiTask {
        val lower = text.lowercase()
        if (containsBengali(text)) return AiTask.BENGALI
        if (fromVoice) return AiTask.VOICE
        return when {
            lower.containsAny(codingWords) -> AiTask.CODING
            lower.containsAny(researchWords) -> AiTask.WEB_RESEARCH
            lower.containsAny(reasoningWords) -> AiTask.REASONING
            lower.containsAny(automationWords) -> AiTask.AUTOMATION
            else -> AiTask.CONVERSATION
        }
    }

    private fun String.containsAny(words: List<String>): Boolean =
        words.any { this.contains(it) }

    /** Detects Bengali script characters (U+0980 - U+09FF). */
    fun containsBengali(text: String): Boolean =
        text.any { it.code in 0x0980..0x09FF }
}

package com.rajon.jarvis.ai.provider

import com.rajon.jarvis.data.model.ChatMessage
import com.rajon.jarvis.data.model.MessageRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Failover behaviour of the multi-provider router.
 * Pure JVM tests: no Android framework, no network.
 */
class FailoverEngineTest {

    private fun config(
        id: String,
        priority: Int = 10,
        tasks: Set<AiTask> = emptySet()
    ) = ProviderConfig(
        id = id,
        displayName = id.replaceFirstChar { it.uppercase() },
        type = ProviderType.OPENAI_COMPATIBLE,
        baseUrl = "https://example.invalid/v1",
        apiKey = "key-$id",
        model = "model-$id",
        priority = priority,
        taskTags = tasks
    )

    private class FakeProvider(
        override val config: ProviderConfig,
        private val outcomes: MutableList<ProviderOutcome>
    ) : AiProvider {
        val receivedHistory = mutableListOf<List<ChatMessage>>()
        val receivedSystem = mutableListOf<String>()
        var calls = 0

        override suspend fun generate(
            systemInstruction: String,
            history: List<ChatMessage>,
            userText: String
        ): ProviderOutcome {
            calls++
            receivedSystem.add(systemInstruction)
            receivedHistory.add(history)
            return if (outcomes.size == 1) {
                outcomes.first()
            } else {
                outcomes.removeAt(0)
            }
        }

        override suspend fun testConnection(): ProviderTestResult =
            ProviderTestResult(config.id, config.displayName, true, "ok")

        override suspend fun discoverModels(): List<String> = emptyList()
    }

    private val history = listOf(
        ChatMessage(content = "hello", role = MessageRole.USER),
        ChatMessage(content = "hi", role = MessageRole.ASSISTANT)
    )

    @Test
    fun `first working provider answers`() = runBlocking {
        val first = FakeProvider(config("alpha"), mutableListOf(ProviderOutcome.Success("alpha answer")))
        val second = FakeProvider(config("beta"), mutableListOf(ProviderOutcome.Success("beta answer")))

        val outcome = FailoverEngine().execute(
            providers = listOf(first, second),
            task = AiTask.CONVERSATION,
            systemInstruction = "system prompt",
            history = history,
            userText = "hi"
        )

        assertTrue(outcome is FailoverOutcome.Success)
        outcome as FailoverOutcome.Success
        assertEquals("alpha answer", outcome.text)
        assertEquals("alpha", outcome.providerId)
        assertEquals(1, outcome.attempts.size)
        assertEquals(1, second.calls)
    }

    @Test
    fun `quota error switches to the next provider`() = runBlocking {
        val quota = FakeProvider(
            config("alpha"),
            mutableListOf(
                ProviderOutcome.Failure("quota exceeded (429)", 429, retryable = true),
                ProviderOutcome.Success("beta answer")
            )
        )
        val beta = FakeProvider(config("beta"), mutableListOf(ProviderOutcome.Success("beta answer")))

        val outcome = FailoverEngine().execute(
            providers = listOf(quota, beta),
            task = AiTask.CONVERSATION,
            systemInstruction = "system prompt",
            history = history,
            userText = "hi"
        )

        assertTrue(outcome is FailoverOutcome.Success)
        outcome as FailoverOutcome.Success
        assertEquals("beta", outcome.providerId)
        assertEquals(2, outcome.attempts.size)
        assertEquals(false, outcome.attempts[0].success)
        assertEquals(429, outcome.attempts[0].httpCode)
    }

    @Test
    fun `timeout switches to the next provider and keeps the context`() = runBlocking {
        val timeout = FakeProvider(
            config("alpha"),
            mutableListOf(ProviderOutcome.Failure("Network error", null, retryable = true))
        )
        val beta = FakeProvider(config("beta"), mutableListOf(ProviderOutcome.Success("beta answer")))

        val outcome = FailoverEngine().execute(
            providers = listOf(timeout, beta),
            task = AiTask.CONVERSATION,
            systemInstruction = "system prompt",
            history = history,
            userText = "hi"
        )

        assertTrue(outcome is FailoverOutcome.Success)
        outcome as FailoverOutcome.Success
        assertEquals("beta", outcome.providerId)
        // The second provider must receive the same conversation history.
        assertEquals(history, beta.receivedHistory.first())
        assertEquals("system prompt", beta.receivedSystem.first())
    }

    @Test
    fun `all providers failing produces an aggregated error`() = runBlocking {
        val alpha = FakeProvider(
            config("alpha"),
            mutableListOf(ProviderOutcome.Failure("alpha broke (429)", 429, retryable = true))
        )
        val beta = FakeProvider(
            config("beta"),
            mutableListOf(ProviderOutcome.Failure("beta broke (503)", 503, retryable = true))
        )

        val outcome = FailoverEngine().execute(
            providers = listOf(alpha, beta),
            task = AiTask.CONVERSATION,
            systemInstruction = "system prompt",
            history = history,
            userText = "hi"
        )

        assertTrue(outcome is FailoverOutcome.Failure)
        outcome as FailoverOutcome.Failure
        assertTrue(outcome.message.contains("alpha broke (429)"))
        assertTrue(outcome.message.contains("beta broke (503)"))
    }

    @Test
    fun `a failed provider is skipped while it cools down`() = runBlocking {
        var now = 1_000L
        val engine = FailoverEngine(clock = { now }, cooldownMillis = 60_000L)

        val alpha = FakeProvider(
            config("alpha"),
            mutableListOf(
                ProviderOutcome.Failure("quota", 429, retryable = true),
                ProviderOutcome.Success("alpha recovered")
            )
        )
        val beta = FakeProvider(config("beta"), mutableListOf(ProviderOutcome.Success("beta answer")))

        val first = engine.execute(listOf(alpha, beta), AiTask.CONVERSATION, "s", history, "hi")
        assertTrue(first is FailoverOutcome.Success)

        now += 1_000L
        val second = engine.execute(listOf(alpha, beta), AiTask.CONVERSATION, "s", history, "hi")
        assertTrue(second is FailoverOutcome.Success)
        second as FailoverOutcome.Success
        assertEquals("beta", second.providerId)
        assertTrue(second.attempts.first().message?.contains("cooling down") == true)
        // The provider that failed is not called again during the cooldown.
        assertEquals(1, alpha.calls)
    }

    @Test
    fun `task specialists are preferred over generic providers`() = runBlocking {
        val generalist = FakeProvider(config("generalist", priority = 5), mutableListOf(ProviderOutcome.Success("generic")))
        val specialist = FakeProvider(
            config("coder", priority = 50, tasks = setOf(AiTask.CODING)),
            mutableListOf(ProviderOutcome.Success("code answer"))
        )

        val outcome = FailoverEngine().execute(
            providers = listOf(generalist, specialist),
            task = AiTask.CODING,
            systemInstruction = "s",
            history = history,
            userText = "fix this bug"
        )

        assertTrue(outcome is FailoverOutcome.Success)
        outcome as FailoverOutcome.Success
        assertEquals("coder", outcome.providerId)
    }

    @Test
    fun `no provider returns a clear configuration error`() = runBlocking {
        val outcome = FailoverEngine().execute(
            providers = emptyList(),
            task = AiTask.CONVERSATION,
            systemInstruction = "s",
            history = history,
            userText = "hi"
        )

        assertTrue(outcome is FailoverOutcome.Failure)
        outcome as FailoverOutcome.Failure
        assertTrue(outcome.message.contains("No AI provider is configured"))
    }
}

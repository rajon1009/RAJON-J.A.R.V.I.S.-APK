package com.rajon.jarvis.ai

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.rajon.jarvis.data.model.ChatMessage
import com.rajon.jarvis.data.model.MessageRole
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.Dns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.Inet4Address
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

sealed class GeminiOutcome {
    data class Success(val text: String) : GeminiOutcome()

    data class Failure(
        val userMessage: String,
        val httpCode: Int? = null,
        val technicalDetail: String? = null
    ) : GeminiOutcome()
}

data class ConnectionTestResult(
    val isSuccess: Boolean,
    val message: String,
    val latencyMs: Long? = null,
    val httpCode: Int? = null
)

/**
 * Minimal REST transport for the Gemini API.
 *
 * Why REST instead of the legacy `generativeai:0.9.0` SDK:
 *  - the SDK is frozen at 0.9.0 (published 2024-07-02) and cannot set thinking levels;
 *  - REST lets us enforce connect/read timeouts, retry once on transport failure,
 *    surface the real HTTP status, and keep the API key out of URLs/logs.
 */
@Singleton
class GeminiClient @Inject constructor() {

    companion object {
        /** Latest Stable Flash model verified against the Gemini model catalogue. */
        const val MODEL_NAME = "gemini-3.8-flash"
        const val API_VERSION = "v1beta"

        private const val BASE_URL = "https://generativelanguage.googleapis.com/$API_VERSION"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    // The phone's 5G path advertises IPv6 but cannot actually reach Gemini over
    // it (curl -6 fails instantly). Prefer IPv4 when the host resolves to both.
    private val ipv4FirstDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            val resolved = Dns.SYSTEM.lookup(hostname)
            val ipv4 = resolved.filterIsInstance<Inet4Address>()
            return ipv4.ifEmpty { resolved }
        }
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        // HTTP/2 POSTs to generativelanguage.googleapis.com can be answered with an
        // empty 404 by some mobile networks / middleboxes, while GETs and HTTP/1.1
        // requests succeed. Force HTTP/1.1 so chat requests follow the reliable path.
        .protocols(listOf(Protocol.HTTP_1_1))
        .dns(ipv4FirstDns)
        // Carrier NAT silently drops idle keep-alive sockets, which makes OkHttp
        // wait for the full read timeout on a reused connection. A zero-sized pool
        // matches curl's fresh-connection behaviour.
        .connectionPool(ConnectionPool(0, 1, TimeUnit.NANOSECONDS))
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun generate(
        apiKey: String,
        systemInstruction: String,
        history: List<ChatMessage>,
        userText: String
    ): GeminiOutcome {
        if (apiKey.isBlank()) {
            return GeminiOutcome.Failure("No API key configured. Add your Gemini API key in AI Settings.")
        }

        val url = "$BASE_URL/models/$MODEL_NAME:generateContent"
        val initialRequest = Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", apiKey)
            .post(buildRequestBody(systemInstruction, history, userText, includeThinking = true).toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return try {
            var (code, body) = executeWithRetry(initialRequest)

            // Older projects may reject the thinking config. Retry once without it.
            if (code == 400 && body.contains("thinking", ignoreCase = true)) {
                val retryRequest = initialRequest.newBuilder()
                    .post(buildRequestBody(systemInstruction, history, userText, includeThinking = false).toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                val second = executeWithRetry(retryRequest)
                code = second.first
                body = second.second
            }

            if (code in 200..299) {
                val text = extractText(body)
                if (text.isNullOrBlank()) {
                    GeminiOutcome.Failure(
                        "Gemini returned an empty response. It may have been blocked by a safety filter.",
                        code
                    )
                } else {
                    GeminiOutcome.Success(text)
                }
            } else {
                GeminiOutcome.Failure(
                    userMessage = httpErrorMessage(code, body),
                    httpCode = code,
                    technicalDetail = serverMessage(body)
                )
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (e: Exception) {
            GeminiOutcome.Failure(
                userMessage = AiErrorMapper.fromException(e),
                technicalDetail = e.message
            )
        }
    }

    suspend fun testConnection(apiKey: String): ConnectionTestResult {
        if (apiKey.isBlank()) {
            return ConnectionTestResult(false, "No API key saved. Add your Gemini API key first.")
        }

        val startedAt = System.currentTimeMillis()
        // A real generateContent call, not just model metadata: GET can succeed
        // while POST is blocked by the network, so this is the honest check.
        val payload = JsonObject().apply {
            add("contents", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("role", "user")
                    add("parts", JsonArray().apply {
                        add(JsonObject().apply { addProperty("text", "Reply with exactly: OK") })
                    })
                })
            })
            add("generationConfig", JsonObject().apply {
                addProperty("maxOutputTokens", 16)
            })
        }
        val request = Request.Builder()
            .url("$BASE_URL/models/$MODEL_NAME:generateContent")
            .addHeader("x-goog-api-key", apiKey)
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return try {
            val (code, body) = executeWithRetry(request)
            val latency = System.currentTimeMillis() - startedAt
            if (code in 200..299) {
                val reply = extractText(body)
                if (reply.isNullOrBlank()) {
                    ConnectionTestResult(
                        isSuccess = false,
                        message = "Connected, but $MODEL_NAME returned no text (possibly a safety filter).",
                        latencyMs = latency,
                        httpCode = code
                    )
                } else {
                    ConnectionTestResult(
                        isSuccess = true,
                        message = "Connected to $MODEL_NAME - live reply received.",
                        latencyMs = latency,
                        httpCode = code
                    )
                }
            } else {
                ConnectionTestResult(
                    isSuccess = false,
                    message = httpErrorMessage(code, body),
                    latencyMs = latency,
                    httpCode = code
                )
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (e: Exception) {
            ConnectionTestResult(
                isSuccess = false,
                message = AiErrorMapper.fromException(e),
                latencyMs = System.currentTimeMillis() - startedAt
            )
        }
    }

    private suspend fun executeWithRetry(request: Request): Pair<Int, String> = withContext(Dispatchers.IO) {
        var last: Pair<Int, String>? = null
        for (attempt in 0..1) {
            try {
                val result = execute(request)
                last = result
                val (code, body) = result
                // Retry transient edge/proxy failures: an empty 404 is not a real
                // Gemini response, and 502/503/504 are usually momentary.
                val transient = (code == 404 && body.isBlank()) || code == 502 || code == 503 || code == 504
                if (!transient || attempt == 1) return@withContext result
                delay(if (code == 503) 2500 else 1200)
            } catch (e: IOException) {
                // One retry helps with pooled-connection resets and brief radio handovers.
                if (attempt == 1) throw e
                delay(700)
            }
        }
        last ?: throw IOException("No response from Gemini")
    }

    private fun execute(request: Request): Pair<Int, String> {
        client.newCall(request).execute().use { response ->
            return response.code to (response.body?.string() ?: "")
        }
    }

    private fun buildRequestBody(
        systemInstruction: String,
        history: List<ChatMessage>,
        userText: String,
        includeThinking: Boolean
    ): String {
        val root = JsonObject()

        val system = JsonObject().apply {
            add("parts", JsonArray().apply {
                add(JsonObject().apply { addProperty("text", systemInstruction) })
            })
        }
        root.add("systemInstruction", system)

        val contents = JsonArray()

        history.takeLast(10).forEach { message ->
            if (message.role == MessageRole.SYSTEM) return@forEach
            contents.add(
                JsonObject().apply {
                    addProperty("role", if (message.role == MessageRole.USER) "user" else "model")
                    add("parts", JsonArray().apply {
                        add(JsonObject().apply { addProperty("text", message.content) })
                    })
                }
            )
        }

        contents.add(
            JsonObject().apply {
                addProperty("role", "user")
                add("parts", JsonArray().apply {
                    add(JsonObject().apply { addProperty("text", userText) })
                })
            }
        )
        root.add("contents", contents)

        val generationConfig = JsonObject().apply {
            addProperty("maxOutputTokens", 1024)
            if (includeThinking) {
                add("thinkingConfig", JsonObject().apply {
                    addProperty("thinkingLevel", "low")
                })
            }
        }
        root.add("generationConfig", generationConfig)

        return root.toString()
    }

    private fun extractText(body: String): String? {
        return runCatching {
            val root = JsonParser.parseString(body).asJsonObject
            val candidates = root.getAsJsonArray("candidates") ?: return null
            if (candidates.size() == 0) return null

            val parts = candidates
                .get(0)
                .asJsonObject
                .getAsJsonObject("content")
                ?.getAsJsonArray("parts")
                ?: return null

            parts.mapNotNull { element ->
                val part = element.asJsonObject
                val isThought = part.get("thought")?.takeIf { !it.isJsonNull }?.asBoolean ?: false
                if (isThought) null
                else part.get("text")?.takeIf { !it.isJsonNull }?.asString
            }.joinToString("")
        }.getOrNull()
    }

    private fun serverMessage(body: String): String? {
        return runCatching {
            JsonParser.parseString(body)
                .asJsonObject
                .getAsJsonObject("error")
                ?.get("message")
                ?.takeIf { !it.isJsonNull }
                ?.asString
        }.getOrNull()
    }

    /**
     * An empty 404 is not a Gemini API response (the API always returns JSON).
     * It usually means a VPN, Private DNS, ad-blocker or carrier proxy dropped
     * the POST before it reached Google, so give a network-focused message.
     */
    private fun httpErrorMessage(code: Int, body: String): String {
        return if (code == 404 && body.isBlank()) {
            "Gemini was unreachable: the network returned an empty HTTP 404 instead of an API response. " +
                "Turn off VPN / Private DNS / ad-blockers or switch between Wi-Fi and mobile data, then retry."
        } else {
            AiErrorMapper.fromHttp(code, serverMessage(body))
        }
    }
}

package com.rajon.jarvis

import com.rajon.jarvis.ai.AiErrorMapper
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class AiErrorMapperTest {

    @Test
    fun unauthorizedKeyIsReportedAsAuthenticationFailure() {
        val message = AiErrorMapper.fromHttp(401)
        assertTrue(message.contains("401"))
        assertTrue(message.contains("API key", ignoreCase = true))
    }

    @Test
    fun forbiddenKeyMentionsApiEnablement() {
        val message = AiErrorMapper.fromHttp(403)
        assertTrue(message.contains("403"))
        assertTrue(message.contains("not enabled", ignoreCase = true))
    }

    @Test
    fun retiredModelProducesModelSpecific404() {
        val message = AiErrorMapper.fromHttp(404)
        assertTrue(message.contains("404"))
        assertTrue(message.contains("gemini-3.8-flash"))
    }

    @Test
    fun serverMessageIsAppendedButTruncated() {
        val longDetail = "x".repeat(500)
        val message = AiErrorMapper.fromHttp(400, longDetail)
        assertTrue(message.contains("400"))
        assertTrue(message.length < 400)
    }

    @Test
    fun econnresetIsExplained() {
        val message = AiErrorMapper.fromException(IOException("Connection reset (ECONNRESET)"))
        assertTrue(message.contains("ECONNRESET"))
        assertTrue(message.contains("VPN", ignoreCase = true))
    }

    @Test
    fun dnsFailureIsExplained() {
        val message = AiErrorMapper.fromException(UnknownHostException("generativelanguage.googleapis.com"))
        assertTrue(message.contains("DNS", ignoreCase = true))
    }

    @Test
    fun timeoutIsExplained() {
        val message = AiErrorMapper.fromException(SocketTimeoutException("timeout"))
        assertTrue(message.contains("timed out", ignoreCase = true))
    }

    @Test
    fun rateLimitIsExplained() {
        val message = AiErrorMapper.fromHttp(429)
        assertTrue(message.contains("429"))
        assertTrue(message.contains("Rate limit", ignoreCase = true))
    }

    @Test
    fun serverErrorIsExplained() {
        val message = AiErrorMapper.fromHttp(503)
        assertTrue(message.contains("503"))
        assertTrue(message.contains("service error", ignoreCase = true))
    }
}

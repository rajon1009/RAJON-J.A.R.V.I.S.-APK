package com.rajon.jarvis.ai.provider

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

data class TransportResponse(
    val code: Int,
    val body: String
)

/**
 * Shared HTTP transport for every non-Gemini provider.
 *
 * Mobile network quirks handled here:
 *  - HTTP/1.1 only (some carriers answer HTTP/2 POSTs with an empty 404),
 *  - IPv4 preferred (the 5G path advertises IPv6 but cannot reach Google over it),
 *  - no idle connection reuse (carrier NAT drops keep-alive sockets silently),
 *  - one automatic retry for connection level failures.
 */
@Singleton
class HttpTransport @Inject constructor() {

    private val ipv4FirstDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            val resolved = Dns.SYSTEM.lookup(hostname)
            val ipv4 = resolved.filterIsInstance<Inet4Address>()
            return ipv4.ifEmpty { resolved }
        }
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .protocols(listOf(Protocol.HTTP_1_1))
        .dns(ipv4FirstDns)
        .connectionPool(ConnectionPool(0, 1, TimeUnit.NANOSECONDS))
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun postJson(
        url: String,
        headers: Map<String, String>,
        body: String
    ): TransportResponse = execute("POST", url, headers, body)

    suspend fun get(url: String, headers: Map<String, String>): TransportResponse =
        execute("GET", url, headers, null)

    private suspend fun execute(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?
    ): TransportResponse = withContext(Dispatchers.IO) {
        var lastError: IOException? = null
        for (attempt in 0..1) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .apply {
                        headers.forEach { (name, value) -> header(name, value) }
                    }
                    .apply {
                        if (method == "POST") {
                            post((body ?: "{}").toRequestBody(JSON_MEDIA_TYPE))
                        } else {
                            get()
                        }
                    }
                    .build()

                client.newCall(request).execute().use { response ->
                    return@withContext TransportResponse(response.code, response.body?.string() ?: "")
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (io: IOException) {
                lastError = io
                if (attempt == 1) throw io
                delay(700)
            }
        }
        throw lastError ?: IOException("Request failed")
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

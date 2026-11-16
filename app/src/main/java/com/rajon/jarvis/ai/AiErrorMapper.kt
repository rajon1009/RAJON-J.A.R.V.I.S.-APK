package com.rajon.jarvis.ai

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Converts transport/HTTP failures into short, actionable messages for the UI.
 * Deliberately free of Android dependencies so it can be unit tested on the JVM.
 */
object AiErrorMapper {

    fun fromHttp(code: Int, serverMessage: String? = null): String {
        val base = when (code) {
            400 -> "Request rejected (400). The model or request settings were not accepted."
            401 -> "Authentication failed (401). The API key is missing or invalid."
            403 -> "Access denied (403). The API key is invalid or restricted, or the Generative Language API is not enabled for this project."
            404 -> "Model not found (404). ${GeminiClient.MODEL_NAME} is not available to this API key."
            429 -> "Rate limit reached (429). Wait a moment and try again."
            in 500..599 -> "Gemini service error ($code). Please try again shortly."
            else -> "Gemini request failed with HTTP $code."
        }
        val detail = serverMessage
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { " Server said: ${it.take(180)}" }
            ?: ""
        return base + detail
    }

    fun fromException(e: Exception): String {
        val raw = e.message ?: e.javaClass.simpleName
        return when {
            e is UnknownHostException ->
                "No network connection or DNS failure. Check Wi-Fi/mobile data and Private DNS."
            e is SocketTimeoutException ->
                "The request timed out. The network is slow or blocking generativelanguage.googleapis.com."
            raw.contains("ECONNRESET", ignoreCase = true) ||
                raw.contains("Connection reset", ignoreCase = true) ->
                "The connection was reset (ECONNRESET). A VPN, Private DNS, ad-blocker or a Wi-Fi/mobile-data handover interrupted the request."
            e is IOException ->
                "Network error: ${raw.take(180)}"
            else ->
                "Unexpected error: ${raw.take(180)}"
        }
    }
}

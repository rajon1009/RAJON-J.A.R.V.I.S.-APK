package com.rajon.jarvis.ai.provider

import com.google.gson.JsonParser

/** Shared HTTP-status handling for every provider. */
internal object ProviderErrors {

    /** True when the failure is worth handing over to the next provider. */
    fun isRetryable(code: Int?, body: String): Boolean {
        if (code == null) return true // transport level failure (timeout, DNS, reset)
        if (code == 404 && body.isBlank()) return true // dropped by a middlebox
        return code == 408 || code == 409 || code == 425 || code == 429 ||
            code == 500 || code == 502 || code == 503 || code == 504
    }

    fun message(code: Int?, body: String, providerName: String, model: String): String {
        val serverText = serverMessage(body)
        return when {
            code == null ->
                "Network error while contacting $providerName. The request timed out or the connection was dropped."

            code == 401 ->
                "$providerName rejected the credential (401). The stored API key is invalid or revoked."

            code == 403 ->
                "$providerName denied the request (403). The key is restricted or the account lacks permission."

            code == 404 && body.isBlank() ->
                "$providerName was unreachable: the network returned an empty HTTP 404 instead of an API response. " +
                    "Turn off VPN / Private DNS / ad-blockers or switch network, then retry."

            code == 404 ->
                "$providerName does not expose model '$model' (404). Pick another model in AI Settings."

            code == 429 ->
                "$providerName reached its quota or rate limit (429). Switching to the next provider."

            code in 500..599 ->
                "$providerName is temporarily unavailable ($code)."

            else ->
                "$providerName request failed (HTTP $code)." + (serverText?.let { " $it" } ?: "")
        }
    }

    fun serverMessage(body: String): String? = runCatching {
        JsonParser.parseString(body)
            .asJsonObject
            .getAsJsonObject("error")
            ?.let { error ->
                val message = error.get("message")?.takeIf { !it.isJsonNull }?.asString
                val type = error.get("type")?.takeIf { !it.isJsonNull }?.asString
                if (message != null) {
                    if (type != null) "$message ($type)" else message
                } else {
                    type
                }
            }
    }.getOrNull()
}

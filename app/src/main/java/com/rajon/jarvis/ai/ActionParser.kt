package com.rajon.jarvis.ai

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import android.util.Log

sealed class JarvisAction {
    data class OpenApp(val appName: String) : JarvisAction()
    data object CheckBattery : JarvisAction()
    data object CheckStorage : JarvisAction()
    data object CheckInternet : JarvisAction()
    data object CheckPerformance : JarvisAction()
    data class OpenWhatsApp(val phone: String? = null, val message: String? = null) : JarvisAction()
    data class SearchYouTube(val query: String) : JarvisAction()
    data class SetReminder(val title: String, val minutesFromNow: Int) : JarvisAction()
    data class OpenSettings(val section: String? = null) : JarvisAction()
    data class Unknown(val raw: String) : JarvisAction()
}

object ActionParser {
    private const val TAG = "JARVIS_ActionParser"
    private val gson = Gson()

    fun extractAction(aiResponse: String): JarvisAction? {
        val regex = Regex("```json\\s*([\\s\\S]*?)\\s*```")
        val match = regex.find(aiResponse)
        val jsonStr = match?.groupValues?.get(1) ?: run {
            // Also check if raw string starts with { "action"
            val start = aiResponse.indexOf("{\"action\"")
            if (start != -1) {
                val end = aiResponse.lastIndexOf("}")
                if (end > start) aiResponse.substring(start, end + 1) else null
            } else null
        } ?: return null

        return try {
            val json = JsonParser.parseString(jsonStr).asJsonObject
            val actionType = json.get("action")?.asString?.uppercase() ?: return null
            val params = json.getAsJsonObject("params") ?: JsonObject()

            when (actionType) {
                "OPEN_APP" -> JarvisAction.OpenApp(params.get("app_name")?.asString ?: "")
                "CHECK_BATTERY" -> JarvisAction.CheckBattery
                "CHECK_STORAGE" -> JarvisAction.CheckStorage
                "CHECK_INTERNET" -> JarvisAction.CheckInternet
                "CHECK_PERFORMANCE" -> JarvisAction.CheckPerformance
                "OPEN_WHATSAPP" -> JarvisAction.OpenWhatsApp(
                    phone = params.get("phone")?.asString,
                    message = params.get("message")?.asString
                )
                "SEARCH_YOUTUBE" -> JarvisAction.SearchYouTube(params.get("query")?.asString ?: "")
                "SET_REMINDER" -> JarvisAction.SetReminder(
                    title = params.get("title")?.asString ?: "Reminder",
                    minutesFromNow = params.get("minutes")?.asInt ?: 10
                )
                "OPEN_SETTINGS" -> JarvisAction.OpenSettings(params.get("section")?.asString)
                else -> JarvisAction.Unknown(jsonStr)
            }
        } catch (e: Exception) {
            println("JARVIS_ActionParser: Failed to parse action json: $jsonStr - ${e.message}")
            null
        }
    }

    fun cleanResponseText(raw: String): String {
        return raw.replace(Regex("```json[\\s\\S]*?```"), "").trim()
    }
}

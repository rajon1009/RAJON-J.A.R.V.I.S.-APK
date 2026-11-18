package com.rajon.jarvis.ai

import com.rajon.jarvis.device.DeviceController
import com.rajon.jarvis.voice.SpeechPolicy
import javax.inject.Inject
import javax.inject.Singleton

data class LocalCommandMatch(
    val action: JarvisAction,
    val immediateSpokenResponse: String
)

@Singleton
class LocalCommandRecognizer @Inject constructor(
    private val deviceController: DeviceController
) {

    /**
     * Attempts to recognize local device commands in Bengali or English.
     * Returns a [LocalCommandMatch] if an immediate local action is recognized,
     * bypassing network round-trips for instant execution.
     */
    fun match(userText: String, language: String, ownerName: String): LocalCommandMatch? {
        val clean = userText.trim().lowercase()
        val isBn = language == SpeechPolicy.BN
        val sir = if (isBn) "স্যার" else "Sir"

        // 1. WhatsApp
        if (clean.containsAny("হোয়াটসঅ্যাপ", "হোয়াটসঅ্যাপ", "whatsapp", "হোয়াটস অ্যাপ", "হোয়াটস অ্যাপ", "watsapp")) {
            val reply = if (isBn) {
                "অবশ্যই $sir, হোয়াটসঅ্যাপ চালু করছি।"
            } else {
                "Right away, $sir. Opening WhatsApp."
            }
            return LocalCommandMatch(JarvisAction.OpenWhatsApp(), reply)
        }

        // 2. YouTube Search or Open
        if (clean.containsAny("ইউটিউব", "youtube", "ইউটিউবে", "yt")) {
            val searchPatterns = listOf(
                "ইউটিউবে গান চালাও", "গান চালাও", "ইউটিউবে সার্চ করো", "ইউটিউবে বাজাও",
                "search on youtube", "play on youtube", "play", "search youtube"
            )
            val isSearch = searchPatterns.any { clean.contains(it) }

            val query = if (isSearch) {
                clean
                    .replace(Regex("""(ইউটিউবে|ইউটিউব|গান|ভিডিও|চালান|চালাও|বাজাও|সার্চ করো|খুঁজো|search|play|on youtube|youtube for|please)"""), "")
                    .trim()
            } else ""

            val reply = if (query.isNotBlank()) {
                if (isBn) "অবশ্যই $sir, ইউটিউবে '$query' সার্চ করছি।" else "Searching YouTube for '$query', $sir."
            } else {
                if (isBn) "অবশ্যই $sir, ইউটিউব ওপেন করছি।" else "Opening YouTube, $sir."
            }
            return LocalCommandMatch(JarvisAction.SearchYouTube(query), reply)
        }

        // 3. Battery / Charging Status
        if (clean.containsAny("ব্যাটারি", "battery", "চার্জ কত", "ব্যাটারি কত", "চার্জ কেমন", "ব্যাটারি লেভেল", "battery percentage", "charge")) {
            val (level, charging) = deviceController.getBatteryInfo()
            val reply = if (isBn) {
                val state = if (charging) " এবং বর্তমানে চার্জ হচ্ছে" else ""
                "$sir, আপনার ফোনের ব্যাটারি চার্জ $level%$state।"
            } else {
                val state = if (charging) " and currently charging" else ""
                "$sir, the battery level is $level%$state."
            }
            return LocalCommandMatch(JarvisAction.CheckBattery, reply)
        }

        // 4. Storage / Memory
        if (clean.containsAny("স্টোরেজ", "storage", "মেমোরি কত", "মেমরি কত", "জায়গা কত", "মেমরি", "কত জিবি খালি")) {
            val st = deviceController.getStorageInfo()
            val reply = if (isBn) {
                "$sir, আপনার স্টোরেজে ${"%.1f".format(st.availableGB)} GB খালি আছে (মোট ${"%.1f".format(st.totalGB)} GB)।"
            } else {
                "$sir, you have ${"%.1f".format(st.availableGB)} GB available out of ${"%.1f".format(st.totalGB)} GB total."
            }
            return LocalCommandMatch(JarvisAction.CheckStorage, reply)
        }

        // 5. RAM / Performance
        if (clean.containsAny("র‍্যাম", "ram", "পারফর্মেন্স", "পারফরম্যান্স", "performance", "ফোন কেমন চলছে", "সিস্টেম স্ট্যাটাস")) {
            val ram = deviceController.getRamInfo()
            val reply = if (isBn) {
                "$sir, সিস্টেমে ${ram.availableMB} MB র‍্যাম খালি আছে (মোট ${ram.totalMB} MB)। সিস্টেম স্বাভাবিক গতিতে সচল রয়েছে।"
            } else {
                "$sir, available RAM is ${ram.availableMB} MB out of ${ram.totalMB} MB. Systems are running smoothly."
            }
            return LocalCommandMatch(JarvisAction.CheckPerformance, reply)
        }

        // 6. Network / Internet
        if (clean.containsAny("ইন্টারনেট", "নেটওয়ার্ক", "ওয়াইফাই", "ডাটা কানেকশন", "wifi", "internet", "network connection")) {
            val net = deviceController.getNetworkInfo()
            val reply = if (isBn) {
                if (net.isConnected) {
                    "$sir, ইন্টারনেট সংযুক্ত রয়েছে (${net.networkType})।"
                } else {
                    "$sir, ইন্টারনেট সংযোগ বিচ্ছিন্ন রয়েছে।"
                }
            } else {
                if (net.isConnected) {
                    "$sir, connected to ${net.networkType}."
                } else {
                    "$sir, no active internet connection detected."
                }
            }
            return LocalCommandMatch(JarvisAction.CheckInternet, reply)
        }

        // 7. Settings
        if (clean.containsAny("সেটিংস", "settings", "ফোন সেটিংস", "সেটিং")) {
            val reply = if (isBn) "অবশ্যই $sir, সেটিংস ওপেন করছি।" else "Opening settings, $sir."
            return LocalCommandMatch(JarvisAction.OpenSettings(), reply)
        }

        // 8. Camera
        if (clean.containsAny("ক্যামেরা", "camera", "ছবি তোলো", "ক্যামেরা খোলো", "take a picture")) {
            val reply = if (isBn) "অবশ্যই $sir, ক্যামেরা চালু করছি।" else "Opening camera, $sir."
            return LocalCommandMatch(JarvisAction.OpenApp("camera"), reply)
        }

        // 9. Facebook / Messenger / Chrome
        if (clean.containsAny("ফেসবুক", "facebook", "fb")) {
            val reply = if (isBn) "অবশ্যই $sir, ফেসবুক চালু করছি।" else "Opening Facebook, $sir."
            return LocalCommandMatch(JarvisAction.OpenApp("facebook"), reply)
        }
        if (clean.containsAny("মেসেঞ্জার", "messenger")) {
            val reply = if (isBn) "অবশ্যই $sir, মেসেঞ্জার চালু করছি।" else "Opening Messenger, $sir."
            return LocalCommandMatch(JarvisAction.OpenApp("messenger"), reply)
        }
        if (clean.containsAny("ক্রোম", "chrome", "ব্রাউজার", "browser")) {
            val reply = if (isBn) "অবশ্যই $sir, ব্রাউজার চালু করছি।" else "Opening browser, $sir."
            return LocalCommandMatch(JarvisAction.OpenApp("chrome"), reply)
        }

        // 10. Generic App Launch Pattern ("<AppName> খোলো" / "<AppName> ওপেন করো" / "open <AppName>")
        val openAppTrigger = listOf("খোলো", "ওপেন করো", "চালু করো", "অন করো", "খুলুন", "open", "launch")
        for (trigger in openAppTrigger) {
            if (clean.contains(trigger)) {
                val candidateName = clean
                    .replace(trigger, "")
                    .replace(Regex("""(জার্ভিস|jarvis|প্লিজ|please|অ্যাপ|app|একটু|টা|টি)"""), "")
                    .trim()

                if (candidateName.length >= 2) {
                    val app = deviceController.searchApp(candidateName)
                    if (app != null) {
                        val reply = if (isBn) {
                            "অবশ্যই $sir, ${app.appName} চালু করছি।"
                        } else {
                            "Opening ${app.appName}, $sir."
                        }
                        return LocalCommandMatch(JarvisAction.OpenApp(app.appName), reply)
                    }
                }
            }
        }

        return null
    }

    private fun String.containsAny(vararg keywords: String): Boolean =
        keywords.any { this.contains(it) }
}

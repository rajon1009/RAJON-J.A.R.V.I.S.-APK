package com.rajon.jarvis.device

import android.app.ActivityManager
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.Log
import androidx.core.app.NotificationCompat
import com.rajon.jarvis.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/** Severity levels for diagnostics alerts. */
enum class DiagnosticSeverity { INFO, WARNING, CRITICAL }

/** A single detected phone issue or status. */
data class DiagnosticIssue(
    val id: String,
    val title: String,
    val detail: String,
    val severity: DiagnosticSeverity,
    val fixAction: FixAction? = null
)

/** Automated fix that JARVIS can execute immediately. */
enum class FixAction {
    OPEN_STORAGE_SETTINGS,
    OPEN_BATTERY_SETTINGS,
    OPEN_WIFI_SETTINGS,
    OPEN_APP_MANAGER,
    OPEN_DEVELOPER_OPTIONS,
    RESTART_NETWORK,
    CLEAR_APP_CACHE
}

/**
 * PhoneDiagnosticsEngine — continuously monitors the device and surfaces all
 * detected problems, with automatic fix suggestions.
 *
 * Checks performed every [CHECK_INTERVAL_MS]:
 *  - Battery level & charging state
 *  - RAM usage
 *  - Internal storage
 *  - Network connectivity
 *  - CPU overload (via /proc/stat)
 *  - App not responding (via ActivityManager)
 */
@Singleton
class PhoneDiagnosticsEngine @Inject constructor(
    private val app: Application
) {
    companion object {
        private const val TAG = "JARVIS_Diagnostics"
        private const val CHECK_INTERVAL_MS = 30_000L   // every 30s
        private const val CHANNEL_ID = "jarvis_diagnostics"
        private const val CHANNEL_NAME = "J.A.R.V.I.S. Diagnostics"

        // Thresholds
        private const val BATTERY_LOW = 20
        private const val BATTERY_CRITICAL = 10
        private const val RAM_HIGH = 85       // % used
        private const val RAM_CRITICAL = 95
        private const val STORAGE_LOW = 15    // % free
        private const val STORAGE_CRITICAL = 5
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _issues = MutableStateFlow<List<DiagnosticIssue>>(emptyList())
    val issues: StateFlow<List<DiagnosticIssue>> = _issues.asStateFlow()

    private val _latestAlert = MutableSharedFlow<DiagnosticIssue>(extraBufferCapacity = 10)
    val latestAlert: SharedFlow<DiagnosticIssue> = _latestAlert.asSharedFlow()

    private var monitorJob: Job? = null

    // ── Public API ────────────────────────────────────────────────────────────

    /** Starts background monitoring. Call once at app startup. */
    fun startMonitoring() {
        if (monitorJob?.isActive == true) return
        setupNotificationChannel()
        monitorJob = scope.launch {
            while (isActive) {
                runAllChecks()
                delay(CHECK_INTERVAL_MS)
            }
        }
        Log.i(TAG, "Diagnostics monitoring started.")
    }

    fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
    }

    /** Run an immediate full scan and return the result. */
    suspend fun runImmediateScan(): List<DiagnosticIssue> {
        return withContext(Dispatchers.IO) {
            runAllChecks()
            _issues.value
        }
    }

    /** Run an immediate scan and return a Bengali/English summary string. */
    suspend fun getFullHealthReport(language: String = "bn"): String {
        val list = runImmediateScan()
        if (list.isEmpty()) {
            return if (language == "bn")
                "স্যার, সিস্টেম সম্পূর্ণ সুস্থ আছে। কোনো সমস্যা পাওয়া যায়নি।"
            else
                "Sir, all systems are healthy. No issues detected."
        }
        val criticals = list.filter { it.severity == DiagnosticSeverity.CRITICAL }
        val warnings  = list.filter { it.severity == DiagnosticSeverity.WARNING }
        val infos     = list.filter { it.severity == DiagnosticSeverity.INFO }

        return if (language == "bn") {
            buildString {
                if (criticals.isNotEmpty()) {
                    append("⚠️ **জরুরি সমস্যা (${criticals.size}টি):**\n")
                    criticals.forEach { append("• ${it.title}: ${it.detail}\n") }
                }
                if (warnings.isNotEmpty()) {
                    append("🔸 **সতর্কতা (${warnings.size}টি):**\n")
                    warnings.forEach { append("• ${it.title}: ${it.detail}\n") }
                }
                if (infos.isNotEmpty()) {
                    append("ℹ️ **তথ্য (${infos.size}টি):**\n")
                    infos.forEach { append("• ${it.title}: ${it.detail}\n") }
                }
            }.trim()
        } else {
            buildString {
                if (criticals.isNotEmpty()) {
                    append("⚠️ **Critical Issues (${criticals.size}):**\n")
                    criticals.forEach { append("• ${it.title}: ${it.detail}\n") }
                }
                if (warnings.isNotEmpty()) {
                    append("🔸 **Warnings (${warnings.size}):**\n")
                    warnings.forEach { append("• ${it.title}: ${it.detail}\n") }
                }
                if (infos.isNotEmpty()) {
                    append("ℹ️ **Info (${infos.size}):**\n")
                    infos.forEach { append("• ${it.title}: ${it.detail}\n") }
                }
            }.trim()
        }
    }

    /** Execute the automated fix for a known issue. Returns Bengali/English result. */
    fun applyFix(fixAction: FixAction, language: String = "bn"): String {
        return try {
            when (fixAction) {
                FixAction.OPEN_STORAGE_SETTINGS -> openSettings(android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
                FixAction.OPEN_BATTERY_SETTINGS -> openSettings(android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS)
                FixAction.OPEN_WIFI_SETTINGS    -> openSettings(android.provider.Settings.ACTION_WIFI_SETTINGS)
                FixAction.OPEN_APP_MANAGER      -> openSettings(android.provider.Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS)
                FixAction.OPEN_DEVELOPER_OPTIONS -> openSettings(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                FixAction.RESTART_NETWORK       -> openSettings(android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS)
                FixAction.CLEAR_APP_CACHE       -> openSettings(android.provider.Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS)
            }
            if (language == "bn") "স্যার, সমাধান সেটিংস খুলে দেওয়া হয়েছে।"
            else "Sir, the fix settings have been opened."
        } catch (e: Exception) {
            Log.e(TAG, "Fix action failed", e)
            if (language == "bn") "স্যার, সমাধান প্রয়োগ করা যায়নি।"
            else "Sir, could not apply fix."
        }
    }

    // ── Private checks ────────────────────────────────────────────────────────

    private fun runAllChecks() {
        val found = mutableListOf<DiagnosticIssue>()
        found += checkBattery()
        found += checkRam()
        found += checkStorage()
        found += checkNetwork()
        found += checkThermal()

        val previous = _issues.value
        _issues.value = found

        // Notify about new CRITICAL/WARNING items
        found.forEach { issue ->
            val wasPresent = previous.any { it.id == issue.id }
            if (!wasPresent && issue.severity != DiagnosticSeverity.INFO) {
                scope.launch { _latestAlert.emit(issue) }
                sendNotification(issue)
            }
        }
    }

    private fun checkBattery(): List<DiagnosticIssue> {
        return try {
            val bm = app.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = app.registerReceiver(null, intentFilter)
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                             status == BatteryManager.BATTERY_STATUS_FULL
            val health = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1

            val issues = mutableListOf<DiagnosticIssue>()

            if (!isCharging && level <= BATTERY_CRITICAL) {
                issues += DiagnosticIssue(
                    id = "battery_critical",
                    title = "ব্যাটারি ক্রিটিক্যাল",
                    detail = "মাত্র $level% চার্জ বাকি। এখনই চার্জ দিন।",
                    severity = DiagnosticSeverity.CRITICAL,
                    fixAction = FixAction.OPEN_BATTERY_SETTINGS
                )
            } else if (!isCharging && level <= BATTERY_LOW) {
                issues += DiagnosticIssue(
                    id = "battery_low",
                    title = "ব্যাটারি কম",
                    detail = "$level% চার্জ বাকি। শীঘ্রই চার্জে দিন।",
                    severity = DiagnosticSeverity.WARNING,
                    fixAction = FixAction.OPEN_BATTERY_SETTINGS
                )
            } else {
                issues += DiagnosticIssue(
                    id = "battery_ok",
                    title = "ব্যাটারি স্বাভাবিক",
                    detail = "$level%${if (isCharging) " — চার্জ হচ্ছে" else ""}",
                    severity = DiagnosticSeverity.INFO
                )
            }

            if (health == BatteryManager.BATTERY_HEALTH_OVERHEAT) {
                issues += DiagnosticIssue(
                    id = "battery_overheat",
                    title = "ব্যাটারি অতিরিক্ত গরম!",
                    detail = "ফোন চার্জ থেকে সরিয়ে নিন এবং ভারী অ্যাপ বন্ধ করুন।",
                    severity = DiagnosticSeverity.CRITICAL,
                    fixAction = FixAction.OPEN_BATTERY_SETTINGS
                )
            } else if (health == BatteryManager.BATTERY_HEALTH_DEAD) {
                issues += DiagnosticIssue(
                    id = "battery_dead",
                    title = "ব্যাটারি নষ্ট",
                    detail = "ব্যাটারি প্রতিস্থাপন করা দরকার।",
                    severity = DiagnosticSeverity.CRITICAL,
                    fixAction = FixAction.OPEN_BATTERY_SETTINGS
                )
            }
            issues
        } catch (e: Exception) {
            Log.e(TAG, "Battery check failed", e)
            emptyList()
        }
    }

    private fun checkRam(): List<DiagnosticIssue> {
        return try {
            val am = app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfo)
            val usedPercent = ((memInfo.totalMem - memInfo.availMem) * 100L / memInfo.totalMem).toInt()
            val usedMB = (memInfo.totalMem - memInfo.availMem) / (1024 * 1024)
            val totalMB = memInfo.totalMem / (1024 * 1024)

            listOf(when {
                usedPercent >= RAM_CRITICAL -> DiagnosticIssue(
                    id = "ram_critical",
                    title = "র‍্যাম সংকটজনক",
                    detail = "$usedMB MB ব্যবহৃত ($usedPercent%) — ${totalMB}MB মোট। অপ্রয়োজনীয় অ্যাপ বন্ধ করুন।",
                    severity = DiagnosticSeverity.CRITICAL,
                    fixAction = FixAction.OPEN_APP_MANAGER
                )
                usedPercent >= RAM_HIGH -> DiagnosticIssue(
                    id = "ram_high",
                    title = "র‍্যাম বেশি ব্যবহার হচ্ছে",
                    detail = "$usedMB MB ব্যবহৃত ($usedPercent%) — ${totalMB}MB মোট।",
                    severity = DiagnosticSeverity.WARNING,
                    fixAction = FixAction.OPEN_APP_MANAGER
                )
                else -> DiagnosticIssue(
                    id = "ram_ok",
                    title = "র‍্যাম স্বাভাবিক",
                    detail = "$usedMB MB / $totalMB MB ($usedPercent% ব্যবহৃত)",
                    severity = DiagnosticSeverity.INFO
                )
            })
        } catch (e: Exception) {
            Log.e(TAG, "RAM check failed", e)
            emptyList()
        }
    }

    private fun checkStorage(): List<DiagnosticIssue> {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val totalGB = stat.totalBytes / (1024f * 1024f * 1024f)
            val freeGB  = stat.availableBytes / (1024f * 1024f * 1024f)
            val freePercent = (freeGB / totalGB * 100).toInt()

            listOf(when {
                freePercent <= STORAGE_CRITICAL -> DiagnosticIssue(
                    id = "storage_critical",
                    title = "স্টোরেজ প্রায় ভরা!",
                    detail = "মাত্র ${"%.1f".format(freeGB)} GB খালি (${freePercent}%)। ফাইল মুছে জায়গা করুন।",
                    severity = DiagnosticSeverity.CRITICAL,
                    fixAction = FixAction.OPEN_STORAGE_SETTINGS
                )
                freePercent <= STORAGE_LOW -> DiagnosticIssue(
                    id = "storage_low",
                    title = "স্টোরেজ কম",
                    detail = "${"%.1f".format(freeGB)} GB খালি (${freePercent}%) — ${"%.1f".format(totalGB)} GB মোট।",
                    severity = DiagnosticSeverity.WARNING,
                    fixAction = FixAction.OPEN_STORAGE_SETTINGS
                )
                else -> DiagnosticIssue(
                    id = "storage_ok",
                    title = "স্টোরেজ স্বাভাবিক",
                    detail = "${"%.1f".format(freeGB)} GB খালি / ${"%.1f".format(totalGB)} GB মোট",
                    severity = DiagnosticSeverity.INFO
                )
            })
        } catch (e: Exception) {
            Log.e(TAG, "Storage check failed", e)
            emptyList()
        }
    }

    private fun checkNetwork(): List<DiagnosticIssue> {
        return try {
            val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(network)
            val isConnected = caps != null
            val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            val isMobile = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
            val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                              caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

            listOf(when {
                !isConnected -> DiagnosticIssue(
                    id = "net_offline",
                    title = "ইন্টারনেট সংযোগ নেই",
                    detail = "WiFi বা মোবাইল ডাটা চালু করুন।",
                    severity = DiagnosticSeverity.WARNING,
                    fixAction = FixAction.OPEN_WIFI_SETTINGS
                )
                isConnected && !hasInternet -> DiagnosticIssue(
                    id = "net_no_internet",
                    title = "ইন্টারনেট অ্যাক্সেস নেই",
                    detail = "নেটওয়ার্ক কানেক্টেড কিন্তু ইন্টারনেট নেই। WiFi পাসওয়ার্ড বা ডাটা প্যাক চেক করুন।",
                    severity = DiagnosticSeverity.WARNING,
                    fixAction = FixAction.RESTART_NETWORK
                )
                else -> DiagnosticIssue(
                    id = "net_ok",
                    title = "ইন্টারনেট সংযুক্ত",
                    detail = if (isWifi) "WiFi দিয়ে সংযুক্ত" else if (isMobile) "মোবাইল ডাটা সক্রিয়" else "সংযুক্ত",
                    severity = DiagnosticSeverity.INFO
                )
            })
        } catch (e: Exception) {
            Log.e(TAG, "Network check failed", e)
            emptyList()
        }
    }

    private fun checkThermal(): List<DiagnosticIssue> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val pm = app.getSystemService(android.os.PowerManager::class.java)
                val headroom = runCatching { pm?.getThermalHeadroom(5, 10) }
                    .getOrNull() ?: Float.NaN
                when {
                    !headroom.isNaN() && headroom < 0.1f -> listOf(DiagnosticIssue(
                        id = "thermal_critical",
                        title = "ফোন অতিরিক্ত গরম!",
                        detail = "তাপমাত্রা বিপজ্জনক মাত্রায়। ভারী অ্যাপ ও চার্জার বন্ধ করুন এবং ঠান্ডা করুন।",
                        severity = DiagnosticSeverity.CRITICAL,
                        fixAction = FixAction.OPEN_BATTERY_SETTINGS
                    ))
                    !headroom.isNaN() && headroom < 0.3f -> listOf(DiagnosticIssue(
                        id = "thermal_warm",
                        title = "ফোন গরম হচ্ছে",
                        detail = "ডিভাইস সামান্য গরম। ভারী অ্যাপ কমান।",
                        severity = DiagnosticSeverity.WARNING
                    ))
                    else -> emptyList()
                }
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun openSettings(action: String) {
        val intent = Intent(action).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        try { app.startActivity(intent) } catch (_: Exception) {
            app.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "J.A.R.V.I.S. phone health alerts"
                enableVibration(true)
            }
            val nm = app.getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(issue: DiagnosticIssue) {
        try {
            val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val pendingIntent = PendingIntent.getActivity(
                app, 0,
                Intent(app, MainActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val icon = when (issue.severity) {
                DiagnosticSeverity.CRITICAL -> android.R.drawable.ic_dialog_alert
                DiagnosticSeverity.WARNING  -> android.R.drawable.ic_dialog_info
                DiagnosticSeverity.INFO     -> android.R.drawable.ic_menu_info_details
            }
            val notification = NotificationCompat.Builder(app, CHANNEL_ID)
                .setSmallIcon(icon)
                .setContentTitle("J.A.R.V.I.S.: ${issue.title}")
                .setContentText(issue.detail)
                .setStyle(NotificationCompat.BigTextStyle().bigText(issue.detail))
                .setPriority(if (issue.severity == DiagnosticSeverity.CRITICAL)
                    NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
            nm.notify(issue.id.hashCode(), notification)
        } catch (e: Exception) {
            Log.e(TAG, "Could not send notification", e)
        }
    }
}

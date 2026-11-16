package com.rajon.jarvis.device

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.util.Log
import com.rajon.jarvis.data.model.AppInfo
import com.rajon.jarvis.data.model.DeviceInfo
import com.rajon.jarvis.data.model.NetworkInfo
import com.rajon.jarvis.data.model.RamInfo
import com.rajon.jarvis.data.model.StorageInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceController @Inject constructor(
    private val app: Application
) {
    companion object {
        private const val TAG = "JARVIS_Device"
    }

    fun getBatteryInfo(): Pair<Int, Boolean> {
        return try {
            val batteryManager = app.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = app.registerReceiver(null, intentFilter)
            val isCharging = batteryStatus?.let {
                val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
            } ?: false
            Pair(level, isCharging)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting battery info", e)
            Pair(0, false)
        }
    }

    fun getStorageInfo(): StorageInfo {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val totalBytes = stat.totalBytes
            val availableBytes = stat.availableBytes
            val totalGB = totalBytes / (1024f * 1024f * 1024f)
            val availableGB = availableBytes / (1024f * 1024f * 1024f)
            StorageInfo(totalGB, availableGB, totalGB - availableGB)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting storage info", e)
            StorageInfo(0f, 0f, 0f)
        }
    }

    fun getRamInfo(): RamInfo {
        return try {
            val activityManager = app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            val totalMB = memInfo.totalMem / (1024 * 1024)
            val availableMB = memInfo.availMem / (1024 * 1024)
            RamInfo(totalMB, availableMB, totalMB - availableMB)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting RAM info", e)
            RamInfo(0, 0, 0)
        }
    }

    fun getNetworkInfo(): NetworkInfo {
        return try {
            val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(network)
            val isConnected = caps != null
            val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            val isMobile = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
            val type = when {
                isWifi -> "WiFi"
                isMobile -> "Mobile Data"
                else -> "Disconnected"
            }
            NetworkInfo(isConnected, isWifi, isMobile, type)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting network info", e)
            NetworkInfo(false, false, false, "Unknown")
        }
    }

    fun getFullDeviceInfo(): DeviceInfo {
        val (batteryLevel, isCharging) = getBatteryInfo()
        val storage = getStorageInfo()
        val ram = getRamInfo()
        val network = getNetworkInfo()

        return DeviceInfo(
            batteryLevel = batteryLevel,
            isCharging = isCharging,
            totalStorageGB = storage.totalGB,
            availableStorageGB = storage.availableGB,
            totalRamMB = ram.totalMB,
            availableRamMB = ram.availableMB,
            isWifiConnected = network.isWifi,
            isMobileDataConnected = network.isMobileData,
            androidVersion = Build.VERSION.RELEASE,
            deviceModel = Build.MODEL,
            deviceBrand = Build.MANUFACTURER
        )
    }

    fun launchApp(packageName: String): Boolean {
        return try {
            val intent = app.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                app.startActivity(intent)
                Log.d(TAG, "Launched app: $packageName")
                true
            } else {
                Log.w(TAG, "App not found: $packageName")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error launching app: $packageName", e)
            false
        }
    }

    fun getInstalledApps(): List<AppInfo> {
        return try {
            val pm = app.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            packages
                .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
                .map { appInfo ->
                    AppInfo(
                        packageName = appInfo.packageName,
                        appName = pm.getApplicationLabel(appInfo).toString(),
                        icon = try { pm.getApplicationIcon(appInfo) } catch (_: Exception) { null }
                    )
                }
                .sortedBy { it.appName.lowercase() }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting installed apps", e)
            emptyList()
        }
    }

    fun searchApp(query: String): AppInfo? {
        val apps = getInstalledApps()
        val queryLower = query.lowercase()
        return apps.find { it.appName.lowercase() == queryLower }
            ?: apps.find { it.appName.lowercase().contains(queryLower) }
            ?: apps.find { it.packageName.lowercase().contains(queryLower) }
    }

    fun openYouTubeSearch(query: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SEARCH).apply {
                setPackage("com.google.android.youtube")
                putExtra("query", query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            app.startActivity(intent)
            true
        } catch (e: Exception) {
            // Fallback to browser
            try {
                val searchUrl = "https://www.youtube.com/results?search_query=${Uri.encode(query)}"
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                app.startActivity(browserIntent)
                true
            } catch (e2: Exception) {
                Log.e(TAG, "Error opening YouTube search", e2)
                false
            }
        }
    }

    fun openWhatsApp(): Boolean {
        return launchApp("com.whatsapp")
    }

    fun sendWhatsAppMessage(phone: String, message: String): Boolean {
        return try {
            val url = "https://api.whatsapp.com/send?phone=$phone&text=${Uri.encode(message)}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            app.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending WhatsApp message", e)
            false
        }
    }

    fun openSettings(settingsAction: String = Settings.ACTION_SETTINGS): Boolean {
        return try {
            val intent = Intent(settingsAction).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            app.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error opening settings: $settingsAction", e)
            false
        }
    }

    fun getPerformanceSummary(): String {
        val info = getFullDeviceInfo()
        val storage = getStorageInfo()
        val ram = getRamInfo()

        return buildString {
            appendLine("📱 Device: ${info.deviceBrand} ${info.deviceModel}")
            appendLine("🤖 Android: ${info.androidVersion}")
            appendLine("🔋 Battery: ${info.batteryLevel}%${if (info.isCharging) " (Charging)" else ""}")
            appendLine("💾 Storage: ${"%.1f".format(storage.usedGB)}GB / ${"%.1f".format(storage.totalGB)}GB used")
            appendLine("🧠 RAM: ${ram.usedMB}MB / ${ram.totalMB}MB used")
            appendLine("🌐 Network: ${getNetworkInfo().networkType}")
        }
    }
}

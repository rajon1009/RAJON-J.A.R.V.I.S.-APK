package com.rajon.jarvis.data.model

import android.graphics.drawable.Drawable

data class DeviceInfo(
    val batteryLevel: Int = 0,
    val isCharging: Boolean = false,
    val totalStorageGB: Float = 0f,
    val availableStorageGB: Float = 0f,
    val totalRamMB: Long = 0,
    val availableRamMB: Long = 0,
    val isWifiConnected: Boolean = false,
    val isMobileDataConnected: Boolean = false,
    val androidVersion: String = "",
    val deviceModel: String = "",
    val deviceBrand: String = ""
)

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null
)

data class StorageInfo(
    val totalGB: Float,
    val availableGB: Float,
    val usedGB: Float
)

data class RamInfo(
    val totalMB: Long,
    val availableMB: Long,
    val usedMB: Long
)

data class NetworkInfo(
    val isConnected: Boolean,
    val isWifi: Boolean,
    val isMobileData: Boolean,
    val networkType: String
)

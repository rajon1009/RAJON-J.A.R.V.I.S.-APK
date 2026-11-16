package com.rajon.jarvis.ui.screens.system

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.rajon.jarvis.navigation.Screen
import com.rajon.jarvis.ui.components.BottomNavBar
import com.rajon.jarvis.ui.components.GlassPanel
import com.rajon.jarvis.ui.components.GlowingText
import com.rajon.jarvis.ui.components.HudSectionTitle
import com.rajon.jarvis.ui.theme.JarvisCyan
import com.rajon.jarvis.ui.theme.JarvisCyanLight
import com.rajon.jarvis.ui.theme.JarvisDarkBackground
import com.rajon.jarvis.ui.theme.JarvisDarkSurface
import com.rajon.jarvis.ui.theme.JarvisOrange
import com.rajon.jarvis.ui.theme.JarvisSuccess
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemMonitorScreen(
    navController: NavController,
    viewModel: SystemMonitorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Live refresh while the screen is visible only.
    LaunchedEffect(Unit) {
        while (true) {
            delay(5_000)
            viewModel.refresh()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    GlowingText(
                        text = "SYSTEM MONITOR",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh telemetry",
                            tint = JarvisCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JarvisDarkSurface)
            )
        },
        bottomBar = {
            BottomNavBar(
                currentRoute = Screen.SystemMonitoring.route,
                onNavigate = { screen ->
                    navController.navigate(screen.route) {
                        popUpTo(Screen.Dashboard.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        },
        containerColor = JarvisDarkBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { HudSectionTitle(text = "POWER") }
            item {
                MetricPanel(
                    icon = if (state.deviceInfo.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                    title = "Battery",
                    value = "${state.deviceInfo.batteryLevel}%",
                    detail = if (state.deviceInfo.isCharging) "Charging" else "Discharging",
                    accent = if (state.deviceInfo.batteryLevel < 20) JarvisOrange else JarvisSuccess,
                    progress = (state.deviceInfo.batteryLevel / 100f).coerceIn(0f, 1f)
                )
            }

            item { HudSectionTitle(text = "COMPUTE") }
            item {
                MetricPanel(
                    icon = Icons.Default.Memory,
                    title = "RAM",
                    value = "${state.ramInfo.usedMB} MB used",
                    detail = "${state.ramInfo.availableMB} MB available of ${state.ramInfo.totalMB} MB",
                    accent = JarvisCyanLight,
                    progress = if (state.ramInfo.totalMB > 0L) {
                        (state.ramInfo.usedMB.toFloat() / state.ramInfo.totalMB.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                )
            }
            item {
                MetricPanel(
                    icon = Icons.Default.Storage,
                    title = "Internal Storage",
                    value = "${"%.1f".format(state.storageInfo.usedGB)} GB used",
                    detail = "${"%.1f".format(state.storageInfo.availableGB)} GB free of ${"%.1f".format(state.storageInfo.totalGB)} GB",
                    accent = JarvisCyan,
                    progress = if (state.storageInfo.totalGB > 0f) {
                        (state.storageInfo.usedGB / state.storageInfo.totalGB).coerceIn(0f, 1f)
                    } else 0f
                )
            }

            item { HudSectionTitle(text = "CONNECTIVITY") }
            item {
                MetricPanel(
                    icon = if (state.networkInfo.isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                    title = "Network",
                    value = state.networkInfo.networkType,
                    detail = buildString {
                        append(if (state.networkInfo.isConnected) "Connected" else "Disconnected")
                        if (state.networkInfo.isWifi) append(" • Wi-Fi")
                        if (state.networkInfo.isMobileData) append(" • Mobile data")
                    },
                    accent = if (state.networkInfo.isConnected) JarvisSuccess else JarvisOrange,
                    progress = null
                )
            }

            item { HudSectionTitle(text = "DEVICE") }
            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Devices,
                            contentDescription = null,
                            tint = JarvisCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "${state.deviceInfo.deviceBrand} ${state.deviceInfo.deviceModel}".trim()
                                .ifBlank { "Android device" },
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    InfoRow("Android", state.deviceInfo.androidVersion.ifBlank { "unknown" })
                    InfoRow("CPU cores", "${state.cpuCores}")
                    InfoRow("Uptime", state.uptimeText)
                    InfoRow("Refresh", if (state.isRefreshing) "Scanning…" else "Live every 5s")
                }
            }
        }
    }
}

@Composable
private fun MetricPanel(
    icon: ImageVector,
    title: String,
    value: String,
    detail: String,
    accent: Color,
    progress: Float?
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        borderColor = accent.copy(alpha = 0.35f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(text = title, color = Color.LightGray, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = value,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = detail, color = Color.Gray, fontSize = 11.sp)
        if (progress != null) {
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp),
                color = accent,
                trackColor = Color.White.copy(alpha = 0.08f)
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, fontSize = 11.sp)
        Text(text = value, color = Color.LightGray, fontSize = 11.sp)
    }
}

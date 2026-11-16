package com.rajon.jarvis.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.rajon.jarvis.ai.GeminiClient
import com.rajon.jarvis.navigation.Screen
import com.rajon.jarvis.ui.components.ArcReactor
import com.rajon.jarvis.ui.components.BottomNavBar
import com.rajon.jarvis.ui.components.GlowingText
import com.rajon.jarvis.ui.components.GlassPanel
import com.rajon.jarvis.ui.components.HudSectionTitle
import com.rajon.jarvis.ui.theme.JarvisCyan
import com.rajon.jarvis.ui.theme.JarvisCyanLight
import com.rajon.jarvis.ui.theme.JarvisDarkBackground
import com.rajon.jarvis.ui.theme.JarvisDarkSurface
import com.rajon.jarvis.ui.theme.JarvisOrange
import com.rajon.jarvis.ui.theme.JarvisSuccess
import com.rajon.jarvis.ui.theme.JarvisWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(if (state.isAiConfigured) JarvisSuccess else JarvisWarning)
                        )
                        GlowingText(
                            text = "J.A.R.V.I.S.",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
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
                currentRoute = Screen.Dashboard.route,
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
            item {
                CorePanel(
                    greeting = state.greeting,
                    isAiConfigured = state.isAiConfigured,
                    isCharging = state.deviceInfo.isCharging
                )
            }

            item {
                HudSectionTitle(text = "QUICK COMMANDS")
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardAction(
                        title = "AI Chat",
                        subtitle = "Text with J.A.R.V.I.S.",
                        icon = Icons.Default.Chat,
                        accent = JarvisCyan,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.Chat.route) }
                    )
                    DashboardAction(
                        title = "Voice Command",
                        subtitle = "Speak and it listens",
                        icon = Icons.Default.Mic,
                        accent = JarvisOrange,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("${Screen.Voice.route}?${Screen.ARG_AUTO_START}=true") }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardAction(
                        title = "Device Control",
                        subtitle = "Apps, settings, media",
                        icon = Icons.Default.PhoneAndroid,
                        accent = JarvisCyanLight,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.DeviceControl.route) }
                    )
                    DashboardAction(
                        title = "Automation",
                        subtitle = "Reminders and tasks",
                        icon = Icons.Default.Schedule,
                        accent = JarvisWarning,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.Automation.route) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(2.dp))
                HudSectionTitle(text = "LIVE TELEMETRY")
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TelemetryTile(
                        label = "BATTERY",
                        value = "${state.deviceInfo.batteryLevel}%${if (state.deviceInfo.isCharging) " ⚡" else ""}",
                        icon = if (state.deviceInfo.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                        accent = if (state.deviceInfo.batteryLevel < 20) JarvisOrange else JarvisSuccess,
                        progress = (state.deviceInfo.batteryLevel / 100f).coerceIn(0f, 1f),
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryTile(
                        label = "RAM",
                        value = "${state.ramInfo.usedMB} MB",
                        icon = Icons.Default.Memory,
                        accent = JarvisCyanLight,
                        progress = if (state.ramInfo.totalMB > 0L) {
                            (state.ramInfo.usedMB.toFloat() / state.ramInfo.totalMB.toFloat()).coerceIn(0f, 1f)
                        } else 0f,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TelemetryTile(
                        label = "STORAGE",
                        value = "${"%.1f".format(state.storageInfo.usedGB)} / ${"%.1f".format(state.storageInfo.totalGB)} GB",
                        icon = Icons.Default.Storage,
                        accent = JarvisCyan,
                        progress = if (state.storageInfo.totalGB > 0f) {
                            (state.storageInfo.usedGB / state.storageInfo.totalGB).coerceIn(0f, 1f)
                        } else 0f,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryTile(
                        label = "NETWORK",
                        value = state.networkInfo.networkType,
                        icon = if (state.networkInfo.isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                        accent = if (state.networkInfo.isConnected) JarvisSuccess else JarvisOrange,
                        progress = null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                SystemLinkPanel(
                    isAiConfigured = state.isAiConfigured,
                    deviceName = "${state.deviceInfo.deviceBrand} ${state.deviceInfo.deviceModel}".trim(),
                    androidVersion = state.deviceInfo.androidVersion
                )
            }
        }
    }
}

@Composable
private fun CorePanel(
    greeting: String,
    isAiConfigured: Boolean,
    isCharging: Boolean
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        borderColor = JarvisCyan.copy(alpha = 0.55f),
        contentPadding = PaddingValues(vertical = 20.dp, horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ArcReactor(
                coreSize = 168.dp,
                isActive = isAiConfigured
            )
            Spacer(modifier = Modifier.height(10.dp))
            GlowingText(
                text = "J.A.R.V.I.S.",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isAiConfigured) "NEURAL CORE ONLINE" else "AI KEY REQUIRED",
                color = if (isAiConfigured) JarvisSuccess else JarvisOrange,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = greeting,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = if (isCharging) "All systems nominal • Charging" else "All systems monitored in real time",
                color = Color.Gray,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun DashboardAction(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    GlassPanel(
        modifier = modifier
            .height(112.dp)
            .clickable(onClick = onClick),
        borderColor = accent.copy(alpha = 0.40f),
        contentPadding = PaddingValues(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = accent,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Text(
            text = subtitle,
            color = Color.Gray,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun TelemetryTile(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    progress: Float?,
    modifier: Modifier = Modifier
) {
    GlassPanel(
        modifier = modifier.height(108.dp),
        borderColor = accent.copy(alpha = 0.35f),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = Color.LightGray,
                fontSize = 10.sp,
                letterSpacing = 1.sp
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        if (progress != null) {
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = accent,
                trackColor = Color.White.copy(alpha = 0.08f)
            )
        }
    }
}

@Composable
private fun SystemLinkPanel(
    isAiConfigured: Boolean,
    deviceName: String,
    androidVersion: String
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        borderColor = JarvisCyan.copy(alpha = 0.25f)
    ) {
        HudSectionTitle(text = "SYSTEM LINK")
        Spacer(modifier = Modifier.height(10.dp))
        LinkRow("AI MODEL", GeminiClient.MODEL_NAME, JarvisCyan)
        LinkRow("API", "Gemini REST ${GeminiClient.API_VERSION}", JarvisCyan)
        LinkRow(
            "AI KEY",
            if (isAiConfigured) "Configured" else "Not configured",
            if (isAiConfigured) JarvisSuccess else JarvisOrange
        )
        LinkRow(
            "DEVICE",
            if (deviceName.isBlank()) "Android $androidVersion" else "$deviceName • Android $androidVersion",
            Color.LightGray
        )
    }
}

@Composable
private fun LinkRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.Gray, fontSize = 10.sp, letterSpacing = 1.sp)
        Text(
            text = value,
            color = valueColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.rajon.jarvis.navigation.Screen
import com.rajon.jarvis.ui.components.BottomNavBar
import com.rajon.jarvis.ui.components.GlowingText
import com.rajon.jarvis.ui.components.StatusCard
import com.rajon.jarvis.ui.theme.JarvisCyan
import com.rajon.jarvis.ui.theme.JarvisDarkBackground
import com.rajon.jarvis.ui.theme.JarvisDarkSurface
import com.rajon.jarvis.ui.theme.JarvisOrange
import com.rajon.jarvis.ui.theme.JarvisSuccess

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
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(JarvisSuccess)
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
                            contentDescription = "Refresh System State",
                            tint = JarvisCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = JarvisDarkSurface
                )
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Greeting Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = JarvisDarkSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = state.greeting,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                        Text(
                            text = "All systems online and operational. How may I assist you today?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Quick Actions Section Title
            item {
                Text(
                    text = "QUICK ACTIONS",
                    style = MaterialTheme.typography.labelMedium,
                    color = JarvisCyan,
                    letterSpacing = 1.sp
                )
            }

            // Quick Actions 2x2 Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        title = "Chat with J.A.R.V.I.S.",
                        icon = Icons.Default.Chat,
                        iconTint = JarvisCyan,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.Chat.route) }
                    )
                    QuickActionCard(
                        title = "Voice Command",
                        icon = Icons.Default.Mic,
                        iconTint = JarvisOrange,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.Chat.route) }
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        title = "Device Status",
                        icon = Icons.Default.PhoneAndroid,
                        iconTint = JarvisCyan,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.DeviceControl.route) }
                    )
                    QuickActionCard(
                        title = "Settings",
                        icon = Icons.Default.Settings,
                        iconTint = Color.LightGray,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.Settings.route) }
                    )
                }
            }

            // System Telemetry Section Title
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "SYSTEM TELEMETRY",
                    style = MaterialTheme.typography.labelMedium,
                    color = JarvisCyan,
                    letterSpacing = 1.sp
                )
            }

            // Battery Card
            item {
                val batteryProgress = (state.deviceInfo.batteryLevel / 100f).coerceIn(0f, 1f)
                val batteryIcon = if (state.deviceInfo.isCharging) {
                    Icons.Default.BatteryChargingFull
                } else {
                    Icons.Default.BatteryFull
                }
                StatusCard(
                    title = "Battery",
                    value = "${state.deviceInfo.batteryLevel}% ${if (state.deviceInfo.isCharging) "(Charging)" else ""}",
                    icon = batteryIcon,
                    progress = batteryProgress
                )
            }

            // Storage Card
            item {
                val storageUsed = state.storageInfo.usedGB
                val storageTotal = state.storageInfo.totalGB
                val storageProgress = if (storageTotal > 0f) (storageUsed / storageTotal).coerceIn(0f, 1f) else 0f
                StatusCard(
                    title = "Internal Storage",
                    value = String.format("%.1f GB / %.1f GB used", storageUsed, storageTotal),
                    icon = Icons.Default.Storage,
                    progress = storageProgress
                )
            }

            // RAM Card
            item {
                val ramUsed = state.ramInfo.usedMB
                val ramTotal = state.ramInfo.totalMB
                val ramProgress = if (ramTotal > 0L) (ramUsed.toFloat() / ramTotal.toFloat()).coerceIn(0f, 1f) else 0f
                StatusCard(
                    title = "RAM Usage",
                    value = "$ramUsed MB / $ramTotal MB",
                    icon = Icons.Default.Memory,
                    progress = ramProgress
                )
            }

            // Network Card
            item {
                val net = state.networkInfo
                val netIcon = if (net.isConnected) Icons.Default.Wifi else Icons.Default.WifiOff
                StatusCard(
                    title = "Network Connection",
                    value = if (net.isConnected) net.networkType else "Disconnected",
                    icon = netIcon
                )
            }
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisDarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
    }
}

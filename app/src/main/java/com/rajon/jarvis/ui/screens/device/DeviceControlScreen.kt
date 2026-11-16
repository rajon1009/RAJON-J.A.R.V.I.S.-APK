package com.rajon.jarvis.ui.screens.device

import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.rajon.jarvis.ui.theme.JarvisCyan
import com.rajon.jarvis.ui.theme.JarvisDarkBackground
import com.rajon.jarvis.ui.theme.JarvisDarkSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceControlScreen(
    navController: NavController,
    viewModel: DeviceControlViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    GlowingText(
                        text = "Device Controller",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = JarvisCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JarvisDarkSurface)
            )
        },
        bottomBar = {
            BottomNavBar(
                currentRoute = Screen.DeviceControl.route,
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
            // Hardware Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = JarvisDarkSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "HARDWARE & SYSTEM SPECIFICATIONS",
                            style = MaterialTheme.typography.labelMedium,
                            color = JarvisCyan
                        )
                        TelemetryRow("Manufacturer", state.deviceInfo.deviceBrand.uppercase())
                        TelemetryRow("Model", state.deviceInfo.deviceModel)
                        TelemetryRow("Android Version", "Android ${state.deviceInfo.androidVersion}")
                        TelemetryRow(
                            "Battery",
                            "${state.deviceInfo.batteryLevel}% ${if (state.deviceInfo.isCharging) "(Charging)" else ""}"
                        )
                        TelemetryRow(
                            "Storage",
                            String.format("%.1f GB free / %.1f GB total", state.storageInfo.availableGB, state.storageInfo.totalGB)
                        )
                        TelemetryRow(
                            "Memory (RAM)",
                            "${state.ramInfo.availableMB} MB available / ${state.ramInfo.totalMB} MB total"
                        )
                        TelemetryRow("Network", state.networkInfo.networkType)
                    }
                }
            }

            // Quick System Settings Shortcuts
            item {
                Text(
                    text = "SYSTEM SHORTCUTS",
                    style = MaterialTheme.typography.labelMedium,
                    color = JarvisCyan
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShortcutButton(
                        text = "Settings",
                        icon = Icons.Default.Settings,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.openSettings(Settings.ACTION_SETTINGS) }
                    )
                    ShortcutButton(
                        text = "Wi-Fi",
                        icon = Icons.Default.Wifi,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.openSettings(Settings.ACTION_WIFI_SETTINGS) }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShortcutButton(
                        text = "Battery",
                        icon = Icons.Default.BatteryStd,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.openSettings(Settings.ACTION_BATTERY_SAVER_SETTINGS) }
                    )
                    ShortcutButton(
                        text = "Apps",
                        icon = Icons.Default.Apps,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.openSettings(Settings.ACTION_APPLICATION_SETTINGS) }
                    )
                }
            }

            // App Launcher Header
            item {
                Text(
                    text = "INSTALLED APPLICATIONS (${state.filteredApps.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = JarvisCyan
                )
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search installed apps...", color = Color.Gray) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = JarvisCyan
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = JarvisDarkSurface,
                        unfocusedContainerColor = JarvisDarkSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // App Items
            items(state.filteredApps, key = { it.packageName }) { appInfo ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.launchApp(appInfo.packageName) },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = JarvisDarkSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = appInfo.appName,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = appInfo.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Launch,
                            contentDescription = "Launch",
                            tint = JarvisCyan
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, color = Color.White, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ShortcutButton(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisCyan)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = text)
            Text(text = text, maxLines = 1)
        }
    }
}

package com.rajon.jarvis.ui.screens.more

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.rajon.jarvis.BuildConfig
import com.rajon.jarvis.navigation.Screen
import com.rajon.jarvis.ui.components.BottomNavBar
import com.rajon.jarvis.ui.components.GlassPanel
import com.rajon.jarvis.ui.components.GlowingText
import com.rajon.jarvis.ui.components.HudSectionTitle
import com.rajon.jarvis.ui.theme.JarvisCyan
import com.rajon.jarvis.ui.theme.JarvisDarkBackground
import com.rajon.jarvis.ui.theme.JarvisDarkSurface
import com.rajon.jarvis.ui.theme.JarvisOrange
import com.rajon.jarvis.ui.theme.JarvisSuccess
import com.rajon.jarvis.ui.theme.JarvisWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    GlowingText(
                        text = "MORE",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JarvisDarkSurface)
            )
        },
        bottomBar = {
            BottomNavBar(
                currentRoute = Screen.More.route,
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { HudSectionTitle(text = "CONTROL PANELS") }

            item {
                MoreRow(
                    title = "System Monitoring",
                    description = "Battery, RAM, storage, network, uptime",
                    icon = Icons.Default.MonitorHeart,
                    accent = JarvisSuccess,
                    onClick = { navController.navigate(Screen.SystemMonitoring.route) }
                )
            }
            item {
                MoreRow(
                    title = "Automation",
                    description = "Schedule and cancel reminders",
                    icon = Icons.Default.Schedule,
                    accent = JarvisWarning,
                    onClick = { navController.navigate(Screen.Automation.route) }
                )
            }
            item {
                MoreRow(
                    title = "AI Settings",
                    description = "Gemini key, model, connection test",
                    icon = Icons.Default.Tune,
                    accent = JarvisCyan,
                    onClick = { navController.navigate(Screen.AiSettings.route) }
                )
            }
            item {
                MoreRow(
                    title = "General Settings",
                    description = "Owner, language, voice, data",
                    icon = Icons.Default.Settings,
                    accent = JarvisOrange,
                    onClick = { navController.navigate(Screen.GeneralSettings.route) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = JarvisCyan.copy(alpha = 0.20f)
                ) {
                    Text(
                        text = "RAJON J.A.R.V.I.S. • v${BuildConfig.VERSION_NAME}",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Android assistant • Gemini ${com.rajon.jarvis.ai.GeminiClient.MODEL_NAME}",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MoreRow(
    title: String,
    description: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        borderColor = accent.copy(alpha = 0.35f),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}

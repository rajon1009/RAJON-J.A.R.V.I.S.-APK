package com.rajon.jarvis.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.rajon.jarvis.navigation.Screen
import com.rajon.jarvis.ui.theme.JarvisCyan
import com.rajon.jarvis.ui.theme.JarvisDarkSurface

data class NavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)

@Composable
fun BottomNavBar(
    currentRoute: String,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavItem(Screen.Dashboard, Icons.Default.Dashboard, "Home"),
        NavItem(Screen.Chat, Icons.Default.Chat, "Chat"),
        NavItem(Screen.Voice, Icons.Default.Mic, "Voice"),
        NavItem(Screen.DeviceControl, Icons.Default.PhoneAndroid, "Device"),
        NavItem(Screen.More, Icons.Default.Apps, "More")
    )

    NavigationBar(
        modifier = modifier,
        containerColor = JarvisDarkSurface,
        contentColor = Color.White
    ) {
        items.forEach { item ->
            val isSelected = when (item.screen) {
                Screen.Voice -> currentRoute.startsWith(Screen.Voice.route)
                Screen.More -> currentRoute in Screen.moreRoutes
                else -> currentRoute == item.screen.route
            }

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        onNavigate(item.screen)
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(text = item.label)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = JarvisCyan,
                    selectedTextColor = JarvisCyan,
                    indicatorColor = JarvisCyan.copy(alpha = 0.15f),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}

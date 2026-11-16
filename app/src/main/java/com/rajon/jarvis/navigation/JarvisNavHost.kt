package com.rajon.jarvis.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rajon.jarvis.ui.screens.chat.ChatScreen
import com.rajon.jarvis.ui.screens.dashboard.DashboardScreen
import com.rajon.jarvis.ui.screens.device.DeviceControlScreen
import com.rajon.jarvis.ui.screens.settings.SettingsScreen

sealed class Screen(val route: String, val title: String) {
    data object Dashboard : Screen("dashboard", "Dashboard")
    data object Chat : Screen("chat", "J.A.R.V.I.S.")
    data object DeviceControl : Screen("device_control", "Device")
    data object Settings : Screen("settings", "Settings")
}

@Composable
fun JarvisNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(navController = navController)
        }
        composable(Screen.Chat.route) {
            ChatScreen(navController = navController)
        }
        composable(Screen.DeviceControl.route) {
            DeviceControlScreen(navController = navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
    }
}

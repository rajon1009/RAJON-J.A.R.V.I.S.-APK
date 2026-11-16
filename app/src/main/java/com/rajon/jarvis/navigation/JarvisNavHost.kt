package com.rajon.jarvis.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rajon.jarvis.ui.screens.automation.AutomationScreen
import com.rajon.jarvis.ui.screens.chat.ChatScreen
import com.rajon.jarvis.ui.screens.dashboard.DashboardScreen
import com.rajon.jarvis.ui.screens.device.DeviceControlScreen
import com.rajon.jarvis.ui.screens.more.MoreScreen
import com.rajon.jarvis.ui.screens.settings.AiSettingsScreen
import com.rajon.jarvis.ui.screens.settings.GeneralSettingsScreen
import com.rajon.jarvis.ui.screens.system.SystemMonitorScreen
import com.rajon.jarvis.ui.screens.voice.VoiceAssistantScreen

sealed class Screen(val route: String, val title: String) {
    data object Dashboard : Screen("dashboard", "Dashboard")
    data object Chat : Screen("chat", "J.A.R.V.I.S.")
    data object Voice : Screen("voice", "Voice Assistant")
    data object DeviceControl : Screen("device_control", "Device")
    data object SystemMonitoring : Screen("system_monitoring", "System Monitoring")
    data object Automation : Screen("automation", "Automation")
    data object AiSettings : Screen("ai_settings", "AI Settings")
    data object GeneralSettings : Screen("general_settings", "General Settings")
    data object More : Screen("more", "More")

    companion object {
        const val VOICE_ROUTE = "voice?autoStart={autoStart}"
        const val ARG_AUTO_START = "autoStart"

        val moreRoutes: Set<String> = setOf(
            More.route,
            SystemMonitoring.route,
            Automation.route,
            AiSettings.route,
            GeneralSettings.route
        )
    }
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
        composable(
            route = Screen.VOICE_ROUTE,
            arguments = listOf(
                navArgument(Screen.ARG_AUTO_START) {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            VoiceAssistantScreen(
                navController = navController,
                autoStart = backStackEntry.arguments?.getBoolean(Screen.ARG_AUTO_START) ?: false
            )
        }
        composable(Screen.DeviceControl.route) {
            DeviceControlScreen(navController = navController)
        }
        composable(Screen.SystemMonitoring.route) {
            SystemMonitorScreen(navController = navController)
        }
        composable(Screen.Automation.route) {
            AutomationScreen(navController = navController)
        }
        composable(Screen.AiSettings.route) {
            AiSettingsScreen(navController = navController)
        }
        composable(Screen.GeneralSettings.route) {
            GeneralSettingsScreen(navController = navController)
        }
        composable(Screen.More.route) {
            MoreScreen(navController = navController)
        }
    }
}

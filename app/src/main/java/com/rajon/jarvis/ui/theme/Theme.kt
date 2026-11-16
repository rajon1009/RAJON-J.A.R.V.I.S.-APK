package com.rajon.jarvis.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Dark color scheme – the default sci-fi / Iron Man–inspired palette.
 */
private val JarvisDarkColorScheme = darkColorScheme(
    primary = JarvisCyan,
    onPrimary = Color.Black,
    primaryContainer = JarvisCyanDark,
    onPrimaryContainer = JarvisCyanLight,
    secondary = JarvisOrange,
    onSecondary = Color.Black,
    secondaryContainer = JarvisOrangeDark,
    onSecondaryContainer = JarvisOrangeLight,
    tertiary = JarvisCyanVariant,
    onTertiary = Color.Black,
    background = JarvisDarkBackground,
    onBackground = JarvisDarkOnSurface,
    surface = JarvisDarkSurface,
    onSurface = JarvisDarkOnSurface,
    surfaceVariant = JarvisDarkSurfaceVariant,
    onSurfaceVariant = JarvisDarkOnSurfaceVariant,
    outline = JarvisDarkOutline,
    error = JarvisError,
    onError = Color.White,
    errorContainer = JarvisErrorDark,
    onErrorContainer = Color.White,
)

/**
 * Light color scheme – softer variant for daylight use.
 */
private val JarvisLightColorScheme = lightColorScheme(
    primary = JarvisCyanDark,
    onPrimary = Color.White,
    primaryContainer = JarvisCyanLight,
    onPrimaryContainer = Color.Black,
    secondary = JarvisOrangeDark,
    onSecondary = Color.White,
    secondaryContainer = JarvisOrangeLight,
    onSecondaryContainer = Color.Black,
    tertiary = JarvisCyanVariant,
    onTertiary = Color.White,
    background = JarvisLightBackground,
    onBackground = JarvisLightOnSurface,
    surface = JarvisLightSurface,
    onSurface = JarvisLightOnSurface,
    surfaceVariant = JarvisLightSurfaceVariant,
    onSurfaceVariant = JarvisLightOnSurfaceVariant,
    outline = JarvisLightOutline,
    error = JarvisError,
    onError = Color.White,
    errorContainer = JarvisErrorDark,
    onErrorContainer = Color.White,
)

/**
 * Main application theme for J.A.R.V.I.S.
 *
 * @param darkTheme Whether to use the dark theme. Defaults to `true` for the sci-fi feel.
 * @param dynamicColor Whether to use Material You dynamic color on Android 12+.
 *                     Defaults to `false` to preserve the brand palette.
 */
@Composable
fun JarvisTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> JarvisDarkColorScheme
        else -> JarvisLightColorScheme
    }

    // Tint the system bars to match the theme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = JarvisTypography,
        content = content,
    )
}

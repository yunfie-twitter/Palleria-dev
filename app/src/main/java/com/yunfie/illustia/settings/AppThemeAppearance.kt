package com.yunfie.illustia.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import android.os.Build
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
fun rememberAppThemeColors(settings: AppSettings): Colors {
    val systemDark = isSystemInDarkTheme()
    val effectiveDark = isAppDarkTheme(settings.themeMode, systemDark)
    val useDynamicColor = settings.useDynamicColor && isDynamicColorAvailable()
    val controller = remember(settings.themeMode, useDynamicColor, systemDark) {
        ThemeController(
            colorSchemeMode = settings.toColorSchemeMode(useDynamicColor),
        )
    }
    val colors = controller.currentColors()
    return if (settings.amoledMode && effectiveDark) {
        colors.toAmoledColors()
    } else {
        colors
    }
}

fun isAppDarkTheme(themeMode: String, systemDark: Boolean): Boolean {
    return when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> systemDark
    }
}

fun isDynamicColorAvailable(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}

private fun AppSettings.toColorSchemeMode(useDynamicColor: Boolean): ColorSchemeMode {
    return when (themeMode) {
        "light" -> if (useDynamicColor) ColorSchemeMode.MonetLight else ColorSchemeMode.Light
        "dark" -> if (useDynamicColor) ColorSchemeMode.MonetDark else ColorSchemeMode.Dark
        else -> if (useDynamicColor) ColorSchemeMode.MonetSystem else ColorSchemeMode.System
    }
}

private fun Colors.toAmoledColors(): Colors {
    val white = Color.White
    val black = Color.Black
    val panel = Color(0xFF121212)
    val panelHigh = Color(0xFF1A1A1A)
    val panelHighest = Color(0xFF202020)
    val panelVariant = Color(0xFF181818)
    return copy(
        background = black,
        onBackground = white,
        onBackgroundVariant = white.copy(alpha = 0.76f),
        surface = panel,
        onSurface = white,
        surfaceVariant = panelVariant,
        onSurfaceSecondary = white.copy(alpha = 0.8f),
        onSurfaceVariantSummary = white.copy(alpha = 0.74f),
        onSurfaceVariantActions = white.copy(alpha = 0.9f),
        disabledOnSurface = white.copy(alpha = 0.28f),
        surfaceContainer = panel,
        onSurfaceContainer = white,
        onSurfaceContainerVariant = white.copy(alpha = 0.82f),
        surfaceContainerHigh = panelHigh,
        onSurfaceContainerHigh = white,
        surfaceContainerHighest = panelHighest,
        onSurfaceContainerHighest = white,
        outline = white.copy(alpha = 0.2f),
        dividerLine = white.copy(alpha = 0.12f),
        windowDimming = black.copy(alpha = 0.6f),
        sliderBackground = white.copy(alpha = 0.12f),
        sliderKeyPoint = white,
        sliderKeyPointForeground = black,
    )
}

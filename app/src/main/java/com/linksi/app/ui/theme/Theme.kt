package com.linksi.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Clean Neutral Color Schemes (No Amber/Brown) ──────────
private val DarkColorScheme = darkColorScheme(
    primary          = Color(0xFFD1E4FF), // Light Blue-White for high contrast icons/text
    onPrimary        = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary        = Color(0xFFD1E4FF),
    onSecondary      = Color(0xFF003258),
    tertiary         = Color(0xFFD1E4FF),
    background       = Color(0xFF0F0F0F), // Clean Neutral Dark
    surface          = Color(0xFF0F0F0F),
    onBackground     = Color.White,       // Pure white text/icons
    onSurface        = Color.White,       // Pure white text/icons
    surfaceVariant   = Color(0xFF1E1E1E), // Dark Gray for cards
    onSurfaceVariant = Color(0xFFE2E2E2),
    outline          = Color(0xFF444444),
    surfaceTint      = Color.Transparent, // Disables color harmonic blending
)

private val LightColorScheme = lightColorScheme(
    primary          = Color(0xFF0061A4), // Professional Blue
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary        = Color(0xFF0061A4),
    onSecondary      = Color.White,
    tertiary         = Color(0xFF0061A4),
    background       = Color(0xFFFAFAFA), // Clean Neutral Light
    surface          = Color(0xFFFAFAFA),
    onBackground     = Color(0xFF1A1C1E),
    onSurface        = Color(0xFF1A1C1E),
    surfaceVariant   = Color(0xFFF2F2F2), // Light Gray for cards
    onSurfaceVariant = Color(0xFF444444),
    outline          = Color(0xFFC4C4C4),
    surfaceTint      = Color.Transparent,
)

@Composable
fun LinksTheme(
    themeMode: String = "system",
    useDynamicColor: Boolean = true,
    useAmoled: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val dynamicScheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (darkTheme && useAmoled) {
                dynamicScheme.copy(
                    background = Color.Black,
                    surface = Color.Black,
                    surfaceVariant = Color(0xFF121212)
                )
            } else {
                dynamicScheme
            }
        }
        darkTheme -> {
            if (useAmoled) {
                DarkColorScheme.copy(
                    background = Color.Black,
                    surface = Color.Black,
                    surfaceVariant = Color(0xFF121212)
                )
            } else {
                DarkColorScheme
            }
        }
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LinksTypography,
        content = content
    )
}

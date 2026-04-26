package com.linksi.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Linksi Brand Palette ─────────────────────────────────────
val LinksiBrandPurple = Color(0xFF6366F1)      // Indigo-500
val LinksiBrandViolet = Color(0xFF8B5CF6)      // Violet-500
val LinksiBrandTeal   = Color(0xFF14B8A6)      // Teal-500
val LinksiAccentPink  = Color(0xFFEC4899)      // Pink-500
val LinksiAccentAmber = Color(0xFFF59E0B)      // Amber-500

private val DarkColorScheme = darkColorScheme(
    primary          = Color(0xFF9B8FFF),
    onPrimary        = Color(0xFF1A0070),
    primaryContainer = Color(0xFF2D1F8C),
    onPrimaryContainer = Color(0xFFE0DEFF),
    secondary        = Color(0xFF6DD5CD),
    onSecondary      = Color(0xFF003733),
    secondaryContainer = Color(0xFF004F4A),
    onSecondaryContainer = Color(0xFF9EF2EA),
    tertiary         = Color(0xFFF48FB1),
    tertiaryContainer= Color(0xFF880E4F),
    background       = Color(0xFF0F0F14),
    onBackground     = Color(0xFFE8E6F0),
    surface          = Color(0xFF1A1824),
    onSurface        = Color(0xFFE8E6F0),
    surfaceVariant   = Color(0xFF252332),
    onSurfaceVariant = Color(0xFFB0ADC4),
    outline          = Color(0xFF4A4760),
    surfaceTint      = Color(0xFF9B8FFF),
    error            = Color(0xFFFF6B6B),
    errorContainer   = Color(0xFF5C1010),
)

private val LightColorScheme = lightColorScheme(
    primary          = Color(0xFF5B4FD4),
    onPrimary        = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEAE5FF),
    onPrimaryContainer = Color(0xFF1A0070),
    secondary        = Color(0xFF008B82),
    onSecondary      = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD0F5F1),
    onSecondaryContainer = Color(0xFF002927),
    tertiary         = Color(0xFFB0005B),
    tertiaryContainer= Color(0xFFFFD9E4),
    background       = Color(0xFFF8F6FF),
    onBackground     = Color(0xFF1A1824),
    surface          = Color(0xFFFFFFFF),
    onSurface        = Color(0xFF1A1824),
    surfaceVariant   = Color(0xFFF0EDFF),
    onSurfaceVariant = Color(0xFF4A4760),
    outline          = Color(0xFFB0ADC4),
    surfaceTint      = Color(0xFF5B4FD4),
)

@Composable
fun LinksTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LinksTypography,
        content = content
    )
}

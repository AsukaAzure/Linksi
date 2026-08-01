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
val LinksiBrandOrange = Color(0xFFF59E0B)      // Amber-500
val LinksiBrandAmber  = Color(0xFFD97706)      // Amber-600
val LinksiBrandWarm   = Color(0xFF78350F)      // Amber-900
val LinksiAccentRed   = Color(0xFFEF4444)      // Red-500
val LinksiAccentGold  = Color(0xFFFBBF24)      // Amber-400

private val DarkColorScheme = darkColorScheme(
    primary          = Color(0xFFFFB74D),
    onPrimary        = Color(0xFF4E2600),
    primaryContainer = Color(0xFF703800),
    onPrimaryContainer = Color(0xFFFFDCC0),
    secondary        = Color(0xFFFFD54F),
    onSecondary      = Color(0xFF452700),
    secondaryContainer = Color(0xFF623A00),
    onSecondaryContainer = Color(0xFFFFE08D),
    tertiary         = Color(0xFFFFAB91),
    tertiaryContainer= Color(0xFF7A2A14),
    background       = Color(0xFF1D1B16),
    onBackground     = Color(0xFFEAE1D4),
    surface          = Color(0xFF1D1B16),
    onSurface        = Color(0xFFEAE1D4),
    surfaceVariant   = Color(0xFF4F4539),
    onSurfaceVariant = Color(0xFFD3C4B4),
    outline          = Color(0xFF9C8F80),
    surfaceTint      = Color(0xFFFFB74D),
    error            = Color(0xFFFFB4AB),
    errorContainer   = Color(0xFF93000A),
)

private val LightColorScheme = lightColorScheme(
    primary          = Color(0xFF8B5000),
    onPrimary        = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDCC0),
    onPrimaryContainer = Color(0xFF2D1600),
    secondary        = Color(0xFF825500),
    onSecondary      = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDDB3),
    onSecondaryContainer = Color(0xFF291800),
    tertiary         = Color(0xFF98452E),
    tertiaryContainer= Color(0xFFFFDAD2),
    background       = Color(0xFFFFF8F1),
    onBackground     = Color(0xFF1F1B16),
    surface          = Color(0xFFFFF8F1),
    onSurface        = Color(0xFF1F1B16),
    surfaceVariant   = Color(0xFFF0E0CF),
    onSurfaceVariant = Color(0xFF4F4539),
    outline          = Color(0xFF817567),
    surfaceTint      = Color(0xFF8B5000),
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

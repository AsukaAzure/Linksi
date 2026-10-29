package com.linksi.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── Monochrome Palette (Pure Black & White) ─────────────────────
// Dark = true black with white accents; Light = white with black
// accents. Error stays red everywhere — it is a functional signal,
// not part of the brand look.

// Light: black ink on white
val PrimaryLight = Color(0xFF101010)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFE8E8E8)
val OnPrimaryContainerLight = Color(0xFF101010)

val SecondaryLight = Color(0xFF555555)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFE3E3E3)
val OnSecondaryContainerLight = Color(0xFF1B1B1B)

val TertiaryLight = Color(0xFF444444)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFE0E0E0)
val OnTertiaryContainerLight = Color(0xFF1C1C1C)

// Dark: white ink on true black
val PrimaryDark = Color(0xFFF5F5F5)
val OnPrimaryDark = Color(0xFF101010)
val PrimaryContainerDark = Color(0xFF2C2C2C)
val OnPrimaryContainerDark = Color(0xFFF0F0F0)

val SecondaryDark = Color(0xFFC6C6C6)
val OnSecondaryDark = Color(0xFF1E1E1E)
val SecondaryContainerDark = Color(0xFF333333)
val OnSecondaryContainerDark = Color(0xFFE3E3E3)

val TertiaryDark = Color(0xFFBBBBBB)
val OnTertiaryDark = Color(0xFF222222)
val TertiaryContainerDark = Color(0xFF303030)
val OnTertiaryContainerDark = Color(0xFFDDDDDD)

// Neutrals — Light
val BackgroundLight = Color(0xFFFFFFFF)
val OnBackgroundLight = Color(0xFF171717)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF171717)
val SurfaceVariantLight = Color(0xFFEFEFEF)
val OnSurfaceVariantLight = Color(0xFF575757)
val OutlineLight = Color(0xFF7A7A7A)

// Neutrals — Dark (true black)
val BackgroundDark = Color(0xFF000000)
val OnBackgroundDark = Color(0xFFF0F0F0)
val SurfaceDark = Color(0xFF000000)
val OnSurfaceDark = Color(0xFFF0F0F0)
val SurfaceVariantDark = Color(0xFF242424)
val OnSurfaceVariantDark = Color(0xFFB0B0B0)
val OutlineDark = Color(0xFF8A8A8A)

// Full surface ramp so every M3 component (cards, sheets, dialogs,
// menus) stays in the same monochrome family — Light
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFF8F8F8)
val SurfaceContainerLight = Color(0xFFF3F3F3)
val SurfaceContainerHighLight = Color(0xFFEDEDED)
val SurfaceContainerHighestLight = Color(0xFFE7E7E7)
val SurfaceDimLight = Color(0xFFDDDDDD)
val SurfaceBrightLight = Color(0xFFFFFFFF)
val InverseSurfaceLight = Color(0xFF2A2A2A)
val InverseOnSurfaceLight = Color(0xFFF5F5F5)
val InversePrimaryLight = Color(0xFFF5F5F5)
val OutlineVariantLight = Color(0xFFDADADA)

// Full surface ramp — Dark
val SurfaceContainerLowestDark = Color(0xFF0A0A0A)
val SurfaceContainerLowDark = Color(0xFF161616)
val SurfaceContainerDark = Color(0xFF1C1C1C)
val SurfaceContainerHighDark = Color(0xFF262626)
val SurfaceContainerHighestDark = Color(0xFF313131)
val SurfaceDimDark = Color(0xFF000000)
val SurfaceBrightDark = Color(0xFF3A3A3A)
val InverseSurfaceDark = Color(0xFFF0F0F0)
val InverseOnSurfaceDark = Color(0xFF1C1C1C)
val InversePrimaryDark = Color(0xFF101010)
val OutlineVariantDark = Color(0xFF363636)

// Special: Error (kept functional red) & Success
val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)

val SuccessGreen = Color(0xFF22C55E)
val OnSuccessGreen = Color(0xFFFFFFFF)

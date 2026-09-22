package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

import androidx.compose.material3.ProvideTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

val MushafPaperColorScheme = lightColorScheme(
    primary = Color(0xFF0F5132), // Royal Islamic Emerald
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E7DD),
    onPrimaryContainer = Color(0xFF082E1C),
    secondary = Color(0xFFC59B27), // Islamic Gold
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF3CD),
    onSecondaryContainer = Color(0xFF523E02),
    tertiary = GoldAccent,
    background = Color(0xFFFAF6EE), // Warm Cream Paper
    onBackground = Color(0xFF1C221F),
    surface = Color(0xFFFFFDF8),    // Crisp Parchment Surface
    onSurface = Color(0xFF1A221E),
    surfaceVariant = Color(0xFFF2ECE0), // Card/Border Variant
    onSurfaceVariant = Color(0xFF4A443C),
    outline = Color(0xFFDFD5C6)
)

private val LightColorScheme = MushafPaperColorScheme

private val SepiaColorScheme = darkColorScheme(
    primary = SepiaAccent,
    onPrimary = Color(0xFF2B1A08),
    primaryContainer = Color(0xFF4A3319),
    onPrimaryContainer = Color(0xFFFCECD5),
    secondary = Color(0xFFD4A373),
    onSecondary = Color(0xFF231508),
    secondaryContainer = Color(0xFF523922),
    onSecondaryContainer = Color(0xFFF7E2CC),
    tertiary = Color(0xFFFAEDCD),
    background = SepiaBackground,
    onBackground = OnSepiaText,
    surface = SepiaSurface,
    onSurface = OnSepiaText,
    surfaceVariant = SepiaSurfaceVariant,
    onSurfaceVariant = Color(0xFFE2D0BE),
    outline = Color(0xFF6B4F3B)
)

private val DarkColorScheme = darkColorScheme(
    primary = TrueDarkPrimary, // Vivid High-Contrast Emerald Green
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = TrueDarkSecondary, // Vivid High-Contrast Islamic Gold
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF78350F),
    onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = Color(0xFF34D399),
    background = TrueDarkBackground, // Pure OLED Deep Black #000000
    onBackground = TrueDarkOnBackground, // Crisp High Contrast White #FFFFFF
    surface = TrueDarkSurface, // Deep OLED Black Surface #121212
    onSurface = TrueDarkOnSurface, // Crisp High Contrast White #FFFFFF
    surfaceVariant = TrueDarkSurfaceVariant, // Elevated Charcoal Card #1E1E1E
    onSurfaceVariant = Color(0xFFE5E7EB), // High contrast readable secondary text
    outline = TrueDarkOutline,
    outlineVariant = TrueDarkOutlineVariant
)

@Composable
fun HolyQuranTheme(
    themeMode: String = "LIGHT",
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val colorScheme = when (themeMode) {
        "LIGHT" -> LightColorScheme
        "MUSHAF" -> MushafPaperColorScheme
        "SEPIA" -> SepiaColorScheme
        "DARK" -> DarkColorScheme
        "SYSTEM" -> if (isSystemDark) DarkColorScheme else LightColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography
    ) {
        ProvideTextStyle(
            value = TextStyle(
                fontFamily = TajawalFontFamily,
                fontSize = 15.sp,
                lineHeight = 22.sp
            ),
            content = content
        )
    }
}


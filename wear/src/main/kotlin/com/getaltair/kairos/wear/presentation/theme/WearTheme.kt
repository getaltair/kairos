package com.getaltair.kairos.wear.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

/**
 * OLED-optimized Kairos color scheme for WearOS.
 * Uses true black background to save battery on OLED watch displays.
 * Colors adapted from the phone app's purple-based palette.
 */
val KairosWearColorScheme = ColorScheme(
    primary = Color(0xFF6750A4),
    primaryDim = Color(0xFF4F378B),
    primaryContainer = Color(0xFFEADDFF),
    onPrimary = Color(0xFFFFFFFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    secondaryDim = Color(0xFF4A4458),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondary = Color(0xFFFFFFFF),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    tertiaryDim = Color(0xFF633B48),
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiary = Color(0xFFFFFFFF),
    onTertiaryContainer = Color(0xFF31111D),
    surfaceContainerLow = Color(0xFF1C1B1F),
    surfaceContainer = Color(0xFF211F26),
    surfaceContainerHigh = Color(0xFF2B2930),
    onSurface = Color(0xFFE6E1E5),
    onSurfaceVariant = Color(0xFF938F99),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFF49454F),
    background = Color(0xFF000000),
    onBackground = Color(0xFFE6E1E5),
    error = Color(0xFFB3261E),
    errorDim = Color(0xFF8C1D18),
    errorContainer = Color(0xFFF9DEDC),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFF410E0B),
)

@Composable
fun KairosWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KairosWearColorScheme,
        content = content,
    )
}

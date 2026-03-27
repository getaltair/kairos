package com.getaltair.kairos.dashboard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------
// DESIGN.md color palette
// ---------------------------------------------------------------------------

/** Base background -- Deep Muted Teal-Navy */
val Background = Color(0xFF0C0E10)

/** Panel background -- Shadowed Teal-Base */
val Surface = Color(0xFF111416)

/** Slightly elevated -- Surface Container */
val SurfaceVariant = Color(0xFF171A1D)

/** Primary action -- Soft Bright Teal */
val Primary = Color(0xFF8EF4E9)

/** Secondary -- Muted Indigo-Lavender */
val Secondary = Color(0xFFBAC3FF)

/** Tertiary -- Warm Sunset Amber */
val Tertiary = Color(0xFFFFC87F)

/** Primary text -- Soft Off-White */
val OnBackground = Color(0xFFE3E6EA)

/** Error -- Soft Coral Red */
val Error = Color(0xFFFF716C)

/** On-primary -- Deep Forest Green (readable on teal) */
val OnPrimary = Color(0xFF003735)

/** On-secondary -- Deep Navy (readable on indigo) */
val OnSecondary = Color(0xFF212479)

/** On-error -- Dark maroon (readable on coral) */
val OnError = Color(0xFF690005)

/** Muted text -- Muted Teal-Sage */
val OnSurfaceVariant = Color(0xFFA8ABB0)

// ---------------------------------------------------------------------------
// Color scheme
// ---------------------------------------------------------------------------

val DashboardColorScheme = darkColorScheme(
    background = Background,
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    primary = Primary,
    onPrimary = OnPrimary,
    secondary = Secondary,
    onSecondary = OnSecondary,
    tertiary = Tertiary,
    error = Error,
    onError = OnError,
    onBackground = OnBackground,
    onSurface = OnBackground,
    onSurfaceVariant = OnSurfaceVariant,
)

// ---------------------------------------------------------------------------
// Typography -- scaled for kiosk readability at 3-4 feet
// ---------------------------------------------------------------------------

val DashboardTypography = Typography(
    bodySmall = TextStyle(
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    bodyLarge = TextStyle(
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    titleSmall = TextStyle(
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Medium,
    ),
    titleMedium = TextStyle(
        fontSize = 36.sp,
        lineHeight = 44.sp,
        fontWeight = FontWeight.Medium,
    ),
    titleLarge = TextStyle(
        fontSize = 40.sp,
        lineHeight = 48.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    displaySmall = TextStyle(
        fontSize = 48.sp,
        lineHeight = 56.sp,
        fontWeight = FontWeight.Bold,
    ),
    headlineMedium = TextStyle(
        fontSize = 52.sp,
        lineHeight = 60.sp,
        fontWeight = FontWeight.Bold,
    ),
)

// ---------------------------------------------------------------------------
// Theme composable
// ---------------------------------------------------------------------------

@Composable
fun DashboardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DashboardColorScheme,
        typography = DashboardTypography,
        content = content,
    )
}

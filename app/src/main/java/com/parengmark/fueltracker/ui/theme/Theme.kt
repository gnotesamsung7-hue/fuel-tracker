package com.parengmark.fueltracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = GreenPrimary,
    primaryContainer = GreenPrimaryContainer,
    secondary = GreenSecondary,
    tertiary = AmberAccent
)

private val DarkColors = darkColorScheme(
    primary = GreenPrimaryDark,
    primaryContainer = GreenPrimaryContainerDark,
    secondary = GreenPrimaryDark,
    tertiary = AmberAccent
)

/**
 * Instrument-cluster look: near-black panels, amber backlight, cyan readouts,
 * red redline. The background is transparent on purpose -- every screen's
 * Scaffold uses colorScheme.background, so the [DashboardBackdrop] drawn
 * behind the whole app shows through without touching each screen.
 */
private val DashboardColors = darkColorScheme(
    primary = DashAmber,
    onPrimary = Color(0xFF1F1400),
    primaryContainer = Color(0xFF3A2C10),
    onPrimaryContainer = Color(0xFFFFE0B2),
    secondary = DashCyan,
    onSecondary = Color(0xFF002024),
    secondaryContainer = Color(0xFF0F3A40),
    onSecondaryContainer = Color(0xFFB2EBF2),
    tertiary = DashRed,
    onTertiary = Color.White,
    background = Color.Transparent,
    onBackground = Color(0xFFE8ECF2),
    surface = Color(0xFF12161D),
    onSurface = Color(0xFFE8ECF2),
    surfaceVariant = Color(0xFF232A35),
    onSurfaceVariant = Color(0xFF9BA6B5),
    surfaceTint = DashAmber,
    surfaceContainerLowest = Color(0xFF0B0E13),
    surfaceContainerLow = Color(0xFF151A22),
    surfaceContainer = Color(0xFF1A2029),
    surfaceContainerHigh = Color(0xFF1F2631),
    surfaceContainerHighest = Color(0xFF252D39),
    outline = Color(0xFF465163),
    outlineVariant = Color(0xFF2C3440),
    error = Color(0xFFFF6E6E),
    onError = Color(0xFF2B0000),
    inverseSurface = Color(0xFFE8ECF2),
    inverseOnSurface = Color(0xFF12161D),
    scrim = Color.Black
)

@Composable
fun FuelTrackerTheme(
    mode: ThemeMode = ThemeMode.CLASSIC,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = when {
        mode == ThemeMode.DASHBOARD -> DashboardColors
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}

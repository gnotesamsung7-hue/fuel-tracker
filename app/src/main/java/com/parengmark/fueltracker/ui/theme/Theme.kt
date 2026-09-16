package com.parengmark.fueltracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

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

@Composable
fun FuelTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}

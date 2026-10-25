package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DinoPlanetColorScheme = darkColorScheme(
    primary = JungleGreen,
    onPrimary = PrehistoricDark,
    primaryContainer = DeepForest,
    onPrimaryContainer = GoldenReward,
    secondary = GoldenReward,
    onSecondary = PrehistoricDark,
    secondaryContainer = SurfaceElevated,
    onSecondaryContainer = GoldenReward,
    tertiary = VolcanicOrange,
    onTertiary = PrehistoricDark,
    background = PrehistoricDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorder,
    error = DangerRed,
    onError = TextPrimary
)

@Composable
fun DinoPlanetTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DinoPlanetColorScheme,
        typography = Typography,
        content = content
    )
}

// Retain alias for any existing code
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    DinoPlanetTheme(content = content)
}

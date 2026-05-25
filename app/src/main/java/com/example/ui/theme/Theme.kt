package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AccentGreen,
    onPrimary = BackgroundDark,
    secondary = AccentPurple,
    onSecondary = BackgroundDark,
    tertiary = AccentCyan,
    onTertiary = BackgroundDark,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceLighter,
    onSurfaceVariant = TextPrimary,
    error = TextError,
    onError = TextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for terminal IDE feel
    dynamicColor: Boolean = false, // Disable dynamic colors to keep terminal aesthetic integer
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

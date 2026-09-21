package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = StudioCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF0C4A6E),
    onPrimaryContainer = Color(0xFFBAE6FD),
    secondary = StudioIndigo,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF312E81),
    onSecondaryContainer = Color(0xFFE0E7FF),
    tertiary = StudioEmerald,
    onTertiary = Color.Black,
    background = StudioDarkBg,
    onBackground = DarkTextPrimary,
    surface = StudioDarkBg,
    onSurface = DarkTextPrimary,
    surfaceVariant = StudioDarkSurface,
    onSurfaceVariant = DarkTextSecondary,
    surfaceContainer = StudioDarkSurface,
    surfaceContainerHigh = StudioDarkSurfaceHigh,
    surfaceContainerLow = Color(0xFF0D1320),
    outline = StudioDarkBorder,
    error = StudioRose,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}


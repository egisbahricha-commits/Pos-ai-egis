package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Sapphire500,
    onPrimary = Color.White,
    primaryContainer = Sapphire800,
    onPrimaryContainer = Sapphire100,
    secondary = Emerald500,
    onSecondary = Color.White,
    secondaryContainer = Emerald800,
    onSecondaryContainer = Emerald100,
    tertiary = Amber400,
    background = Slate900,
    surface = Slate800,
    surfaceVariant = Slate700,
    onBackground = Slate50,
    onSurface = Slate50,
    onSurfaceVariant = Slate200,
    error = Rose500,
    outline = Slate600
)

private val LightColorScheme = lightColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    primaryContainer = AccentBlueSoft,
    onPrimaryContainer = AccentBlue,
    secondary = Emerald700,
    onSecondary = Color.White,
    secondaryContainer = Emerald100,
    onSecondaryContainer = Emerald900,
    tertiary = Amber500,
    background = BgCanvas,
    surface = SurfaceWhite,
    surfaceVariant = SurfaceStrong,
    onBackground = Slate900,
    onSurface = Slate900,
    onSurfaceVariant = Slate500,
    error = Rose500,
    outline = BorderInput
)

@Composable
fun GeraiUnaTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

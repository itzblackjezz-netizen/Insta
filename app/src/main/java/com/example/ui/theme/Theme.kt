package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = IgBlue,
    onPrimary = Color.White,
    secondary = IgPink,
    onSecondary = Color.White,
    tertiary = IgOrange,
    background = IgDarkBackground,
    onBackground = IgDarkTextPrimary,
    surface = IgDarkSurface,
    onSurface = IgDarkTextPrimary,
    surfaceVariant = IgDarkSurfaceVariant,
    onSurfaceVariant = IgDarkTextSecondary,
    outline = IgDarkBorder,
    outlineVariant = IgDarkDivider
)

private val LightColorScheme = lightColorScheme(
    primary = IgBlue,
    onPrimary = Color.White,
    secondary = IgPink,
    onSecondary = Color.White,
    tertiary = IgOrange,
    background = IgLightBackground,
    onBackground = IgLightTextPrimary,
    surface = IgLightSurface,
    onSurface = IgLightTextPrimary,
    surfaceVariant = IgLightSurfaceVariant,
    onSurfaceVariant = IgLightTextSecondary,
    outline = IgLightBorder,
    outlineVariant = IgLightDivider
)

@Composable
fun InstaSnapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

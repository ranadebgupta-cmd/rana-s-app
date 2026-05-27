package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CyberNeonGreen,
    secondary = CyberNeonCyan,
    tertiary = CyberNeonGreen,
    background = CyberDarkBg,
    surface = CyberDarkSurface,
    onPrimary = CyberDarkBg,
    onSecondary = CyberDarkBg,
    onBackground = CyberWhite,
    onSurface = CyberWhite,
    error = CyberWarningRed
)

private val LightColorScheme = lightColorScheme(
    primary = CyberDarkCard,
    secondary = CyberNeonCyan,
    tertiary = CyberNeonGreen,
    background = CyberWhite,
    surface = CyberWhite,
    onPrimary = CyberWhite,
    onSecondary = CyberDarkBg,
    onBackground = CyberDarkBg,
    onSurface = CyberDarkBg,
    error = CyberWarningRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

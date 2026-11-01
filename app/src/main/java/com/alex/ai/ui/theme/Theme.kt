package com.alex.ai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = MarigoldAccent,
    onPrimary = DeepIndigoNight,
    secondary = CyanGlow,
    onSecondary = DeepIndigoNight,
    tertiary = ActionGreen,
    background = DeepIndigoNight,
    surface = IndigoSurface,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun AlexTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}

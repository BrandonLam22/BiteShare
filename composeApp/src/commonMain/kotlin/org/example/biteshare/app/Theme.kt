package org.example.biteshare.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Orange = Color(0xFFFF6B2D)
private val Cream = Color(0xFFFFF4EE)
private val Ink = Color(0xFF1F1F1F)

private val LightColors = lightColorScheme(
    primary = Orange,
    onPrimary = Color.White,
    background = Cream,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
)

private val DarkColors = darkColorScheme(
    primary = Orange,
    onPrimary = Color.Black,
)

@Composable
fun PickMeTheme(
    dark: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content
    )
}

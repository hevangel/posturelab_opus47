package com.posturelab.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Brand {
    val Primary = Color(0xFF1B4F8C)
    val PrimaryDark = Color(0xFF143C6B)
    val PrimaryLight = Color(0xFF3FA9D6)
    val Band = Color(0xFF1B4F8C)
    val Row = Color(0xFFE8F0F8)
    val OnPrimary = Color(0xFFFFFFFF)
    val Body = Color(0xFF222222)
}

private val LightColors = lightColorScheme(
    primary = Brand.Primary,
    onPrimary = Brand.OnPrimary,
    secondary = Brand.PrimaryLight,
    onSecondary = Brand.OnPrimary,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onSurface = Brand.Body,
)

private val DarkColors = darkColorScheme(
    primary = Brand.PrimaryLight,
    onPrimary = Brand.PrimaryDark,
)

@Composable
fun PostureLabTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}

package com.commissioning.cctv

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Navy = Color(0xFF0B3C5D)
val Green = Color(0xFF2E9E5B)
val Red = Color(0xFFD64545)
val Amber = Color(0xFFE29A1A)
val Grey = Color(0xFF8A94A0)

private val colors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    secondary = Green,
    background = Color(0xFFF4F6F8),
    surface = Color.White,
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}

package com.snaptask.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ColorBg           = Color(0xFF080810)
val ColorSurface      = Color(0xFF0F0F1A)
val ColorSurfaceHigh  = Color(0xFF16162A)
val ColorAccent       = Color(0xFF6366F1)
val ColorAccentLight  = Color(0xFF818CF8)
val ColorMuted        = Color(0xFF9CA3AF)
val ColorBorder       = Color(0xFF1F1F30)

private val DarkColorScheme = darkColorScheme(
    primary          = ColorAccentLight,
    onPrimary        = Color.White,
    background       = ColorBg,
    onBackground     = Color.White,
    surface          = ColorSurface,
    onSurface        = Color.White,
    surfaceVariant   = ColorSurfaceHigh,
    onSurfaceVariant = ColorMuted,
    outline          = ColorBorder,
    outlineVariant   = ColorBorder,
)

@Composable
fun SnapTaskTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content     = content
    )
}

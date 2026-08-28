package com.example.liquidglassdemo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DemoColorScheme = darkColorScheme(
    primary = Color(0xFF7E7FEA),
    onPrimary = Color.White,
    background = Color(0xFF0E0E16),
    onBackground = Color.White,
    surface = Color(0xFF15151A),
    onSurface = Color.White,
)

/**
 * Liquid Glass Demo 主題：固定深色，讓動態光照 Tab Bar 的光影對比更明顯。
 */
@Composable
fun LiquidGlassDemoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DemoColorScheme,
        content = content
    )
}
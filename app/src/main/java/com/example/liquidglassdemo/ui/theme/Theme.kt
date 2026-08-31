package com.example.liquidglassdemo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DemoDarkColorScheme = darkColorScheme(
    primary = Color(0xFF7E7FEA),
    onPrimary = Color.White,
    background = Color(0xFF0E0E16),
    onBackground = Color.White,
    surface = Color(0xFF15151A),
    onSurface = Color.White,
)

private val DemoLightColorScheme = lightColorScheme(
    primary = Color(0xFF00A15C),
    onPrimary = Color.White,
    background = Color(0xFFE3E3E3),
    onBackground = Color(0xFF111111),
    surface = Color(0xFFF7F7F7),
    onSurface = Color(0xFF111111),
)

/**
 * Liquid Glass Demo 主題。
 *
 * 深淺要跟著玻璃主題走：Native 模式下的元件是 Material3 原生渲染，若這裡固定深色，
 * 切到淺色的 Neutral 主題就會變成深色元件壓在淺灰頁面上。
 *
 * @param lightSurface 目前的玻璃主題是不是淺色表面
 * @param content 內容
 */
@Composable
fun LiquidGlassDemoTheme(
    lightSurface: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (lightSurface) DemoLightColorScheme else DemoDarkColorScheme,
        content = content
    )
}

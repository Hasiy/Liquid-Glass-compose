package com.liquidglass.core.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.liquidglass.core.blur.BlurStrategy

/** SPEC-02 §2：全局模糊策略开关 */
val LocalBlurStrategy = staticCompositionLocalOf { BlurStrategy.RENDER_EFFECT }

val LocalGlassPalette = staticCompositionLocalOf { GlassPalette.Dark }

@Composable
fun LiquidGlassTheme(
    blurStrategy: BlurStrategy = BlurStrategy.RENDER_EFFECT,
    palette: GlassPalette = GlassPalette.Dark,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalBlurStrategy provides blurStrategy,
        LocalGlassPalette provides palette,
        content = content,
    )
}

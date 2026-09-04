/*
 * Copyright 2026 FitDash contributors.
 */
package top.hasiy.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import top.hasiy.designsystem.tokens.GlassThemeSpec
import top.hasiy.designsystem.tokens.GlassVisualStyle

/** 当前 Compose 树使用的玻璃风格配置。 */
val LocalGlassConfig = staticCompositionLocalOf { GlassPresets.Drop }

/** 当前 Compose 树使用的跨平台主题规格。 */
val LocalGlassThemeSpec = staticCompositionLocalOf {
    GlassThemeSpec.default(GlassVisualStyle.DROP)
}

/**
 * 将跨平台主题规格接入 Material3 和 Liquid Glass 控件。
 *
 * 页面只需要在根节点调用一次；控件仍可通过显式 [GlassConfig] 覆盖局部视觉参数。
 * Android 的模糊和阴影实现仍留在当前模块，主题规格本身不依赖 Android 资源。
 *
 * @param spec 当前主题规格。
 * @param content 页面内容。
 */
@Composable
fun DesignSystemTheme(
    spec: GlassThemeSpec = GlassThemeSpec.default(GlassVisualStyle.DROP),
    content: @Composable () -> Unit,
) {
    val config = spec.toGlassConfig()
    val colorScheme = spec.toMaterialColorScheme()
    CompositionLocalProvider(
        LocalGlassConfig provides config,
        LocalGlassThemeSpec provides spec,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}

/** 将跨平台主题令牌转换为当前 Android Compose 的玻璃配置。 */
fun GlassThemeSpec.toGlassConfig(): GlassConfig {
    return when (visualStyle) {
        GlassVisualStyle.DROP -> GlassPresets.Drop
        GlassVisualStyle.NEUTRAL -> GlassPresets.Neutral
        GlassVisualStyle.DARK -> GlassPresets.Dark
        GlassVisualStyle.NATIVE -> GlassPresets.Native
    }.copy(
        baseColor = glassBase.toComposeColor(),
        contentColor = glassContent.toComposeColor(),
        highlightColor = glassHighlight.toComposeColor(),
        highlightBlendMode = if (isLight) BlendMode.Multiply else BlendMode.Screen,
        borderColor = glassBorder.toComposeColor(),
        accentColor = accent.toComposeColor(),
        accentEnabled = accentEnabled,
        pageBackgroundTop = backgroundTop.toComposeColor(),
        pageBackgroundBottom = backgroundBottom.toComposeColor(),
        native = visualStyle == GlassVisualStyle.NATIVE,
    )
}

/** 将跨平台主题令牌转换为 Material3 颜色方案。 */
fun GlassThemeSpec.toMaterialColorScheme() = if (isLight) {
    lightColorScheme(
        primary = primary.toComposeColor(),
        onPrimary = onPrimary.toComposeColor(),
        secondary = secondary.toComposeColor(),
        background = backgroundTop.toComposeColor(),
        onBackground = onSurface.toComposeColor(),
        surface = surface.toComposeColor(),
        onSurface = onSurface.toComposeColor(),
    )
} else {
    darkColorScheme(
        primary = primary.toComposeColor(),
        onPrimary = onPrimary.toComposeColor(),
        secondary = secondary.toComposeColor(),
        background = backgroundBottom.toComposeColor(),
        onBackground = onSurface.toComposeColor(),
        surface = surface.toComposeColor(),
        onSurface = onSurface.toComposeColor(),
    )
}

private fun Long.toComposeColor(): Color = Color(toULong())
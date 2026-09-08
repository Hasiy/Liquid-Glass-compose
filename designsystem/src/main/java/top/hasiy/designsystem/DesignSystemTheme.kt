/*
 * Copyright 2026 FitDash contributors.
 */
package top.hasiy.designsystem

import androidx.compose.material3.ColorScheme
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
import top.hasiy.designsystem.tokens.ThemePalette

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
    return GlassPresets.forVisualStyle(visualStyle).copy(
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

/**
 * 将一组主题色接入 Material3 和 Liquid Glass 控件。
 *
 * 这是参考设计稿的 8 组主题在页面根节点的入口；与 [DesignSystemTheme] 的
 * [GlassThemeSpec] 版本相比，额外把语义色（强调色的浅／深变体、轨道、危险态、
 * 面板层级）一并带进 [LocalGlassConfig]，控件才不必各自猜这些颜色。
 *
 * @param palette 当前主题色。
 * @param content 页面内容。
 */
@Composable
fun DesignSystemTheme(
    palette: ThemePalette,
    content: @Composable () -> Unit,
) {
    val config = palette.toGlassConfig()
    val colorScheme = palette.toMaterialColorScheme()
    CompositionLocalProvider(
        LocalGlassConfig provides config,
        LocalGlassThemeSpec provides GlassThemeSpec.fromPalette(palette),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}

/**
 * 由一组主题色构造 Compose 玻璃配置。
 *
 * 结构参数来自 [ThemePalette.visualStyle] 对应的预设，颜色全部来自 [ThemePalette]，
 * 并把完整语义色挂到 [GlassConfig.palette] 上。
 */
fun ThemePalette.toGlassConfig(): GlassConfig =
    GlassThemeSpec.fromPalette(this).toGlassConfig().copy(palette = toGlassPalette())

/**
 * 由一组主题色构造 Material3 颜色方案。
 *
 * 比 [GlassThemeSpec.toMaterialColorScheme] 多补了 `tertiary`、`error`、
 * `surfaceVariant`、`outline`——参考稿的环境色、危险态、面板层级与分隔线都有明确取值，
 * 交给 Material 自己推导会得出跟设计稿无关的颜色。
 */
fun ThemePalette.toMaterialColorScheme(): ColorScheme {
    val accentColor = accent.toComposeColor()
    val onAccentColor = onAccent.toComposeColor()
    val dangerColor = danger.toComposeColor()
    val screenColor = screen.toComposeColor()
    val contentColor = screenContent.toComposeColor()
    val lineColor = line.toComposeColor()
    val dangerContainer = dangerColor.copy(alpha = DANGER_CONTAINER_ALPHA)
    // 危险态的字压在深底上时用亮红本色；浅色主题的实心危险底要配白字。
    val onDanger = if (isLight) Color.White else screenColor
    return if (isLight) {
        lightColorScheme(
            primary = accentColor,
            onPrimary = onAccentColor,
            secondary = accentLight.toComposeColor(),
            onSecondary = onAccentColor,
            tertiary = ambient.toComposeColor(),
            onTertiary = onAccentColor,
            background = screenColor,
            onBackground = contentColor,
            surface = panel.toComposeColor(),
            onSurface = contentColor,
            surfaceVariant = lift.toComposeColor(),
            onSurfaceVariant = muted.toComposeColor(),
            outline = lineColor,
            outlineVariant = lineColor,
            error = dangerColor,
            onError = onDanger,
            errorContainer = dangerContainer,
            onErrorContainer = dangerColor,
        )
    } else {
        darkColorScheme(
            primary = accentColor,
            onPrimary = onAccentColor,
            secondary = accentLight.toComposeColor(),
            onSecondary = onAccentColor,
            tertiary = ambient.toComposeColor(),
            onTertiary = onAccentColor,
            background = screenColor,
            onBackground = contentColor,
            surface = panel.toComposeColor(),
            onSurface = contentColor,
            surfaceVariant = lift.toComposeColor(),
            onSurfaceVariant = muted.toComposeColor(),
            outline = lineColor,
            outlineVariant = lineColor,
            error = dangerColor,
            onError = onDanger,
            errorContainer = dangerContainer,
            onErrorContainer = dangerColor,
        )
    }
}

/** 危险态容器底的透明度：参考稿的断连／拒绝底是同色淡底 */
private const val DANGER_CONTAINER_ALPHA = 0.13f

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

/**
 * 把无符号 ARGB 令牌转成 Compose 颜色。
 *
 * 必须走 `Color(Long)` 这个重载——它才把参数当 `0xAARRGGBB` 解读。
 * `Color(ULong)` 是 value class 的主构造，收的是 Compose 的**内部打包格式**
 * （高 32 位 RGBA，低 6 位是 color space id）。把 ARGB 直接塞进去，
 * color space id 会取到 ARGB 的低 6 位，例如 `0xFFFF6969 and 0x3F = 41`，
 * 而 `ColorSpaces` 只有 18 项，任何读 color space 的操作（`copy`、`luminance`）
 * 都会抛 ArrayIndexOutOfBoundsException。
 */
private fun Long.toComposeColor(): Color = Color(this)
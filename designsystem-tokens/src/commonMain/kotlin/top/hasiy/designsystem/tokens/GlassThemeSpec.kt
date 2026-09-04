/*
 * Copyright 2026 FitDash contributors.
 */
package top.hasiy.designsystem.tokens

/** Liquid Glass 的视觉结构预设。 */
enum class GlassVisualStyle {
    /** 深色水滴玻璃。 */
    DROP,

    /** 浅色中性玻璃。 */
    NEUTRAL,

    /** 深色玻璃。 */
    DARK,

    /** 使用 Material3 原生控件。 */
    NATIVE,
}

/**
 * 跨平台主题规格。
 *
 * 颜色使用无符号 ARGB 32 位值保存，避免主题令牌依赖 Android 资源或平台 UI 类型。
 * Android/iOS 的渲染层负责将这些值转换成对应的 Compose/原生颜色类型。
 *
 * @param id 稳定主题标识，只用于持久化和诊断，不应使用显示文案。
 * @param visualStyle 玻璃视觉结构预设。
 * @param isLight 是否使用浅色表面语义。
 * @param primary 主操作颜色。
 * @param onPrimary 主操作上的前景色。
 * @param secondary 次级强调色。
 * @param backgroundTop 页面背景渐变起点。
 * @param backgroundBottom 页面背景渐变终点。
 * @param surface Material3 surface 颜色。
 * @param onSurface surface 上的前景色。
 * @param glassBase 玻璃表面基色。
 * @param glassContent 玻璃表面的默认内容色。
 * @param glassHighlight 玻璃柔光颜色。
 * @param glassBorder 玻璃描边颜色。
 * @param accent 强调色，用于选中态和填充段。
 * @param accentEnabled 是否启用强调色语义。
 */
data class GlassThemeSpec(
    val id: String,
    val visualStyle: GlassVisualStyle,
    val isLight: Boolean,
    val primary: Long,
    val onPrimary: Long,
    val secondary: Long,
    val backgroundTop: Long,
    val backgroundBottom: Long,
    val surface: Long,
    val onSurface: Long,
    val glassBase: Long,
    val glassContent: Long,
    val glassHighlight: Long,
    val glassBorder: Long,
    val accent: Long,
    val accentEnabled: Boolean,
) {
    companion object {
        private const val WHITE = 0xFFFFFFFFL
        private const val BLACK = 0xFF000000L
        private const val GREEN = 0xFF00A15CL

        /** 返回 SDK 自带视觉预设，供示例和无品牌应用使用。 */
        fun default(style: GlassVisualStyle): GlassThemeSpec = when (style) {
            GlassVisualStyle.DROP -> GlassThemeSpec(
                id = "drop",
                visualStyle = style,
                isLight = false,
                primary = 0xFF7E7FEAL,
                onPrimary = WHITE,
                secondary = 0xFF6FD3E8L,
                backgroundTop = 0xFF2A1630L,
                backgroundBottom = 0xFF0B0D1AL,
                surface = 0xFF15151AL,
                onSurface = WHITE,
                glassBase = 0xFF9A9AA8L,
                glassContent = WHITE,
                glassHighlight = WHITE,
                glassBorder = WHITE,
                accent = GREEN,
                accentEnabled = false,
            )

            GlassVisualStyle.NEUTRAL -> GlassThemeSpec(
                id = "neutral",
                visualStyle = style,
                isLight = true,
                primary = GREEN,
                onPrimary = WHITE,
                secondary = 0xFF72C7A9L,
                backgroundTop = 0xFFE3E3E3L,
                backgroundBottom = 0xFFD1D1D1L,
                surface = 0xFFF7F7F7L,
                onSurface = 0xFF111111L,
                glassBase = 0xFFF7F7F7L,
                glassContent = 0xFF111111L,
                glassHighlight = BLACK,
                glassBorder = BLACK,
                accent = GREEN,
                accentEnabled = true,
            )

            GlassVisualStyle.DARK -> GlassThemeSpec(
                id = "dark",
                visualStyle = style,
                isLight = false,
                primary = 0xFF7E7FEAL,
                onPrimary = WHITE,
                secondary = 0xFF6FD3E8L,
                backgroundTop = 0xFF2A1630L,
                backgroundBottom = 0xFF0B0D1AL,
                surface = 0xFF15151AL,
                onSurface = WHITE,
                glassBase = 0xFF2E2E3AL,
                glassContent = WHITE,
                glassHighlight = WHITE,
                glassBorder = WHITE,
                accent = GREEN,
                accentEnabled = false,
            )

            GlassVisualStyle.NATIVE -> GlassThemeSpec(
                id = "native",
                visualStyle = style,
                isLight = false,
                primary = 0xFF7E7FEAL,
                onPrimary = WHITE,
                secondary = 0xFF6FD3E8L,
                backgroundTop = 0xFF2A1630L,
                backgroundBottom = 0xFF0B0D1AL,
                surface = 0xFF15151AL,
                onSurface = WHITE,
                glassBase = 0xFFCFCFD8L,
                glassContent = WHITE,
                glassHighlight = WHITE,
                glassBorder = WHITE,
                accent = GREEN,
                accentEnabled = false,
            )
        }
    }
}
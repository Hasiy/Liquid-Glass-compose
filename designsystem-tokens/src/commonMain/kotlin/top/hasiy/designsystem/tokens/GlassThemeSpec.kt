/*
 * Copyright 2026 FitDash contributors.
 */
package top.hasiy.designsystem.tokens

/**
 * Liquid Glass 的视觉结构预设。
 *
 * 这一层只描述材质结构（透明度、柔光、描边、陰影），配色由 [ThemePalette] 决定。
 * 两者分离才能组合出「NEUTRAL 结构 + NORDIC 配色」这类搭配。
 */
enum class GlassVisualStyle {
    /** 深色水滴玻璃。 */
    DROP,

    /** 浅色中性玻璃。 */
    NEUTRAL,

    /** 深色玻璃。 */
    DARK,

    /** 使用 Material3 原生控件。 */
    NATIVE,

    /** 浅色数字仪表：低圆角、实色卡片、七段数码读数，不走玻璃质感。 */
    DIGITAL,

    /** 深色实体按键：内凹底座、凸起胶帽、机械按压反馈。 */
    TACTILE,
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

            // Digital / Tactile 没有「SDK 自带配色」的历史值，配色一律以对应
            // ThemePalette 为唯一来源。
            GlassVisualStyle.DIGITAL -> fromPalette(ThemePalette.Digital)

            GlassVisualStyle.TACTILE -> fromPalette(ThemePalette.Tactile)
        }

        /**
         * 由一组主题色构造完整主题规格。
         *
         * 这是参考设计稿的 8 组配色进入 SDK 的入口：结构参数由
         * [ThemePalette.visualStyle] 对应的预设提供，颜色全部取自 [palette]。
         *
         * 背景渐层两端取同一个 [ThemePalette.screen]：参考稿的屏底是纯色，
         * 上面那层光晕是单独用强调色画的径向渐层，不是背景本身的明度渐变。
         *
         * @param palette 权威配色来源
         */
        fun fromPalette(palette: ThemePalette): GlassThemeSpec = GlassThemeSpec(
            id = palette.id.id,
            visualStyle = palette.visualStyle,
            isLight = palette.isLight,
            primary = palette.accent,
            onPrimary = palette.onAccent,
            secondary = palette.ambient,
            backgroundTop = palette.screen,
            backgroundBottom = palette.screen,
            surface = palette.panel,
            onSurface = palette.screenContent,
            glassBase = palette.lift,
            glassContent = palette.screenContent,
            // 柔光方向由表面明暗决定：深色表面往白提亮，浅色表面往黑压暗。
            glassHighlight = if (palette.isLight) BLACK else WHITE,
            glassBorder = borderColorFor(palette.visualStyle, palette.isLight),
            accent = palette.accent,
            // 8 组主题都有明确的强调色语义，不再回退到玻璃提亮质感。
            accentEnabled = true,
        )

        /**
         * 结构性描边色。
         *
         * 前四组 Liquid Glass 结构的描边是通用玻璃边——深色表面往白、浅色表面往黑，
         * 按明暗推就对了。DIGITAL 与 TACTILE 不行，它们有自己的切边色：
         *
         * TACTILE 是**深色**表面，但它的边是近黑的机械切边（胶帽之间的缝）。
         * 按明暗推会得到白边，而渲染层的描边与内缘阴影共用同一个颜色
         * （见 `GlassModifier` 的 `innerShadowAlpha` 与 `border` 两处），
         * 于是每个胶帽会多一圈 74% 的白框加一层白色内发光，凸起感直接反掉。
         */
        private fun borderColorFor(style: GlassVisualStyle, isLight: Boolean): Long =
            when (style) {
                GlassVisualStyle.DIGITAL -> DIGITAL_EDGE
                GlassVisualStyle.TACTILE -> TACTILE_EDGE
                GlassVisualStyle.DROP,
                GlassVisualStyle.NEUTRAL,
                GlassVisualStyle.DARK,
                GlassVisualStyle.NATIVE,
                -> if (isLight) BLACK else WHITE
            }

        /** Digital 卡片的描边色，对应参考稿的 `rgba(12,17,15,.13)` */
        private const val DIGITAL_EDGE = 0xFF0C110FL

        /** Tactile 胶帽之间的近黑切边 */
        private const val TACTILE_EDGE = 0xFF080909L
    }
}

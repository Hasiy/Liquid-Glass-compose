/*
 * Copyright 2026 FitDash contributors.
 */
package top.hasiy.designsystem.tokens

import kotlin.math.pow

/**
 * 主题色标识。
 *
 * [id] 是持久化与诊断用的稳定字符串，取值与参考设计稿的 `data-theme` 一致；
 * 显示名称由使用端从字符串资源提供，不在 token 层写死文案。
 */
enum class ThemePaletteId(val id: String) {
    /** 荧光绿：参考设计稿的基准主题。 */
    LIME("lime"),

    /** 品牌天蓝。 */
    SKY("sky"),

    /** 北欧雾绿：浅色中性轴 + 绿色语义强调。 */
    NORDIC("nordic"),

    /** 冰川蓝。 */
    OCEAN("ocean"),

    /** 熔岩橙。 */
    EMBER("ember"),

    /** 极光紫。 */
    AURORA("aurora"),

    /** 数码白：浅色数字仪表风格。 */
    DIGITAL("digital"),

    /** 实体按键：深色机械控件风格。 */
    TACTILE("tactile"),
    ;

    companion object {
        /**
         * 按稳定 ID 反查主题标识。
         *
         * @param id 已持久化的主题 ID；未知或为空时回退到 [LIME]
         */
        fun fromId(id: String?): ThemePaletteId = entries.firstOrNull { it.id == id } ?: LIME
    }
}

/**
 * 一组主题色 token。
 *
 * 这一层只描述「颜色语义」，不含玻璃材质的结构参数（透明度、圆角、陰影）——
 * 后者由 [GlassVisualStyle] 决定。两者分离才能组合出「Neutral 结构 + Nordic 配色」
 * 这类搭配，而不是每加一种配色就复制一整套材质参数。
 *
 * 颜色使用无符号 ARGB 32 位值保存，避免 token 依赖 Android 资源或平台 UI 类型。
 *
 * @param id 稳定主题标识。
 * @param visualStyle 这组配色默认搭配的玻璃视觉结构。
 * @param isLight 设备屏幕内的表面是否为浅色语义。注意判断依据是 [screen] 而不是
 *   [background]：Digital 与 Nordic 的画布都是浅灰，但只有它们的屏内也是浅色；
 *   Tactile 的画布浅、屏内深，必须归为深色表面。
 * @param accent 主强调色：CTA、选中态、进度填充、当前调节项。
 * @param accentLight 强调色的浅色变体，用于填充段由浅到深的渐层起点。
 * @param accentDeep 强调色的加深变体，用于压在浅底上的小字（连接状态、Live 标签）。
 *   深色主题下与 [accent] 同值。
 * @param quietAccent 辅助微文字要不要退出强调色。指标卡的 ±、量程旁的 STEP、
 *   抽屉里的「已在别处」这类说明字，在深色屏上用强调色是点缀，换到浅色屏上就变成
 *   一路绿到底、而且小字对比度不够（Nordic 的 `#079D68` 压在近白面板上只有 3:1，
 *   小字不过 AA）。配色可以在这里给一个中性色让它们整批降下来；
 *   留 `0L`（未指定）表示不降，仍走 [accent]。
 *   注意这只管**装饰性小字**，语义色块不受影响：CTA、选中态、当前调节项、
 *   连接与录制状态仍然是 [accent] / [accentDeep]。
 * @param onAccent 压在 [accent] 上的文字色，必须随主题走：配黄绿的暗黄留在蓝底上会显脏。
 * @param ambient 环境光色，用于页面背景的第二个径向渐层。
 * @param track 轨道色：仪表环未到达的量程、滑杆未填充段。
 * @param danger 危险语义色：断连、写入失败、设备拒绝。
 * @param background 画布底色（设备外框之外的页面底）。
 * @param screen 设备屏幕底色。
 * @param panel 面板底色，比 [screen] 高一阶的容器。
 * @param line 分隔线与卡片描边色，含 alpha。
 * @param muted 次级文字色（指标标签、单位、说明文案）。
 * @param ink 画布上的主文字色（设备外框之外）。
 * @param screenContent 设备屏幕内的主文字色。
 * @param lift 比 [panel] 提亮一阶的表面，用于卡片。
 * @param sunk 比 [panel] 压暗一阶的表面，用于底部 dock 与内凹容器。
 * @param glass 浮层表面色（调节浮层、Toast），含 alpha。
 * @param glassDeep 更深一阶的浮层表面色（抽屉、对话框），含 alpha。
 * @param sheen 凸起表面的受光高光色。见 [SurfaceSet.sheen]。
 */
data class ThemePalette(
    val id: ThemePaletteId,
    val visualStyle: GlassVisualStyle,
    val isLight: Boolean,
    val accent: Long,
    val accentLight: Long,
    val accentDeep: Long,
    val quietAccent: Long = 0L,
    val onAccent: Long,
    val ambient: Long,
    val track: Long,
    val danger: Long,
    val background: Long,
    val screen: Long,
    val panel: Long,
    val line: Long,
    val muted: Long,
    val ink: Long,
    val screenContent: Long,
    val lift: Long,
    val sunk: Long,
    val glass: Long,
    val glassDeep: Long,
    val sheen: Long = 0L,
) {
    /**
     * 画布是不是浅色的。
     *
     * 与 [isLight] 不是同一件事：那个说的是**屏内**表面。页面顶端露出来的是画布，
     * 所以系统列图示的明暗要跟这个走——Tactile 正是「画布浅、屏内深」，
     * 用 [isLight] 会在近白的画布上画出白色图示。
     *
     * 判断方式是比对黑字与白字在这个底色上的对比度，取较高的那一边；
     * 单纯拿亮度跟 0.5 比会把 Nordic 的中灰画布 `#ABABAB` 误判成深色
     * （它对黑字 8.9:1、对白字只有 2.4:1）。
     */
    val isCanvasLight: Boolean
        get() {
            val luminance = relativeLuminance(background)
            val contrastWithBlack = (luminance + CONTRAST_OFFSET) / CONTRAST_OFFSET
            val contrastWithWhite = (1f + CONTRAST_OFFSET) / (luminance + CONTRAST_OFFSET)
            return contrastWithBlack >= contrastWithWhite
        }

    companion object {
        /** WCAG 对比度公式里的常数项 */
        private const val CONTRAST_OFFSET = 0.05f

        /** sRGB 相对亮度（WCAG 定义）。忽略 alpha：画布底色一律是不透明的。 */
        private fun relativeLuminance(argb: Long): Float =
            RED_WEIGHT * channelLuminance((argb shr 16) and 0xFF) +
                GREEN_WEIGHT * channelLuminance((argb shr 8) and 0xFF) +
                BLUE_WEIGHT * channelLuminance(argb and 0xFF)

        private fun channelLuminance(value: Long): Float {
            val channel = value / 255f
            return if (channel <= GAMMA_THRESHOLD) {
                channel / GAMMA_LOW_DIVISOR
            } else {
                ((channel + GAMMA_OFFSET) / GAMMA_DIVISOR).pow(GAMMA_EXPONENT)
            }
        }

        private const val RED_WEIGHT = 0.2126f
        private const val GREEN_WEIGHT = 0.7152f
        private const val BLUE_WEIGHT = 0.0722f
        private const val GAMMA_THRESHOLD = 0.03928f
        private const val GAMMA_LOW_DIVISOR = 12.92f
        private const val GAMMA_OFFSET = 0.055f
        private const val GAMMA_DIVISOR = 1.055f
        private const val GAMMA_EXPONENT = 2.4f

        // ---------- 六组主题色 ----------
        // 六组走的是**同一条组装路径**：都由 [palette] 把一组强调色 [AccentSet] 和
        // 所属色系的表面色 [SurfaceSet] 拼起来，差异全部落在 `PaletteTokens` 里的色值。
        // 想加一组新配色，在那个文件里补一个 AccentSet、在这里加一行即可，不必动实现。

        /** 荧光绿。 */
        val Lime: ThemePalette = darkPalette(ThemePaletteId.LIME, PaletteTokens.Lime)

        /** 品牌天蓝。 */
        val Sky: ThemePalette = darkPalette(ThemePaletteId.SKY, PaletteTokens.Sky)

        /** 北欧雾绿。白色系。 */
        val Nordic: ThemePalette = lightPalette(ThemePaletteId.NORDIC, PaletteTokens.Nordic)

        /** 冰川蓝。白色系。 */
        val Ocean: ThemePalette = lightPalette(ThemePaletteId.OCEAN, PaletteTokens.Ocean)

        /** 熔岩橙。 */
        val Ember: ThemePalette = darkPalette(ThemePaletteId.EMBER, PaletteTokens.Ember)

        /** 极光紫。 */
        val Aurora: ThemePalette = darkPalette(ThemePaletteId.AURORA, PaletteTokens.Aurora)

        // ---------- 两套自带材质的风格 ----------
        // 这两组换掉的不只是配色，还有整套材质，所以各自带一份表面色。
        // 组装仍然走同一个 [palette]，只是不共用色系的表面。

        /** 数码白：浅色数字仪表风格。 */
        val Digital: ThemePalette = palette(
            id = ThemePaletteId.DIGITAL,
            visualStyle = GlassVisualStyle.DIGITAL,
            isLight = true,
            accents = PaletteTokens.Digital,
            surfaces = PaletteTokens.DigitalSurfaces,
        )

        /**
         * 实体按键：深色机械控件风格。
         *
         * 画布是浅色塑胶质感，但屏内是深色，因此 [isLight] 为 false。
         */
        val Tactile: ThemePalette = palette(
            id = ThemePaletteId.TACTILE,
            visualStyle = GlassVisualStyle.TACTILE,
            isLight = false,
            accents = PaletteTokens.Tactile,
            surfaces = PaletteTokens.TactileSurfaces,
        )

        /** 全部 8 组主题色，顺序与参考稿的主题切换器一致。 */
        val All: List<ThemePalette> = listOf(
            Lime,
            Sky,
            Nordic,
            Ocean,
            Ember,
            Aurora,
            Digital,
            Tactile,
        )

        /**
         * 按稳定 ID 取主题色。
         *
         * @param id 已持久化的主题 ID；未知或为空时回退到 [Lime]
         */
        fun fromId(id: String?): ThemePalette = of(ThemePaletteId.fromId(id))

        /** 按主题标识取主题色。 */
        fun of(id: ThemePaletteId): ThemePalette = All.first { it.id == id }

        /**
         * 黑色系：荧光绿、品牌天蓝、熔岩橙、极光紫。
         *
         * 深色底上强调色本身就够亮，加深变体与降调色都用不上，留给 [AccentSet] 的默认值。
         */
        private fun darkPalette(id: ThemePaletteId, accents: AccentSet): ThemePalette = palette(
            id = id,
            visualStyle = GlassVisualStyle.DROP,
            isLight = false,
            accents = accents,
            surfaces = PaletteTokens.Dark,
        )

        /** 白色系：北欧雾绿、冰川蓝。 */
        private fun lightPalette(id: ThemePaletteId, accents: AccentSet): ThemePalette = palette(
            id = id,
            visualStyle = GlassVisualStyle.NEUTRAL,
            isLight = true,
            accents = accents,
            surfaces = PaletteTokens.Light,
        )

        /**
         * 把一组强调色和一组表面色拼成主题色。
         *
         * 所有主题都走这里，没有第二条路径——某一组「长得不一样」只能是色值不一样。
         *
         * @param id 稳定主题标识
         * @param visualStyle 这组配色默认搭配的玻璃视觉结构
         * @param isLight 屏内表面是否为浅色语义
         * @param accents 强调色
         * @param surfaces 表面色
         */
        private fun palette(
            id: ThemePaletteId,
            visualStyle: GlassVisualStyle,
            isLight: Boolean,
            accents: AccentSet,
            surfaces: SurfaceSet,
        ): ThemePalette = ThemePalette(
            id = id,
            visualStyle = visualStyle,
            isLight = isLight,
            accent = accents.accent,
            accentLight = accents.accentLight,
            // 没给加深变体就用主色本身：深色底上两者本来就是同一个值
            accentDeep = accents.accentDeep.takeIf { it != 0L } ?: accents.accent,
            quietAccent = accents.quietAccent,
            onAccent = accents.onAccent,
            ambient = accents.ambient,
            track = surfaces.track,
            danger = surfaces.danger,
            background = surfaces.background,
            screen = surfaces.screen,
            panel = surfaces.panel,
            line = surfaces.line,
            muted = surfaces.muted,
            ink = surfaces.ink,
            screenContent = surfaces.screenContent,
            lift = surfaces.lift,
            sunk = surfaces.sunk,
            glass = surfaces.glass,
            glassDeep = surfaces.glassDeep,
            sheen = surfaces.sheen,
        )
    }
}

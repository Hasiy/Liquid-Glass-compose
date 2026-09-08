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
 */
data class ThemePalette(
    val id: ThemePaletteId,
    val visualStyle: GlassVisualStyle,
    val isLight: Boolean,
    val accent: Long,
    val accentLight: Long,
    val accentDeep: Long,
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

        // ---------- 深色玻璃主题共用的结构性表面色 ----------
        // 参考稿的 5 组深色主题（Lime / Sky / Ocean / Ember / Aurora）只换强调色系，
        // 屏底、面板与浮层这些结构色一律沿用基准值。
        private const val DARK_TRACK = 0xFF303536L
        private const val DARK_DANGER = 0xFFFF6969L
        private const val DARK_BACKGROUND = 0xFFEDF0E9L
        private const val DARK_SCREEN = 0xFF070909L
        private const val DARK_PANEL = 0xFF171B1CL

        /** rgba(255,255,255,.09) */
        private const val DARK_LINE = 0x17FFFFFFL
        private const val DARK_MUTED = 0xFF818A85L
        private const val DARK_INK = 0xFF0B0E0DL
        private const val DARK_SCREEN_CONTENT = 0xFFF7F9F6L
        private const val DARK_LIFT = 0xFF1D2121L
        private const val DARK_SUNK = 0xFF151919L

        /** rgba(29,33,33,.97) */
        private const val DARK_GLASS = 0xF71D2121L
        private const val DARK_GLASS_DEEP = 0xFF161A19L

        /** 荧光绿：参考稿的基准主题，其余深色主题以此为底只换强调色系。 */
        val Lime: ThemePalette = darkPalette(
            id = ThemePaletteId.LIME,
            accent = 0xFFDFFF32L,
            accentLight = 0xFFEFFF9AL,
            onAccent = 0xFF151800L,
            ambient = 0xFF69E2CEL,
        )

        /**
         * 品牌天蓝。
         *
         * 基准品牌色 #2B7FD4 在深色屏上只有 4.8:1，小字发闷，
         * 因此取深色场景的提亮版 #5AA9EE（7.9:1）。
         */
        val Sky: ThemePalette = darkPalette(
            id = ThemePaletteId.SKY,
            accent = 0xFF5AA9EEL,
            accentLight = 0xFFC4E2FBL,
            onAccent = 0xFF06202FL,
            ambient = 0xFF6FD3E8L,
        )

        /** 冰川蓝。 */
        val Ocean: ThemePalette = darkPalette(
            id = ThemePaletteId.OCEAN,
            accent = 0xFF43D9FFL,
            accentLight = 0xFFB8F2FFL,
            onAccent = 0xFF041E29L,
            ambient = 0xFF5B7EFFL,
        )

        /** 熔岩橙。 */
        val Ember: ThemePalette = darkPalette(
            id = ThemePaletteId.EMBER,
            accent = 0xFFFF9A3DL,
            accentLight = 0xFFFFD0A3L,
            onAccent = 0xFF2A1400L,
            ambient = 0xFFFF635BL,
        )

        /** 极光紫。 */
        val Aurora: ThemePalette = darkPalette(
            id = ThemePaletteId.AURORA,
            accent = 0xFFB78CFFL,
            accentLight = 0xFFDECAFFL,
            onAccent = 0xFF1A0F2EL,
            ambient = 0xFF45E2BEL,
        )

        /**
         * 北欧雾绿：浅色中性轴 + 绿色语义强调。
         *
         * 灰阶一律不掺绿；绿只留给仪表、主 CTA、当前调节项和连接状态。
         * 明度台阶：画布 #ABABAB → 屏底 #D1D1D1 → 面板 #E3E3E3 → 提亮 #F1F1F1。
         */
        val Nordic: ThemePalette = ThemePalette(
            id = ThemePaletteId.NORDIC,
            visualStyle = GlassVisualStyle.NEUTRAL,
            isLight = true,
            accent = 0xFF079D68L,
            accentLight = 0xFF74D4ACL,
            // 压白字的绿要够深：#0A6D4B 对白字 6.4:1，#087A55 只有 4.3:1，小字不过 AA。
            accentDeep = 0xFF0A6D4BL,
            onAccent = 0xFFFFFFFFL,
            ambient = 0xFF72C7A9L,
            track = 0xFFB0B0B0L,
            danger = 0xFFBD4545L,
            background = 0xFFABABABL,
            screen = 0xFFD1D1D1L,
            panel = 0xFFE3E3E3L,
            // rgba(13,13,13,.08)
            line = 0x140D0D0DL,
            muted = 0xFF545454L,
            ink = 0xFF0D0D0DL,
            screenContent = 0xFF0D0D0DL,
            lift = 0xFFF1F1F1L,
            sunk = 0xFFC9C9C9L,
            // rgba(238,238,238,.72)
            glass = 0xB8EEEEEEL,
            // rgba(232,232,232,.8)
            glassDeep = 0xCCE8E8E8L,
        )

        /**
         * 数码白：浅色数字仪表风格。
         *
         * 仪表环换成点环，因此 [track] 取点环未到达色 #D7DAD7 而不是深色主题的深灰轨道
         * ——参考稿没有覆盖 `--track`，级联下来的深灰在这套浅色仪表里不会出现。
         */
        val Digital: ThemePalette = ThemePalette(
            id = ThemePaletteId.DIGITAL,
            visualStyle = GlassVisualStyle.DIGITAL,
            isLight = true,
            accent = 0xFFFF3B30L,
            accentLight = 0xFFFFB8B2L,
            accentDeep = 0xFFFF3B30L,
            onAccent = 0xFFFFFFFFL,
            ambient = 0xFF4A9B63L,
            track = 0xFFD7DAD7L,
            danger = 0xFFFF3B30L,
            background = 0xFFE7E9E7L,
            screen = 0xFFF4F5F2L,
            panel = 0xFFE9ECE9L,
            // rgba(12,17,15,.14)
            line = 0x240C110FL,
            muted = 0xFF737A77L,
            ink = 0xFF111514L,
            screenContent = 0xFF111514L,
            // rgba(235,238,234,.86)
            lift = 0xDBEBEEEAL,
            // rgba(228,232,228,.94)
            sunk = 0xF0E4E8E4L,
            // rgba(244,246,243,.98)
            glass = 0xFAF4F6F3L,
            // rgba(229,233,229,.8)
            glassDeep = 0xCCE5E9E5L,
        )

        /**
         * 实体按键：深色机械控件风格。
         *
         * 画布是浅色塑胶质感，但屏内是深色，因此 [isLight] 为 false。
         */
        val Tactile: ThemePalette = ThemePalette(
            id = ThemePaletteId.TACTILE,
            visualStyle = GlassVisualStyle.TACTILE,
            isLight = false,
            accent = 0xFF10E66BL,
            accentLight = 0xFF9DFFC5L,
            accentDeep = 0xFF10E66BL,
            onAccent = 0xFF05200FL,
            ambient = 0xFF10E66BL,
            track = 0xFF4A4D4BL,
            danger = 0xFFFF6D70L,
            background = 0xFFDEDFDDL,
            screen = 0xFF191A1BL,
            panel = 0xFF2A2B2CL,
            // rgba(255,255,255,.12)
            line = 0x1FFFFFFFL,
            muted = 0xFF999D9BL,
            ink = 0xFF151716L,
            screenContent = 0xFFF0F1EEL,
            lift = 0xFF3B3C3DL,
            sunk = 0xFF1B1C1DL,
            glass = 0xFF363839L,
            glassDeep = 0xFF1C1D1EL,
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
         * 构造一组深色玻璃主题：只换强调色系，结构性表面色沿用基准值。
         *
         * 深色底上强调色本身就够亮，[ThemePalette.accentDeep] 与 [accent] 同值——
         * 只有压在浅底上的小字才需要加深变体。
         */
        private fun darkPalette(
            id: ThemePaletteId,
            accent: Long,
            accentLight: Long,
            onAccent: Long,
            ambient: Long,
        ): ThemePalette = ThemePalette(
            id = id,
            visualStyle = GlassVisualStyle.DROP,
            isLight = false,
            accent = accent,
            accentLight = accentLight,
            accentDeep = accent,
            onAccent = onAccent,
            ambient = ambient,
            track = DARK_TRACK,
            danger = DARK_DANGER,
            background = DARK_BACKGROUND,
            screen = DARK_SCREEN,
            panel = DARK_PANEL,
            line = DARK_LINE,
            muted = DARK_MUTED,
            ink = DARK_INK,
            screenContent = DARK_SCREEN_CONTENT,
            lift = DARK_LIFT,
            sunk = DARK_SUNK,
            glass = DARK_GLASS,
            glassDeep = DARK_GLASS_DEEP,
        )
    }
}

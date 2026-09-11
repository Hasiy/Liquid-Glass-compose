package top.hasiy.designsystem.tokens

/**
 * 主题色的**色值配置**。
 *
 * 这个文件只放数字，不放逻辑：一组主题长什么样，全部由这里的常量决定；
 * 怎么组装成 [ThemePalette] 由那边的工厂函数负责。两者分开是为了让
 * 「换一组配色」永远只是改色值，不需要动任何代码路径——六组主题色
 * （荧光绿 / 品牌天蓝 / 北欧雾绿 / 冰川蓝 / 熔岩橙 / 极光紫）走的是同一套实现。
 *
 * 色系归属：白色系是北欧雾绿、冰川蓝；黑色系是荧光绿、品牌天蓝、熔岩橙、极光紫。
 *
 * 分两层：
 * - **表面层**（[Dark] / [Light] / [DigitalSurfaces] / [TactileSurfaces]）：屏底、面板、
 *   分隔线、文字色这些结构色。同一个色系里的主题共用一份。
 * - **强调层**（[AccentSet]）：每组主题各自的强调色。六组主题之间**只差这一层**。
 *
 * 颜色一律是无符号 ARGB 32 位值（`0xAARRGGBB`），不依赖任何平台 UI 类型。
 */

/**
 * 一组主题的强调色。
 *
 * 六组主题色之间的全部差异都在这里；表面色由所属色系统一提供。
 *
 * @param accent 主强调色：CTA、选中态、进度填充、当前调节项
 * @param accentLight 浅色变体，填充段由浅到深的渐层起点
 * @param onAccent 压在 [accent] 上的文字色
 * @param ambient 环境光色，页面背景的第二个径向渐层
 * @param accentDeep 压在浅底上的加深变体。留 `0L` 表示不需要，回落到 [accent]——
 *   深色屏上强调色本身就够亮，只有浅色系才要加深一档
 * @param quietAccent 装饰性小字的降调色。留 `0L` 表示不降，仍走 [accent]。
 *   浅色系才用得上：强调色在近白面板上的小字对比度往往过不了 AA
 */
data class AccentSet(
    val accent: Long,
    val accentLight: Long,
    val onAccent: Long,
    val ambient: Long,
    val accentDeep: Long = 0L,
    val quietAccent: Long = 0L,
)

/**
 * 一个色系共用的表面色。
 *
 * @param track 轨道色：仪表环未到达的量程、滑杆未填充段
 * @param danger 危险语义色：断连、写入失败、设备拒绝
 * @param background 画布底色（设备外框之外）
 * @param screen 设备屏幕底色
 * @param panel 面板底色，比 [screen] 高一阶
 * @param line 分隔线与卡片描边色，含 alpha
 * @param muted 次级文字色
 * @param ink 画布上的主文字色
 * @param screenContent 屏内主文字色
 * @param lift 比 [panel] 提亮一阶的表面
 * @param sunk 比 [panel] 压暗一阶的表面
 * @param glass 浮层表面色，含 alpha
 * @param glassDeep 更深一阶的浮层表面色，含 alpha
 * @param sheen 凸起表面的受光高光色（仪表盘面、凸圆容器的光心）。
 *   白色系要给到近白：浅色屏上盘面和面板明度太近，不靠这一档提亮就糊成一片。
 *   黑色系用不到——那边的凸起感由外环给，盘面是纯色，所以填 [panel] 即可。
 */
data class SurfaceSet(
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
    val sheen: Long,
)

/** 六组主题色的色值表。 */
object PaletteTokens {

    // ---------- 表面层 ----------

    /**
     * 黑色系共用的表面色：荧光绿、品牌天蓝、熔岩橙、极光紫。
     *
     * 明度台阶：屏底 #070909 → 面板 #171B1C → 提亮 #1D2121。
     */
    val Dark: SurfaceSet = SurfaceSet(
        track = 0xFF303536L,
        danger = 0xFFFF6969L,
        background = 0xFFEDF0E9L,
        screen = 0xFF070909L,
        panel = 0xFF171B1CL,
        // rgba(255,255,255,.09)
        line = 0x17FFFFFFL,
        muted = 0xFF818A85L,
        ink = 0xFF0B0E0DL,
        screenContent = 0xFFF7F9F6L,
        lift = 0xFF1D2121L,
        sunk = 0xFF151919L,
        // rgba(29,33,33,.97)
        glass = 0xF71D2121L,
        glassDeep = 0xFF161A19L,
        // 深色屏的盘面是纯色（参考稿 #15191A ≈ panel），不走高光那一档
        sheen = 0xFF171B1CL,
    )

    /**
     * 白色系共用的表面色：北欧雾绿、冰川蓝。
     *
     * 灰阶一律不掺色，强调色只留给语义。
     * 明度台阶：画布 #ABABAB → 屏底 #D1D1D1 → 面板 #E3E3E3 → 提亮 #F1F1F1。
     */
    val Light: SurfaceSet = SurfaceSet(
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
        // 参考稿盘面 radial-gradient 的光心：#FFFFFF
        sheen = 0xFFFFFFFFL,
    )

    // ---------- 强调层：六组主题色 ----------

    /** 荧光绿。参考稿的基准主题。 */
    val Lime: AccentSet = AccentSet(
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
    val Sky: AccentSet = AccentSet(
        accent = 0xFF5AA9EEL,
        accentLight = 0xFFC4E2FBL,
        onAccent = 0xFF06202FL,
        ambient = 0xFF6FD3E8L,
    )

    /**
     * 北欧雾绿。
     *
     * 白色系唯一一组，所以两个「只有浅底才需要」的变体都给满：
     * 压白字的绿要够深（#0A6D4B 对白字 6.4:1，#087A55 只有 4.3:1，小字不过 AA）；
     * 装饰性小字则整批回中性灰（#079D68 压在近白面板上只有 3:1）。
     */
    val Nordic: AccentSet = AccentSet(
        accent = 0xFF079D68L,
        accentLight = 0xFF74D4ACL,
        onAccent = 0xFFFFFFFFL,
        ambient = 0xFF72C7A9L,
        accentDeep = 0xFF0A6D4BL,
        quietAccent = 0xFF545454L,
    )

    /**
     * 冰川蓝。**白色系**。
     *
     * 挪到浅色轴之后整组都要重定：原来那套是给深色屏配的，主色 #43D9FF 压在
     * 面板 #E3E3E3 上只有 1.3:1——仪表弧、填充段、选中态全是看不见的色块。
     * 加深到 #168FB4（2.91:1）才和北欧雾绿的 #079D68（2.71:1）同一档位。
     *
     * 两个「只有浅底才需要」的变体也要给满：
     * 主色压白字只有 3.7:1（雾绿同样只有 3.5:1），所以压白字的场合走
     * accentDeep #0A6A87（6.1:1）；装饰性小字整批回中性灰。
     */
    val Ocean: AccentSet = AccentSet(
        accent = 0xFF168FB4L,
        accentLight = 0xFF7FCADFL,
        onAccent = 0xFFFFFFFFL,
        ambient = 0xFF5B93C7L,
        accentDeep = 0xFF0A6A87L,
        quietAccent = 0xFF545454L,
    )

    /** 熔岩橙。 */
    val Ember: AccentSet = AccentSet(
        accent = 0xFFFF9A3DL,
        accentLight = 0xFFFFD0A3L,
        onAccent = 0xFF2A1400L,
        ambient = 0xFFFF635BL,
    )

    /** 极光紫。 */
    val Aurora: AccentSet = AccentSet(
        accent = 0xFFB78CFFL,
        accentLight = 0xFFDECAFFL,
        onAccent = 0xFF1A0F2EL,
        ambient = 0xFF45E2BEL,
    )

    // ---------- 两套自带材质的风格 ----------
    // 这两组不在「六组主题色」之列：它们换掉的不只是配色，还有整套材质
    // （数码管读数、点环仪表 / 胶帽按键、面板点阵），表面色也各自独立。

    /** 数码白的强调色。 */
    val Digital: AccentSet = AccentSet(
        accent = 0xFFFF3B30L,
        accentLight = 0xFFFFB8B2L,
        onAccent = 0xFFFFFFFFL,
        ambient = 0xFF4A9B63L,
    )

    /**
     * 数码白的表面色。
     *
     * 仪表环换成点环，因此 [SurfaceSet.track] 取点环未到达色 #D7DAD7 而不是深色主题的
     * 深灰轨道——参考稿没有覆盖 `--track`，级联下来的深灰在这套浅色仪表里不会出现。
     */
    val DigitalSurfaces: SurfaceSet = SurfaceSet(
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
        sheen = 0xFFFFFFFFL,
    )

    /** 实体按键的强调色。 */
    val Tactile: AccentSet = AccentSet(
        accent = 0xFF10E66BL,
        accentLight = 0xFF9DFFC5L,
        onAccent = 0xFF05200FL,
        ambient = 0xFF10E66BL,
    )

    /** 实体按键的表面色。画布是浅色塑胶，屏内是深色机械面板。 */
    val TactileSurfaces: SurfaceSet = SurfaceSet(
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
        // 金属盘面自带斜向受光，不再叠一层光心
        sheen = 0xFF2A2B2CL,
    )
}

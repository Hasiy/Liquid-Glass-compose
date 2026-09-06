package top.hasiyliquidglassdemo.ui.legacy

import androidx.compose.animation.core.Spring

/**
 * DynamicLightTabBar 的可調參數設定。
 *
 * 所有跟「動態光影、毛玻璃、動畫、尺寸」相關的數值集中於此，方便直接微調，
 * 不需進到 [DynamicLightTabBar] 主體。
 */
object DynamicLightTabBarLegacyConfig {

    // ---------- 尺寸 ----------
    /** Tab Bar 高度 */
    const val BAR_HEIGHT_DP = 72

    /** 背景圓角 */
    const val BAR_CORNER_RADIUS_DP = 36

    /** Pill（選中指示器）圓角 */
    const val INDICATOR_CORNER_RADIUS_DP = 32

    /** Tab 文字大小（sp） */
    const val TAB_TEXT_SIZE_SP = 13

    // ---------- 光暈參數（AGSL RuntimeShader）----------
    /**
     * 整條 Bar 的光暈半徑倍率（相對於單一 item 寬度）。
     * 1.05 → 光暈略大於一個 item，邊緣更柔和地擴散到相鄰區域。
     */
    const val BAR_GLOW_RADIUS_FACTOR = 1.05f

    /** 整條 Bar 的光暈強度（0.0~1.0）；越高越亮 */
    const val BAR_GLOW_INTENSITY = 0.20f

    /**
     * Pill 內補光半徑倍率（相對於 item 寬度）。
     * 光暈只在接近手指時微微提亮選中項。
     */
    const val INDICATOR_GLOW_RADIUS_FACTOR = 0.72f

    /** Pill 內補光強度（0.0~1.0） */
    const val INDICATOR_GLOW_INTENSITY = 0.035f

    // ---------- 背景玻璃感 ----------
    /** 背景主色（淺灰白，低透明度時呈現通透毛玻璃感） */
    val BACKGROUND_COLOR_HEX: Long = 0xFF8A8A9A

    /** 背景透明度（0.0~1.0）；越低越透、越能看見底層內容 */
    const val BACKGROUND_ALPHA = 0.22f

    /** 玻璃頂部反光渐变起始透明度（0.0~1.0），越高玻璃感越强 */
    const val GLASS_REFLECTION_TOP_ALPHA = 0.14f

    /** 玻璃頂部反光渐变结束透明度 */
    const val GLASS_REFLECTION_BOTTOM_ALPHA = 0.02f

    /** 是否啟用玻璃反光层 */
    const val GLASS_REFLECTION_ENABLED = true

    // ---------- 玻璃邊緣描邊 ----------
    /** 是否啟用玻璃邊緣描邊 */
    const val GLASS_BORDER_ENABLED = true

    /** 玻璃邊緣描邊寬度（dp） */
    const val GLASS_BORDER_WIDTH_DP = 0.8

    /** 邊緣描邊頂部（最亮）透明度 */
    const val GLASS_BORDER_TOP_ALPHA = 0.28f

    /** 邊緣描邊底部（漸淡）透明度 */
    const val GLASS_BORDER_BOTTOM_ALPHA = 0.05f

    /** 外陰影環境光顏色 */
    val OUTER_SHADOW_AMBIENT_HEX: Long = 0xFF4444FF

    /** 外陰影環境光透明度 */
    const val OUTER_SHADOW_AMBIENT_ALPHA = 0.12f

    /** 外陰影聚光顏色 */
    val OUTER_SHADOW_SPOT_HEX: Long = 0xFF4444FF

    /** 外陰影聚光透明度 */
    const val OUTER_SHADOW_SPOT_ALPHA = 0.08f

    /** 外陰影高度（dp） */
    const val OUTER_SHADOW_ELEVATION_DP = 16

    // ---------- Pill 內部 ----------
    /** 是否啟用水滴質感（荷葉上水滴的視覺） */
    const val WATER_DROP_ENABLED = true

    /** Pill 背景主色（半透明白玻璃，讓底下深色透出形成磨砂玻璃感） */
    val INDICATOR_BACKGROUND_HEX: Long = 0xFF9A9AA8

    /**
     * 水滴主體：頂部高光 → 中間主色 → 底部聚光的垂直漸層。
     * 底部偏亮模擬水滴在葉面上的折射聚光。
     */
    const val INDICATOR_BODY_TOP_ALPHA = 0.30f
    const val INDICATOR_BODY_BOTTOM_ALPHA = 0.32f

    /**
     * 玻璃頂部高光：內圈透明度、外圈透明度。
     * 內圈越高、玻璃上緣反光越明顯。
     */
    /** 頂部高光內圈透明度（0.0~1.0） */
    const val SPECKLE_INNER_ALPHA = 0.16f

    /** 頂部高光外圈透明度 */
    const val SPECKLE_OUTER_ALPHA = 0.035f

    /** 水滴高光中心的預設水平位置（0.0=最左，1.0=最右） */
    const val WATER_DROP_HIGHLIGHT_CENTER_X_FRACTION = 0.28f

    /** 水滴高光中心的預設垂直位置（0.0=最上，1.0=最下） */
    const val WATER_DROP_HIGHLIGHT_CENTER_Y_FRACTION = 0.08f

    /** 高光柔化：radial 光暈半徑（佔寬度比例） */
    const val WATER_DROP_GLOW_RADIUS_FACTOR = 0.72f

    /** Pill 邊框漸層頂部透明度（水滴邊緣高光） */
    const val INDICATOR_BORDER_TOP_ALPHA = 0.20f

    /** Pill 邊框漸層底部透明度 */
    const val INDICATOR_BORDER_BOTTOM_ALPHA = 0.02f

    /** Pill 內陰影環境光透明度（與葉面的接觸陰影） */
    const val INDICATOR_SHADOW_AMBIENT_ALPHA = 0.30f

    /** Pill 內陰影聚光透明度 */
    const val INDICATOR_SHADOW_SPOT_ALPHA = 0.22f

    /** Pill 內陰影高度（dp） */
    const val INDICATOR_SHADOW_ELEVATION_DP = 5

    // ---------- 動畫 ----------
    /** Indicator 朝新選項方向拉伸的水平倍率 */
    const val INDICATOR_STRETCH_SCALE_X = 1.15f

    /** 拉伸時的垂直收束倍率，避免 Pill 只是等比例放大 */
    const val INDICATOR_STRETCH_SCALE_Y = 0.96f

    /** 到位後短暫水平壓縮，形成液體吸附感 */
    const val INDICATOR_SETTLE_SCALE_X = 0.96f

    /** 到位壓縮時的垂直回彈倍率 */
    const val INDICATOR_SETTLE_SCALE_Y = 1.04f

    /** 朝目標拉伸階段時長（ms） */
    const val INDICATOR_STRETCH_DURATION_MS = 110

    /** 到位壓縮階段時長（ms） */
    const val INDICATOR_SETTLE_DURATION_MS = 75

    /** Indicator 最後回到原形的彈性 stiffness */
    const val SCALE_SPRING_STIFFNESS = 450f

    /** Indicator 最後回到原形的彈性 dampingRatio */
    const val SCALE_SPRING_DAMPING = 0.55f

    /** item 按下時縮放目標（0.88 = 縮到 88%） */
    const val PRESSED_SCALE = 0.88f

    /** item 按下彈性 stiffness */
    const val PRESSED_SPRING_STIFFNESS = 400f

    /** item 按下彈性 dampingRatio */
    const val PRESSED_SPRING_DAMPING = 0.6f

    /** Indicator 跟手位移彈性 stiffness（Spring.StiffnessMediumLow） */
    const val OFFSET_SPRING_STIFFNESS = Spring.StiffnessMediumLow

    /** Indicator 跟手位移彈性 dampingRatio */
    const val OFFSET_SPRING_DAMPING = 0.8f

    // ---------- 未選中文字樣式 ----------
    /** 未選中文字透明度 */
    const val TAB_TEXT_UNSELECTED_ALPHA = 0.55f
}
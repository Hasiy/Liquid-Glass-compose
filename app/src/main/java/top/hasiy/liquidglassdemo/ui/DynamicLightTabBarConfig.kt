package top.hasiyliquidglassdemo.ui

import androidx.compose.animation.core.Spring
import androidx.compose.ui.graphics.Color

/**
 * DynamicLightTabBar 的可調參數設定。
 *
 * 所有跟「動態光影、毛玻璃、動畫、尺寸」相關的數值集中於此，方便直接微調，
 * 不需進到 [DynamicLightTabBar] 主體。
 */
object DynamicLightTabBarConfig {

    // ---------- 尺寸 ----------
    /** Tab Bar 高度 */
    const val BAR_HEIGHT_DP = 72

    /** Demo 中 Tab Bar 相對可用螢幕寬度的比例 */
    const val BAR_WIDTH_FRACTION = 0.95f

    /** 是否繪製整條 Tab Bar 的半透明背景；關閉後只保留內容與邊緣效果 */
    const val BAR_BACKGROUND_ENABLED = false

    /** 背景圓角 */
    const val BAR_CORNER_RADIUS_DP = 36

    /** Pill（選中指示器）圓角 */
    const val INDICATOR_CORNER_RADIUS_DP = 32

    // ---------- 選中 Lens（凸透鏡）幾何 ----------
    /** Lens 高度：比 Bar 高約 12dp，上下各突出 6dp */
    const val LENS_HEIGHT_DP = 84

    /** Lens 寬度倍率（相對於單一槽位寬度）；1.45 可輕微侵入相鄰項 */
    const val LENS_WIDTH_FACTOR = 1.45f

    /** Lens 膠囊圓角（接近半高，形成飽滿凸透鏡） */
    const val LENS_CORNER_RADIUS_DP = 40

    /** Lens 內垂直內縮（dp）：讓材質在突出高度內保留邊距 */
    const val LENS_VERTICAL_PADDING_DP = 2

    // ---------- 選中 Lens 視覺效果 ----------
    /** 是否採用「內容採樣折射」（AGSL，內容放大/色散）；false 則用下方漸層玻璃鏡面（預設，無重影） */
    const val LENS_REFRACTION_ENABLED_DEFAULT = false

    /** 折射模式：內容放大倍率（>1 產生凸透鏡放大感） */
    const val LENS_MAGNIFICATION = 1.10f

    /** 折射模式：邊緣 RGB 分通道偏移量（px），產生色散 */
    const val LENS_CHROMATIC_PX = 1.5f

    /** 折射模式：內容放大後的輕微徑向扭曲強度（0~1） */
    const val LENS_DISTORTION = 0.05f

    /** 折射 Lens 內的透明 tint 強度，避免鏡片變成實心膠囊 */
    const val LENS_REFRACTION_TINT_ALPHA = 0.10f

    /** 折射 Lens 邊緣的單層彩色描邊透明度 */
    const val LENS_REFRACTION_BORDER_ALPHA = 0.58f

    /** 折射 Lens 自身的輕量陰影高度，跟著鏡片位置移動 */
    const val LENS_REFRACTION_SHADOW_ELEVATION_DP = 4

    /** 折射 Lens 自身的陰影透明度 */
    const val LENS_REFRACTION_SHADOW_ALPHA = 0.12f

    /** 折射 Lens 邊緣的彩色光譜，沿水平方向形成單一玻璃輪廓 */
    val LENS_REFRACTION_BORDER_COLORS = listOf(
        Color(0xFF63D8FF),
        Color(0xFFFFFFFF),
        Color(0xFFFFD277),
        Color(0xFFB38BFF),
        Color(0xFF63D8FF),
    )

    /** 膠囊邊緣 Fresnel 高光強度（0~1）：強調 Lens 輪廓 */
    const val LENS_FRESNEL_STRENGTH = 0.18f

    /** Lens 邊緣額外的彩色折射帶強度（0~1） */
    const val LENS_EDGE_CHROMATIC_STRENGTH = 0.12f

    /** Tab 文字大小（sp） */
    const val TAB_TEXT_SIZE_SP = 13

    /** Tab 圖標大小（dp），圖標在上、標籤在下 */
    const val TAB_ICON_SIZE_DP = 22

    /** Tab 圖標與標籤之間的間距（dp） */
    const val TAB_ICON_LABEL_SPACING_DP = 3

    /** 中央主操作按鈕直徑（dp），不參與選中索引 */
    const val CENTER_ACTION_SIZE_DP = 48

    // ---------- 光暈參數（AGSL RuntimeShader）----------
    /**
     * 整條 Bar 的光暈半徑倍率（相對於單一 item 寬度）。
     * 1.05 → 光暈略大於一個 item，邊緣更柔和地擴散到相鄰區域。
     */
    const val BAR_GLOW_RADIUS_FACTOR = 1.05f

    /** 整條 Bar 的光暈強度（0.0~1.0）；越高越亮。深色材質下降低，避免中心過亮 */
    const val BAR_GLOW_INTENSITY = 0.12f

    /**
     * Pill 內補光半徑倍率（相對於 item 寬度）。
     * 光暈只在接近手指時微微提亮選中項。
     */
    const val INDICATOR_GLOW_RADIUS_FACTOR = 0.72f

    /** Pill 內補光強度（0.0~1.0） */
    const val INDICATOR_GLOW_INTENSITY = 0.03f

    // ---------- 背景玻璃感 ----------
    /** 背景主色（近黑灰，較高不透明度時呈現深色玻璃條） */
    val BACKGROUND_COLOR_HEX: Long = 0xFF1A1A1E

    /** 背景透明度（0.0~1.0）；深色材質下提高，避免偏亮偏紫 */
    const val BACKGROUND_ALPHA = 0.62f

    /** 玻璃頂部反光渐变起始透明度（0.0~1.0），越高玻璃感越强 */
    const val GLASS_REFLECTION_TOP_ALPHA = 0.08f

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

    /** 外陰影環境光顏色（黑色環境陰影，移除藍紫外發光） */
    val OUTER_SHADOW_AMBIENT_HEX: Long = 0xFF000000

    /** 外陰影環境光透明度 */
    const val OUTER_SHADOW_AMBIENT_ALPHA = 0.35f

    /** 外陰影聚光顏色（黑色） */
    val OUTER_SHADOW_SPOT_HEX: Long = 0xFF000000

    /** 外陰影聚光透明度 */
    const val OUTER_SHADOW_SPOT_ALPHA = 0.25f

    /** 外陰影高度（dp） */
    const val OUTER_SHADOW_ELEVATION_DP = 16

    // ---------- Pill 內部 ----------
    /** 是否啟用水滴質感（荷葉上水滴的視覺） */
    const val WATER_DROP_ENABLED = true

    /** Pill 背景主色（暗灰玻璃，貼合參考圖的深色 Lens） */
    val INDICATOR_BACKGROUND_HEX: Long = 0xFF2A2A30

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
    const val TAB_TEXT_UNSELECTED_ALPHA = 0.68f
}

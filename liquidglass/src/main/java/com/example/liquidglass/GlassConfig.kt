package com.example.liquidglass

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 單一玻璃元件的視覺參數。
 *
 * 將「玻璃基底漸層、左上柔光、邊緣描邊、接觸陰影、文字顏色」抽象成一組可設定的參數，
 * 任何 Button / Card / Box 都可透過 [Modifier.glassSurface] 套用，並可隨時切換不同預設主題。
 *
 * @param baseColor 玻璃基底主色（半透明，透出底層背景）
 * @param bodyTopAlpha 基底頂部透明度（0.0~1.0）
 * @param bodyBottomAlpha 基底底部透明度（0.0~1.0）
 * @param highlightInnerAlpha 柔光內圈透明度（0.0~1.0），越高玻璃上緣反光越明顯
 * @param highlightOuterAlpha 柔光外圈透明度（0.0~1.0）
 * @param highlightCenterX 柔光中心水平位置（0.0=最左，1.0=最右）
 * @param highlightCenterY 柔光中心垂直位置（0.0=最上，1.0=最下）
 * @param highlightRadiusFactor 柔光半徑（佔寬度比例，>1 會擴散到整個元件）
 * @param highlightColor 柔光與指尖高光的顏色；深色表面用白（提亮），淺色表面用黑（壓暗）
 * @param highlightBlendMode 柔光的混合模式；深色表面用 [BlendMode.Screen]，淺色表面用 [BlendMode.Multiply]
 * @param touchSpotPeakAlpha 指尖高光的峰值透明度；[BlendMode.Multiply] 的感知強度較高，淺色主題需要更小的值
 * @param borderWidth 邊緣描邊寬度
 * @param borderColor 邊緣描邊顏色
 * @param innerShadowAlpha 邊緣內陰影濃度（0 = 關閉）。淺色主題下同色表面互相嵌套（例如卡片裡的
 *   輸入框）光靠描邊分不出邊界，沿內緣壓一圈由深到淺的陰影才看得出輪廓與凹陷感
 * @param innerShadowWidth 內陰影由邊緣往內延伸的寬度
 * @param borderTopAlpha 邊緣描邊頂部透明度
 * @param borderBottomAlpha 邊緣描邊底部透明度
 * @param shadowEnabled 是否繪製接觸陰影。關閉後所有玻璃表面都不投影，畫面更平；
 *   淺色主題靠陰影分層，關掉後改由描邊與內陰影撐住輪廓
 * @param shadowElevation 接觸陰影高度
 * @param shadowAmbientAlpha 陰影環境光透明度
 * @param shadowSpotAlpha 陰影聚光透明度
 * @param softShadowSpread 柔和外陰影的擴散半徑。大於 0 時改用自繪的大擴散外陰影，取代
 *   系統的 elevation 陰影——後者的擴散被 elevation 綁死，在淺色底上怎麼調都偏緊。
 *   與 [shadowElevation] 連動：elevation 為 0 的元件（填充段、選中態等）自動不畫
 * @param softShadowOffsetY 柔和外陰影往下的偏移量
 * @param softShadowAlpha 柔和外陰影的總濃度
 * @param softShadowColor 柔和外陰影的顏色
 * @param contentColor 建議的文字/圖示顏色（供元件內容使用）
 * @param accentEnabled 是否啟用強調色。false 時選中態與填充段沿用「深色提亮／淺色壓暗」的玻璃質感
 * @param accentColor 強調色：選中態指示、進度/音量/滑桿的填充段；僅在 [accentEnabled] 為 true 時生效
 * @param pageBackgroundTop 頁面背景漸層起點色（供使用端繪製頁面底色）
 * @param pageBackgroundBottom 頁面背景漸層終點色
 * @param followTouchHighlight 是否在觸摸（按下）時讓柔光跟隨手指位置移動；false 時固定於默認位置
 * @param hideHighlightOnTouch 是否在觸摸期間隱藏柔光；優先於 [followTouchHighlight]，預設 false（保留跟手光影）
 * @param overlayBlurRadius Dialog、Popup、Menu、Picker、Drawer、ModalBottomSheet 等彈層自身的背景模糊半徑
 * @param overlayFallbackAlpha 裝置不支援背景模糊或頁面未放入 [GlassBackdropHost] 時的磨砂遮罩透明度
 * @param native 是否為「Compose 原生」模式：true 時元件改用 Material3 原生樣式（無玻璃效果）
 */
@Stable
data class GlassConfig(
    val baseColor: Color = Color(0xFF9A9AA8),
    val bodyTopAlpha: Float = 0.30f,
    val bodyBottomAlpha: Float = 0.32f,
    val highlightInnerAlpha: Float = 0.16f,
    val highlightOuterAlpha: Float = 0.035f,
    val highlightCenterX: Float = 0.28f,
    val highlightCenterY: Float = 0.08f,
    val highlightRadiusFactor: Float = 0.72f,
    val highlightColor: Color = Color.White,
    val highlightBlendMode: BlendMode = BlendMode.Screen,
    val touchSpotPeakAlpha: Float = 0.07f,
    val borderWidth: Dp = 1.dp,
    val borderColor: Color = Color.White,
    val innerShadowAlpha: Float = 0f,
    val innerShadowWidth: Dp = 5.dp,
    val borderTopAlpha: Float = 0.20f,
    val borderBottomAlpha: Float = 0.02f,
    val shadowEnabled: Boolean = true,
    val shadowElevation: Dp = 5.dp,
    val shadowAmbientAlpha: Float = 0.30f,
    val shadowSpotAlpha: Float = 0.22f,
    val softShadowSpread: Dp = 0.dp,
    val softShadowOffsetY: Dp = 4.dp,
    val softShadowAlpha: Float = 0.16f,
    val softShadowColor: Color = Color.Black,
    val contentColor: Color = Color.White,
    val accentEnabled: Boolean = false,
    val accentColor: Color = Color(0xFF00A15C),
    val pageBackgroundTop: Color = Color(0xFF2A1630),
    val pageBackgroundBottom: Color = Color(0xFF0B0D1A),
    val followTouchHighlight: Boolean = true,
    val hideHighlightOnTouch: Boolean = false,
    val overlayBlurRadius: Dp = 24.dp,
    val overlayFallbackAlpha: Float = 0.62f,
    val native: Boolean = false,
) {
    companion object {
        /** 預設使用水滴變體 */
        val Default: GlassConfig = GlassPresets.Drop
    }
}

/**
 * 填充段的配置：進度條的完成區、滑桿的已選區、音量條的已填區。
 *
 * 啟用強調色時是一塊實心的 [GlassConfig.accentColor]；否則沿用玻璃質感，並依表面明暗
 * 選擇對比方向——深色表面往白提亮、淺色表面往黑壓暗。原本一律往白提亮，在淺色主題上
 * 會變成白壓白而看不見填充。
 *
 * @param enabled 元件是否可用；false 時整段以較低不透明度呈現
 */
fun GlassConfig.asFillSurface(enabled: Boolean = true): GlassConfig = if (accentEnabled) {
    copy(
        baseColor = accentColor,
        bodyTopAlpha = if (enabled) 0.95f else 0.30f,
        bodyBottomAlpha = if (enabled) 0.90f else 0.28f,
        // 實色段再疊柔光只會顯髒
        highlightInnerAlpha = 0f,
        highlightOuterAlpha = 0f,
        touchSpotPeakAlpha = 0f,
        borderTopAlpha = 0f,
        borderBottomAlpha = 0f,
        // 實色填充段不需要凹陷感，帶著內陰影反而會在色塊邊緣糊一圈
        innerShadowAlpha = 0f,
        shadowElevation = 0.dp,
    )
} else {
    copy(
        baseColor = lerp(
            baseColor,
            if (isLightSurface) Color.Black else Color.White,
            if (isLightSurface) FILL_DARKEN_ON_LIGHT else FILL_LIGHTEN_ON_DARK
        ),
        bodyTopAlpha = if (enabled) 0.95f else 0.30f,
        bodyBottomAlpha = if (enabled) 0.90f else 0.28f,
        highlightInnerAlpha = if (isLightSurface) 0f else 0.35f,
        highlightOuterAlpha = if (isLightSurface) 0f else 0.10f,
        highlightCenterX = 0.30f,
        highlightCenterY = 0.20f,
        borderTopAlpha = if (isLightSurface) 0f else 0.35f,
        borderBottomAlpha = if (isLightSurface) 0f else 0.05f,
        innerShadowAlpha = 0f,
        shadowElevation = 0.dp,
        followTouchHighlight = false,
    )
}

/**
 * 選中態的表面配置：導航項、Chip、開關軌道等「被選上」的元件。
 *
 * 啟用強調色時是強調色的淡底加同色描邊；否則沿用玻璃質感，並依表面明暗選擇方向——
 * 深色表面把玻璃調亮，淺色表面把玻璃壓暗。原本一律調亮，在淺色主題的近白表面上
 * 沒有提亮空間，選中與否會看不出差別。
 *
 * @param strong 是否要更強的對比（例如開關軌道），false 時只做輕微標示（例如導航項）
 */
fun GlassConfig.asSelectedSurface(strong: Boolean = false): GlassConfig = if (accentEnabled) {
    copy(
        baseColor = accentColor,
        bodyTopAlpha = if (strong) 0.92f else 0.22f,
        bodyBottomAlpha = if (strong) 0.88f else 0.18f,
        highlightInnerAlpha = 0f,
        highlightOuterAlpha = 0f,
        touchSpotPeakAlpha = 0f,
        borderColor = accentColor,
        borderTopAlpha = if (strong) 0.35f else 0.42f,
        borderBottomAlpha = if (strong) 0.15f else 0.25f,
        shadowElevation = 0.dp,
    )
} else if (isLightSurface) {
    copy(
        baseColor = lerp(baseColor, Color.Black, if (strong) SELECTED_DARKEN_STRONG else SELECTED_DARKEN),
        bodyTopAlpha = bodyTopAlpha,
        bodyBottomAlpha = bodyBottomAlpha,
        borderTopAlpha = borderTopAlpha * 1.5f,
        borderBottomAlpha = 0f,
        shadowElevation = 0.dp,
    )
} else {
    copy(
        bodyTopAlpha = (bodyTopAlpha * if (strong) 1.6f else 1.45f).coerceAtMost(0.7f),
        bodyBottomAlpha = (bodyBottomAlpha * if (strong) 1.6f else 1.30f).coerceAtMost(0.7f),
        highlightInnerAlpha = (highlightInnerAlpha * if (strong) 1.6f else 1.3f).coerceAtMost(0.5f),
        borderTopAlpha = borderTopAlpha * 0.75f,
        borderBottomAlpha = 0f,
        shadowElevation = 0.dp,
    )
}

/** 填充段相對於表面色的對比比例 */
private const val FILL_LIGHTEN_ON_DARK = 0.35f
private const val FILL_DARKEN_ON_LIGHT = 0.45f

/**
 * 內嵌控件的表面：按鈕、輸入框、開關軌道、勾選框、清單項、滑桿軌道等。
 *
 * 淺色主題採「毛玻璃容器 + 白色控件」的層次——容器是半透明的，會透出頁面底色而偏灰；
 * 控件則是接近純白的實面，浮在容器之上。兩者若同色（原本都是 baseColor）就會糊在一起。
 *
 * 深色主題不需要這層區分：半透明玻璃壓在深底上本來就有對比，所以直接回傳自己。
 */
fun GlassConfig.asControlSurface(): GlassConfig = if (isLightSurface) {
    copy(
        baseColor = Color.White,
        bodyTopAlpha = CONTROL_ALPHA_ON_LIGHT,
        bodyBottomAlpha = CONTROL_ALPHA_ON_LIGHT,
        // 淺色主題下控件不投影：白控件與灰容器的色差已經足夠分辨，
        // 再讓每個小元件都帶一圈陰影，整頁會佈滿灰暈而顯得髒。
        // 只有容器（卡片、頂欄、導覽列）才投影，這也是參考設計的做法。
        shadowElevation = 0.dp,
    )
} else {
    this
}

/** 淺色主題下內嵌控件的不透明度：要夠實才浮得出容器 */
private const val CONTROL_ALPHA_ON_LIGHT = 0.98f

/** 選中態在淺色表面上的壓暗比例 */
private const val SELECTED_DARKEN = 0.08f
private const val SELECTED_DARKEN_STRONG = 0.30f

/**
 * 這個主題的表面是不是淺色的。
 *
 * 判斷依據是柔光的混合模式：只有淺色表面才需要用 [BlendMode.Multiply] 壓暗
 * （白底上用 [BlendMode.Screen] 提亮是無效的）。元件若要依明暗選擇對比方向
 * ——例如容器要比卡片更深一階——用這個屬性，不要再另外加一個可能與
 * [GlassConfig.highlightColor] 互相矛盾的旗標。
 */
val GlassConfig.isLightSurface: Boolean
    get() = highlightBlendMode == BlendMode.Multiply

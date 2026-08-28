package com.example.liquidglass

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
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
 * @param borderWidth 邊緣描邊寬度
 * @param borderTopAlpha 邊緣描邊頂部透明度
 * @param borderBottomAlpha 邊緣描邊底部透明度
 * @param shadowElevation 接觸陰影高度
 * @param shadowAmbientAlpha 陰影環境光透明度
 * @param shadowSpotAlpha 陰影聚光透明度
 * @param contentColor 建議的文字/圖示顏色（供元件內容使用）
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
    val borderWidth: Dp = 1.dp,
    val borderTopAlpha: Float = 0.20f,
    val borderBottomAlpha: Float = 0.02f,
    val shadowElevation: Dp = 5.dp,
    val shadowAmbientAlpha: Float = 0.30f,
    val shadowSpotAlpha: Float = 0.22f,
    val contentColor: Color = Color.White,
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

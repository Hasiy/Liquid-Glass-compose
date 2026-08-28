package com.example.liquidglass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

/** 按壓時柔光內圈亮度倍率與上限，避免高 alpha 主題按下後過曝 */
private const val PRESS_INNER_BOOST = 1.25f
private const val PRESS_INNER_MAX = 0.22f

/** 按壓時柔光外圈亮度倍率與上限 */
private const val PRESS_OUTER_BOOST = 1.6f
private const val PRESS_OUTER_MAX = 0.09f

/** 按壓時柔光收斂比例：僅輕微收斂，收太緊會讓光斑變成一塊生硬的亮團 */
private const val PRESS_RADIUS_SHRINK = 0.92f

/**
 * 指尖高光的尺寸與亮度。
 *
 * 半徑取「短邊比例」與 [TOUCH_SPOT_MAX_RADIUS] 的較小值：小元件不會溢出，
 * 大卡片也不會因為按比例放大而變成一大團生硬的亮斑。
 * 亮度曲線刻意做成「平頂」（見下方 colorStops）：中心到 0.4 半徑幾乎等亮，
 * 避免出現一個看得出來的亮心。
 */
private const val TOUCH_SPOT_RADIUS_FACTOR = 0.5f
private val TOUCH_SPOT_MAX_RADIUS = 84.dp
private const val TOUCH_SPOT_PEAK_ALPHA = 0.07f

/**
 * 將玻璃質感（基底漸層 + 柔光 + 邊緣描邊 + 接觸陰影）套用到任意元件。
 *
 * 柔光預設在 [GlassConfig.highlightCenterX] / [GlassConfig.highlightCenterY] 位置。
 * 按壓時的預設行為（[GlassConfig.followTouchHighlight] = true、
 * [GlassConfig.hideHighlightOnTouch] = false）是：柔光跟隨手指移動、亮度提高並收斂，
 * 同時在指尖疊一層高光；鬆手後以 spring 動畫淡出並移回默認位置。
 * 若把 [GlassConfig.hideHighlightOnTouch] 設為 true，則改為按住期間淡出柔光。
 * 全程僅以 [PointerEventPass.Initial] 觀察、不消費事件，不影響點擊/拖動手勢。
 *
 * 用法：
 * ```kotlin
 * Button(
 *     modifier = Modifier.glassSurface(shape = RoundedCornerShape(24.dp)),
 *     onClick = { ... }
 * ) { Text("送出") }
 * ```
 *
 * Modifier 順序：`onSizeChanged` → `shadow`（陰影在裁切外側）→ `clip`（內容裁成形狀）→
 * `drawBehind`（基底與柔光）→ `border`（內部描邊）→ `pointerInput`（觸摸觀察）。
 *
 * 注意：需要漣漪（indication）的點擊 modifier 必須放在 `glassSurface` **之後**，
 * 否則漣漪畫在 `clip` 之前，會以直角矩形溢出圓角。
 *
 * @param shape 玻璃形狀（圓角等）
 * @param config 玻璃主題參數
 */
@Composable
fun Modifier.glassSurface(
    shape: Shape = RoundedCornerShape(16.dp),
    config: GlassConfig = GlassConfig.Default,
): Modifier {
    var isTouching by remember { mutableStateOf(false) }
    var touchX by remember { mutableFloatStateOf(0f) }
    var touchY by remember { mutableFloatStateOf(0f) }
    var sizePx by remember { mutableStateOf(IntSize.Zero) }

    // 原生模式不追蹤觸摸；其餘一律追蹤，讓每個玻璃元件都有觸摸光影。
    val trackTouch = !config.native

    // 按壓進度：0=未按壓（默認柔光），1=按壓中（光斑跟手、亮度提高）。
    val pressProgress by animateFloatAsState(
        targetValue = if (isTouching && !config.hideHighlightOnTouch) 1f else 0f,
        animationSpec = spring(stiffness = 240f, dampingRatio = 1f),
        label = "glassPressProgress"
    )
    // 隱藏進度：僅在 hideHighlightOnTouch = true 時作用，1=柔光完全淡出。
    val hideProgress by animateFloatAsState(
        targetValue = if (isTouching && config.hideHighlightOnTouch) 1f else 0f,
        animationSpec = spring(stiffness = 240f, dampingRatio = 1f),
        label = "glassHideProgress"
    )

    // 目標柔光中心：按壓時為手指位置（像素）；釋放後回到默認位置。
    // 用 spring 平滑過渡，讓鬆手時光斑「緩緩移回」默認位置，而非瞬間跳回。
    val followsTouch = isTouching && config.followTouchHighlight && !config.hideHighlightOnTouch
    val targetCenterX = if (followsTouch) touchX else sizePx.width * config.highlightCenterX
    val targetCenterY = if (followsTouch) touchY else sizePx.height * config.highlightCenterY
    val glowCenterX by animateFloatAsState(
        targetValue = targetCenterX,
        animationSpec = spring(stiffness = 230f, dampingRatio = 0.95f),
        label = "glassGlowCenterX"
    )
    val glowCenterY by animateFloatAsState(
        targetValue = targetCenterY,
        animationSpec = spring(stiffness = 230f, dampingRatio = 0.95f),
        label = "glassGlowCenterY"
    )

    val touchObserver = if (trackTouch) {
        Modifier.pointerInput(Unit) {
            try {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val pressed = event.changes.firstOrNull { it.pressed }
                        if (pressed != null) {
                            isTouching = true
                            touchX = pressed.position.x
                            touchY = pressed.position.y
                        } else {
                            isTouching = false
                        }
                    }
                }
            } finally {
                isTouching = false
            }
        }
    } else {
        Modifier
    }

    return this
        .onSizeChanged { sizePx = it }
        .shadow(
            elevation = config.shadowElevation,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = config.shadowAmbientAlpha),
            spotColor = Color.Black.copy(alpha = config.shadowSpotAlpha)
        )
        .clip(shape)
        .drawBehind {
            // 玻璃基底：半透明白垂直漸層（頂亮 → 底聚光）
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to config.baseColor.copy(alpha = config.bodyTopAlpha),
                        1f to config.baseColor.copy(alpha = config.bodyBottomAlpha)
                    )
                ),
                topLeft = Offset.Zero,
                size = size
            )
            if (size.minDimension <= 0f) return@drawBehind

            val glowCenter = Offset(glowCenterX, glowCenterY)
            val visible = 1f - hideProgress
            // 柔光：按壓時提高亮度並略微收斂，鬆手後回到默認亮度與半徑。
            val innerAlpha = lerp(
                config.highlightInnerAlpha,
                (config.highlightInnerAlpha * PRESS_INNER_BOOST).coerceAtMost(PRESS_INNER_MAX),
                pressProgress
            ) * visible
            val outerAlpha = lerp(
                config.highlightOuterAlpha,
                (config.highlightOuterAlpha * PRESS_OUTER_BOOST).coerceAtMost(PRESS_OUTER_MAX),
                pressProgress
            ) * visible
            val radiusFactor = lerp(
                config.highlightRadiusFactor,
                config.highlightRadiusFactor * PRESS_RADIUS_SHRINK,
                pressProgress
            )
            val glowRadius = size.width * radiusFactor
            if (glowRadius > 0f && (innerAlpha > 0f || outerAlpha > 0f)) {
                drawRect(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to Color.White.copy(alpha = innerAlpha),
                            0.45f to Color.White.copy(alpha = outerAlpha),
                            1f to Color.Transparent
                        ),
                        center = glowCenter,
                        radius = glowRadius
                    ),
                    topLeft = Offset.Zero,
                    size = size,
                    blendMode = BlendMode.Screen
                )
            }
            // 指尖高光：疊在柔光之上的小範圍亮點，讓「跟手」的感覺更明確。
            // 用多段近似高斯的衰減取代線性三段，避免光斑邊緣出現可見的硬邊。
            if (pressProgress > 0.01f) {
                val peak = TOUCH_SPOT_PEAK_ALPHA * pressProgress
                val spotRadius = minOf(
                    size.minDimension * TOUCH_SPOT_RADIUS_FACTOR,
                    TOUCH_SPOT_MAX_RADIUS.toPx()
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to Color.White.copy(alpha = peak),
                            0.4f to Color.White.copy(alpha = peak * 0.92f),
                            0.62f to Color.White.copy(alpha = peak * 0.50f),
                            0.82f to Color.White.copy(alpha = peak * 0.16f),
                            1f to Color.Transparent
                        ),
                        center = glowCenter,
                        radius = spotRadius
                    ),
                    topLeft = Offset.Zero,
                    size = size,
                    blendMode = BlendMode.Screen
                )
            }
        }
        .border(
            width = config.borderWidth,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = config.borderTopAlpha),
                    Color.White.copy(alpha = config.borderBottomAlpha)
                )
            ),
            shape = shape
        )
        .then(touchObserver)
}

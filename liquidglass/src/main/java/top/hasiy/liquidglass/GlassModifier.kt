package top.hasiyliquidglass

import android.graphics.BlurMaskFilter
import android.os.Build
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.exp

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
 * 避免出現一個看得出來的亮心。峰值亮度由 [GlassConfig.touchSpotPeakAlpha] 決定。
 */
private const val TOUCH_SPOT_RADIUS_FACTOR = 0.5f

/** 內陰影的疊加層數：層數越多漸層越細膩，代價是每次繪製多幾次 drawOutline */
private const val INNER_SHADOW_LAYERS = 8

/** 細長元件開始淡出方向性漸層的長短邊比例。 */
private const val DIRECTIONAL_EFFECT_FADE_START = 1.6f

/** 到達此長短邊比例後完全改用均勻玻璃底，避免形成橫向或縱向色帶。 */
private const val DIRECTIONAL_EFFECT_FADE_END = 2.5f

/** 柔和外陰影的疊加層數 */
private const val SOFT_SHADOW_LAYERS = 18

/** 柔和外陰影的高斯衰減係數：越大收得越緊，越小擴散越開（僅 API 27 以下的退路使用） */
private const val SOFT_SHADOW_FALLOFF = 3f

/**
 * BlurMaskFilter 的半徑相對 [GlassConfig.softShadowSpread] 的比例。
 *
 * 模糊是雙向擴散的，取一半左右才對得上設定的擴散範圍。
 *
 * 這個值會連帶影響陰影的深淺：高斯模糊是把固定的濃度攤開，調大擴散就會同時變淡
 * （0.55 → 0.85 時得把 [GlassConfig.softShadowAlpha] 從 0.032 提到 0.075 才補得回來）。
 * 想單獨調深淺請改 alpha，不要動這裡。
 */
private const val SOFT_SHADOW_BLUR_FACTOR = 0.55f
private val TOUCH_SPOT_MAX_RADIUS = 84.dp

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

    val measuredElongation = if (sizePx.width > 0 && sizePx.height > 0) {
        maxOf(
            sizePx.width.toFloat() / sizePx.height,
            sizePx.height.toFloat() / sizePx.width
        )
    } else {
        1f
    }
    val suppressElongatedShadow = measuredElongation >= DIRECTIONAL_EFFECT_FADE_END

    // 原生模式不追蹤觸摸；其餘一律追蹤，讓每個玻璃元件都有觸摸光影。
    val trackTouch = !config.native

    // 柔和外陰影與 shadowElevation 連動：把 elevation 設為 0 的元件（填充段、選中態、
    // 滑桿軌道…）本來就不該投影，這樣就不必逐個再關一次 softShadowSpread。
    val useSoftShadow = config.shadowEnabled &&
        config.softShadowSpread > 0.dp &&
        config.shadowElevation > 0.dp &&
        !suppressElongatedShadow

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
        .then(
            if (config.shadowEnabled && !useSoftShadow && !suppressElongatedShadow) {
                Modifier.shadow(
                    elevation = config.shadowElevation,
                    shape = shape,
                    ambientColor = Color.Black.copy(alpha = config.shadowAmbientAlpha),
                    spotColor = Color.Black.copy(alpha = config.shadowSpotAlpha)
                )
            } else {
                Modifier
            }
        )
        .then(
            // 柔和外陰影畫在 clip 之前，才能落在形狀外側。
            // 做法與內陰影相同——多層同心 Stroke 疊加，只是這裡把形狀內部裁掉，
            // 讓陰影只留在外側；越靠近邊緣被越多層覆蓋，向外自然衰減。
            if (useSoftShadow) {
                Modifier.drawBehind {
                    if (size.minDimension <= 0f) return@drawBehind
                    val outline = shape.createOutline(size, layoutDirection, this)
                    val shadowPath = Path().apply { addOutline(outline) }
                    val spread = config.softShadowSpread.toPx()
                    if (spread <= 0f) return@drawBehind
                    val offsetY = config.softShadowOffsetY.toPx()
                    val shadowColor = config.softShadowColor.copy(alpha = config.softShadowAlpha)

                    // 形狀內部裁掉，陰影只留在外側；否則半透明的容器基底會透出陰影
                    clipPath(shadowPath, ClipOp.Difference) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            // 真高斯模糊：一次繪製，邊緣沒有任何近似造成的階梯
                            drawIntoCanvas { canvas ->
                                val paint = Paint().asFrameworkPaint().apply {
                                    isAntiAlias = true
                                    color = shadowColor.toArgb()
                                    maskFilter = BlurMaskFilter(
                                        spread * SOFT_SHADOW_BLUR_FACTOR,
                                        BlurMaskFilter.Blur.NORMAL
                                    )
                                }
                                val nativeCanvas = canvas.nativeCanvas
                                val checkpoint = nativeCanvas.save()
                                nativeCanvas.translate(0f, offsetY)
                                nativeCanvas.drawPath(shadowPath.asAndroidPath(), paint)
                                nativeCanvas.restoreToCount(checkpoint)
                            }
                        } else {
                            // API 27 以下的硬體加速 Canvas 會忽略 BlurMaskFilter，
                            // 退回多層同心 Stroke 疊加。每層權重取高斯的導數，
                            // 累積後濃度是 exp(-k·d²)，近處平緩、遠處才快速趨零。
                            val layerAlpha = config.softShadowAlpha / SOFT_SHADOW_LAYERS
                            translate(top = offsetY) {
                                repeat(SOFT_SHADOW_LAYERS) { index ->
                                    val t = (index + 1f) / SOFT_SHADOW_LAYERS
                                    val alpha = layerAlpha * SOFT_SHADOW_FALLOFF * 2f * t *
                                        exp(-SOFT_SHADOW_FALLOFF * t * t)
                                    if (alpha > 0f) {
                                        drawOutline(
                                            outline = outline,
                                            color = config.softShadowColor.copy(alpha = alpha),
                                            style = Stroke(width = spread * t * 2f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Modifier
            }
        )
        .clip(shape)
        .drawBehind {
            if (size.minDimension <= 0f) return@drawBehind

            // 同一套方向性漸層放到輸入框、長按區域或窄高導覽列時，會沿長邊被看成
            // 一整條色差。依長短邊比例平滑淡出；一般卡片與接近方形的元件仍保留光感。
            val elongation = maxOf(
                size.width / size.height,
                size.height / size.width
            )
            val directionalEffectScale = when {
                elongation <= DIRECTIONAL_EFFECT_FADE_START -> 1f
                elongation >= DIRECTIONAL_EFFECT_FADE_END -> 0f
                else -> (DIRECTIONAL_EFFECT_FADE_END - elongation) /
                    (DIRECTIONAL_EFFECT_FADE_END - DIRECTIONAL_EFFECT_FADE_START)
            }
            val averageBodyAlpha = (config.bodyTopAlpha + config.bodyBottomAlpha) / 2f
            val resolvedBodyTopAlpha = lerp(
                averageBodyAlpha,
                config.bodyTopAlpha,
                directionalEffectScale
            )
            val resolvedBodyBottomAlpha = lerp(
                averageBodyAlpha,
                config.bodyBottomAlpha,
                directionalEffectScale
            )

            // 玻璃基底：半透明白垂直漸層（頂亮 → 底聚光）
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to config.baseColor.copy(alpha = resolvedBodyTopAlpha),
                        1f to config.baseColor.copy(alpha = resolvedBodyBottomAlpha)
                    )
                ),
                topLeft = Offset.Zero,
                size = size
            )

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
            if (
                glowRadius > 0f &&
                (innerAlpha > 0f || outerAlpha > 0f) &&
                directionalEffectScale > 0f
            ) {
                drawRect(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to config.highlightColor.copy(
                                alpha = innerAlpha * directionalEffectScale
                            ),
                            0.45f to config.highlightColor.copy(
                                alpha = outerAlpha * directionalEffectScale
                            ),
                            1f to Color.Transparent
                        ),
                        center = glowCenter,
                        radius = glowRadius
                    ),
                    topLeft = Offset.Zero,
                    size = size,
                    blendMode = config.highlightBlendMode
                )
            }
            // 指尖高光：疊在柔光之上的小範圍亮點，讓「跟手」的感覺更明確。
            // 用多段近似高斯的衰減取代線性三段，避免光斑邊緣出現可見的硬邊。
            if (pressProgress > 0.01f) {
                val peak = config.touchSpotPeakAlpha * pressProgress
                val spotRadius = minOf(
                    size.minDimension * TOUCH_SPOT_RADIUS_FACTOR,
                    TOUCH_SPOT_MAX_RADIUS.toPx()
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to config.highlightColor.copy(alpha = peak),
                            0.4f to config.highlightColor.copy(alpha = peak * 0.92f),
                            0.62f to config.highlightColor.copy(alpha = peak * 0.50f),
                            0.82f to config.highlightColor.copy(alpha = peak * 0.16f),
                            1f to Color.Transparent
                        ),
                        center = glowCenter,
                        radius = spotRadius
                    ),
                    topLeft = Offset.Zero,
                    size = size,
                    blendMode = config.highlightBlendMode
                )
            }
        }
        .drawWithContent {
            drawContent()
            // 邊緣內陰影：畫在內容之上，沿輪廓由外往內衰減。
            // 多層同心 Stroke 疊加而成——Stroke 是置中對齊的，外半部分會被 clip 裁掉，
            // 剩下的內半部分層層相疊，越靠邊緣疊得越多，自然形成由深到淺的漸層。
            if (config.innerShadowAlpha > 0f && size.minDimension > 0f) {
                val outline = shape.createOutline(size, layoutDirection, this)
                val maxWidth = config.innerShadowWidth.toPx()
                val layerAlpha = config.innerShadowAlpha / INNER_SHADOW_LAYERS
                repeat(INNER_SHADOW_LAYERS) { index ->
                    val strokeWidth =
                        maxWidth * (INNER_SHADOW_LAYERS - index) / INNER_SHADOW_LAYERS * 2f
                    if (strokeWidth > 0f) {
                        drawOutline(
                            outline = outline,
                            color = config.borderColor.copy(alpha = layerAlpha),
                            style = Stroke(width = strokeWidth)
                        )
                    }
                }
            }
        }
        .border(
            width = config.borderWidth,
            brush = Brush.verticalGradient(
                colors = listOf(
                    config.borderColor.copy(alpha = config.borderTopAlpha),
                    config.borderColor.copy(alpha = config.borderBottomAlpha)
                )
            ),
            shape = shape
        )
        .then(touchObserver)
}

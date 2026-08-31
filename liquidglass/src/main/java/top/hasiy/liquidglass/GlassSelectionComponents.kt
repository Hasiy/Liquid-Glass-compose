package top.hasiyliquidglass

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val CHECKBOX_SIZE = 24.dp
private val CHECKBOX_SHAPE = RoundedCornerShape(6.dp)

/**
 * 勾號畫布相對於方框的放大倍率。
 *
 * 勾號畫在方框的 **外層**（不受 [glassSurface] 的 clip 影響），畫布比方框大一圈，
 * 收筆處便能自然衝出方框右上角，做出手寫勾的感覺。
 */
private const val CHECKMARK_CANVAS_SCALE = 1.4f
private val RADIO_SIZE = 24.dp
private val SLIDER_TRACK_HEIGHT = 8.dp
private val SLIDER_THUMB_SIZE = 20.dp
private const val DISABLED_ALPHA = 0.38f

/**
 * 玻璃複選框（Checkbox）：玻璃質感的勾選元件。
 *
 * @param checked 是否勾選
 * @param onCheckedChange 勾選狀態變更回呼（null 時不可互動）
 * @param modifier 外部修飾符
 * @param config 玻璃主題參數
 * @param enabled 是否可用
 */
@Composable
fun GlassCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    enabled: Boolean = true,
) {
    if (config.native) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled
        )
        return
    }

    val contentColor = config.contentColor
    val iconAlpha = if (enabled) 1f else DISABLED_ALPHA
    val boxShape = CHECKBOX_SHAPE
    val boxConfig = if (checked) {
        config.asSelectedSurface(strong = true).copy(shadowElevation = 0.dp)
    } else {
        config.asControlSurface().copy(shadowElevation = 0.dp)
    }
    val checkmarkColor = if (checked && config.accentEnabled) {
        Color.White
    } else {
        contentColor
    }

    Box(
        modifier = modifier.size(CHECKBOX_SIZE),
        contentAlignment = Alignment.Center
    ) {
        // 方框本身：clip 只作用在這一層，勾號不受影響
        Box(
            modifier = Modifier
                .matchParentSize()
                // 淺色主題下要比所在卡片壓暗一階，方框才有輪廓
                .glassSurface(shape = boxShape, config = boxConfig)
                .clickable(
                    enabled = enabled && onCheckedChange != null,
                    onClick = { onCheckedChange?.invoke(!checked) }
                )
        )
        // 勾號：畫布比方框大一圈，收筆處溢出右上角
        Canvas(modifier = Modifier.size(CHECKBOX_SIZE * CHECKMARK_CANVAS_SCALE)) {
            drawCheckmark(
                color = checkmarkColor,
                alpha = if (checked) iconAlpha else 0f,
                sizePx = this.size.minDimension
            )
        }
    }
}

/**
 * 畫出勾號。
 *
 * 座標以畫布為基準，而畫布比方框大 [CHECKMARK_CANVAS_SCALE] 倍且置中，因此
 * 靠近畫布右上角的收筆點會落在方框之外，形成「勾稍微超出框」的效果。
 *
 * @param color 勾號顏色
 * @param alpha 勾號透明度，0 表示不繪製
 * @param sizePx 畫布短邊像素長度
 */
private fun DrawScope.drawCheckmark(color: Color, alpha: Float, sizePx: Float) {
    if (alpha <= 0f) return
    val strokeWidth = sizePx * 0.115f
    val path = Path().apply {
        // 起筆在框內左側，轉折在框內偏下，收筆衝出方框右上角
        val left = sizePx * 0.20f
        val midY = sizePx * 0.52f
        val midX = sizePx * 0.42f
        val bottom = sizePx * 0.72f
        val right = sizePx * 0.97f
        val top = sizePx * 0.07f

        moveTo(left, midY)
        lineTo(midX, bottom)
        lineTo(right, top)
    }
    drawPath(
        path = path,
        color = color.copy(alpha = alpha),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
}

/**
 * 玻璃單選按鈕（RadioButton）：玻璃質感的單選元件。
 *
 * @param selected 是否選中
 * @param onClick 點擊回呼（null 時不可互動）
 * @param modifier 外部修飾符
 * @param config 玻璃主題參數
 * @param enabled 是否可用
 */
@Composable
fun GlassRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    enabled: Boolean = true,
) {
    if (config.native) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled
        )
        return
    }

    val contentColor = config.contentColor
    val iconAlpha = if (enabled) 1f else DISABLED_ALPHA
    val radioConfig = if (selected) {
        config.asSelectedSurface().copy(shadowElevation = 0.dp)
    } else {
        config.asControlSurface().copy(shadowElevation = 0.dp)
    }
    val indicatorColor = if (selected && config.accentEnabled) {
        config.accentColor
    } else {
        contentColor
    }

    Box(
        modifier = modifier
            .size(RADIO_SIZE)
            .glassSurface(shape = CircleShape, config = radioConfig)
            .clickable(
                enabled = enabled && onClick != null,
                onClick = { onClick?.invoke() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(RADIO_SIZE)) {
            val strokeWidth = sizePx * 0.08f
            drawCircle(
                color = indicatorColor.copy(alpha = iconAlpha),
                radius = (sizePx / 2f) - strokeWidth,
                style = Stroke(width = strokeWidth)
            )
            if (selected) {
                val dotRadius = (sizePx / 2f) * 0.38f
                drawCircle(
                    color = indicatorColor.copy(alpha = iconAlpha),
                    radius = dotRadius
                )
            }
        }
    }
}

private val DrawScope.sizePx: Float
    get() = this.size.minDimension

/**
 * 玻璃滑桿（Slider）：玻璃質感的數值滑桿。
 *
 * @param value 目前數值
 * @param onValueChange 數值變更回呼
 * @param modifier 外部修飾符
 * @param config 玻璃主題參數
 * @param enabled 是否可用
 * @param valueRange 數值範圍
 * @param steps 區間內的離散步數（不含起訖點）
 */
@Composable
fun GlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
) {
    if (config.native) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            valueRange = valueRange,
            steps = steps
        )
        return
    }

    val trackShape = RoundedCornerShape(SLIDER_TRACK_HEIGHT / 2)
    val contentColor = config.contentColor
    // 已選段：見 GlassConfig.asFillSurface
    val fillConfig = config.asFillSurface(enabled)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(SLIDER_THUMB_SIZE),
        contentAlignment = Alignment.CenterStart
    ) {
        val trackWidth = maxWidth - SLIDER_THUMB_SIZE
        val rangeLength = valueRange.endInclusive - valueRange.start
        val fraction = ((value - valueRange.start) / rangeLength).coerceIn(0f, 1f)

        var dragOffset by remember { mutableFloatStateOf(0f) }
        val thumbOffset by animateDpAsState(
            targetValue = trackWidth * fraction,
            label = "glassSliderThumb"
        )

        val updateValueFromFraction: (Float) -> Unit = remember(valueRange, steps) {
            { newFraction ->
                val clampedFraction = newFraction.coerceIn(0f, 1f)
                val snappedFraction = if (steps > 0) {
                    val stepFraction = 1f / (steps + 1)
                    val stepIndex = (clampedFraction / stepFraction).roundToInt()
                    (stepIndex * stepFraction).coerceIn(0f, 1f)
                } else {
                    clampedFraction
                }
                onValueChange(valueRange.start + snappedFraction * rangeLength)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(SLIDER_TRACK_HEIGHT)
                .glassSurface(
                    shape = trackShape,
                    config = config.asControlSurface().copy(shadowElevation = 0.dp)
                )
                .then(
                    if (enabled) {
                        Modifier.pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val newFraction = offset.x / size.width.toFloat()
                                updateValueFromFraction(newFraction)
                            }
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(SLIDER_TRACK_HEIGHT)
                    .glassSurface(shape = trackShape, config = fillConfig)
            )
        }

        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(SLIDER_THUMB_SIZE)
                .glassSurface(shape = CircleShape, config = config.asControlSurface())
                .then(
                    if (enabled) {
                        Modifier.pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = { dragOffset = 0f },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffset += dragAmount
                                    val newFraction = fraction + dragOffset / trackWidth.toPx()
                                    updateValueFromFraction(newFraction)
                                }
                            )
                        }
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(SLIDER_THUMB_SIZE * 0.4f)) {
                drawCircle(
                    color = contentColor.copy(alpha = if (enabled) 1f else DISABLED_ALPHA)
                )
            }
        }
    }
}

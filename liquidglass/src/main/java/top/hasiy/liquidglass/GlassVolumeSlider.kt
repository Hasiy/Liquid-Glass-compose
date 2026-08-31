package top.hasiyliquidglass

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 音量條預設高度：夠粗才好直接用手指推 */
private val VOLUME_SLIDER_HEIGHT = 44.dp

/** 音量條內圖示的大小與左內距 */
private val VOLUME_SLIDER_ICON_SIZE = 20.dp
private val VOLUME_SLIDER_ICON_PADDING = 14.dp

/** 停用狀態的內容透明度 */
private const val VOLUME_SLIDER_DISABLED_ALPHA = 0.38f

/**
 * 玻璃音量條：像系統音量控制那樣，整條軌道都能直接按與拖。
 *
 * 與 [GlassSlider] 的差別是沒有獨立的圓鈕——按在軌道任一點即跳到該值，
 * 手指滑動時數值即時跟隨，鬆手才觸發 [onValueChangeFinished]。
 * 已填充的部分是一層較亮的玻璃，未填充的部分是軌道本身的玻璃。
 *
 * 用法：
 * ```kotlin
 * var volume by remember { mutableFloatStateOf(0.6f) }
 * GlassVolumeSlider(
 *     value = volume,
 *     onValueChange = { volume = it },
 *     icon = Icons.Filled.VolumeUp
 * )
 * ```
 *
 * @param value 目前數值，會被夾在 [valueRange] 內
 * @param onValueChange 拖動過程中的數值變更回呼（連續觸發）
 * @param modifier 外部修飾符
 * @param config 玻璃主題參數
 * @param enabled 是否可互動；false 時不接受手勢並以較低透明度呈現
 * @param valueRange 數值範圍
 * @param height 軌道高度
 * @param icon 軌道左側的圖示（可為 null）
 * @param onValueChangeFinished 手指離開軌道時觸發，適合用來提交最終值
 */
@Composable
fun GlassVolumeSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    height: Dp = VOLUME_SLIDER_HEIGHT,
    icon: ImageVector? = null,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    if (config.native) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            valueRange = valueRange,
            onValueChangeFinished = onValueChangeFinished
        )
        return
    }

    val trackShape = RoundedCornerShape(height / 2)
    val rangeLength = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
    val fraction = ((value - valueRange.start) / rangeLength).coerceIn(0f, 1f)
    val contentAlpha = if (enabled) 1f else VOLUME_SLIDER_DISABLED_ALPHA

    // 已填充段：見 GlassConfig.asFillSurface
    val fillConfig = config.asFillSurface(enabled)

    // 手勢回呼在 pointerInput 存活期間不重啟，用 rememberUpdatedState 取最新值
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnFinished by rememberUpdatedState(onValueChangeFinished)
    val currentRangeStart by rememberUpdatedState(valueRange.start)
    val currentRangeLength by rememberUpdatedState(rangeLength)

    val gestureModifier = if (enabled) {
        Modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val width = size.width.toFloat()
                if (width <= 0f) return@awaitEachGesture

                fun emit(x: Float) {
                    val f = (x / width).coerceIn(0f, 1f)
                    currentOnValueChange(currentRangeStart + f * currentRangeLength)
                }
                emit(down.position.x)
                down.consume()

                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    if (change.positionChange().x != 0f) {
                        emit(change.position.x)
                    }
                    change.consume()
                }
                currentOnFinished?.invoke()
            }
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .glassSurface(
                shape = trackShape,
                config = config.asControlSurface().copy(shadowElevation = 0.dp)
            )
            .then(gestureModifier),
        contentAlignment = Alignment.CenterStart
    ) {
        // 填充段用直角矩形，左端由軌道的 clip 裁出圓角，右端保持齊平，貼近系統音量條
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .glassSurface(shape = RectangleShape, config = fillConfig)
            )
        }
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(start = VOLUME_SLIDER_ICON_PADDING)
                    .size(VOLUME_SLIDER_ICON_SIZE),
                tint = config.contentColor.copy(alpha = contentAlpha)
            )
        }
    }
}

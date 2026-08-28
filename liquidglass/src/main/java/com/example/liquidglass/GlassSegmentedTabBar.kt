package com.example.liquidglass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * 可拖動、可點擊的玻璃分段 Tab Bar。這是 SDK 版本，不依賴 Demo app 的資源或 package。
 */
@Composable
fun GlassSegmentedTabBar(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    height: Dp = 72.dp,
) {
    if (items.isEmpty()) return

    val safeSelectedIndex = selectedIndex.coerceIn(items.indices)
    val density = LocalDensity.current
    var widthPx by remember { mutableFloatStateOf(0f) }
    var dragX by remember { mutableFloatStateOf(Float.NaN) }
    val itemWidthPx = widthPx / items.size
    val targetOffsetPx = if (!dragX.isNaN() && itemWidthPx > 0f) {
        (dragX - itemWidthPx / 2f).coerceIn(0f, widthPx - itemWidthPx)
    } else {
        safeSelectedIndex * itemWidthPx
    }
    val indicatorOffsetPx by animateFloatAsState(
        targetValue = targetOffsetPx,
        animationSpec = spring(stiffness = 300f, dampingRatio = 0.82f),
        label = "glassSegmentedTabOffset"
    )
    val itemWidth = with(density) { itemWidthPx.toDp() }
    val barShape = RoundedCornerShape(height / 2)
    val indicatorShape = RoundedCornerShape((height - 8.dp) / 2)
    val barConfig = config.copy(
        baseColor = Color(0xFF15151A),
        bodyTopAlpha = 0.55f,
        bodyBottomAlpha = 0.62f,
        highlightInnerAlpha = config.highlightInnerAlpha * 0.45f,
        shadowElevation = 10.dp,
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .onSizeChanged { widthPx = it.width.toFloat() }
            .glassSurface(shape = barShape, config = barConfig)
            .pointerInput(items.size, widthPx) {
                detectDragGestures(
                    onDragStart = { dragX = it.x.coerceIn(0f, widthPx) },
                    onDrag = { change, _ ->
                        change.consume()
                        dragX = change.position.x.coerceIn(0f, widthPx)
                    },
                    onDragEnd = {
                        if (!dragX.isNaN() && widthPx > 0f) {
                            onSelect(
                                ((dragX / widthPx) * items.size)
                                    .toInt()
                                    .coerceIn(items.indices)
                            )
                        }
                        dragX = Float.NaN
                    },
                    onDragCancel = { dragX = Float.NaN }
                )
            }
    ) {
        if (itemWidthPx > 0f) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(indicatorOffsetPx.roundToInt(), 0) }
                    .width(itemWidth)
                    .fillMaxHeight()
                    .padding(4.dp)
                    .glassSurface(shape = indicatorShape, config = config)
            )
        }

        Row(modifier = Modifier.fillMaxSize()) {
            items.forEachIndexed { index, label ->
                val selected = index == safeSelectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(index) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = config.contentColor.copy(alpha = if (selected) 1f else 0.55f),
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package top.hasiyliquidglass

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt

private val CONTEXT_MENU_SHAPE = RoundedCornerShape(14.dp)

/**
 * 可長按開啟的玻璃 Context Menu 區域。
 *
 * 選單會以長按點為起點，並自動限制在目前視窗範圍內。若目標本身也需要一般點擊，使用
 * [onClick] 傳入同一個操作，避免外層長按語意吞掉點擊。
 */
@Composable
fun GlassContextMenuArea(
    menuContent: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
    onLongClickLabel: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var pressPosition by remember { mutableStateOf(Offset.Zero) }
    var areaSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .onGloballyPositioned { areaSize = it.size }
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClickLabel = onLongClickLabel,
                onLongClick = {
                    // combinedClickable 不暴露按壓座標；使用區域中心作為穩定錨點。
                    pressPosition = Offset(areaSize.width / 2f, areaSize.height / 2f)
                    expanded = true
                }
            ),
        content = content
    )

    if (expanded) {
        val positionProvider = remember(pressPosition) {
            ContextMenuPositionProvider(
                relativeOffset = IntOffset(
                    pressPosition.x.roundToInt(),
                    pressPosition.y.roundToInt()
                )
            )
        }
        Popup(
            popupPositionProvider = positionProvider,
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = true)
        ) {
            val menuModifier = if (config.native) {
                Modifier
            } else {
                Modifier
                    .glassOverlayBackdrop(shape = CONTEXT_MENU_SHAPE, config = config)
                    .glassSurface(shape = CONTEXT_MENU_SHAPE, config = config)
            }
            Surface(
                modifier = menuModifier,
                shape = CONTEXT_MENU_SHAPE,
                color = if (config.native) MaterialTheme.colorScheme.surfaceContainer
                else Color.Transparent,
                contentColor = if (config.native) MaterialTheme.colorScheme.onSurface
                else config.contentColor,
                shadowElevation = if (config.native) 6.dp else 0.dp,
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    menuContent { expanded = false }
                }
            }
        }
    }
}

private class ContextMenuPositionProvider(
    private val relativeOffset: IntOffset,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val maxY = (windowSize.height - popupContentSize.height).coerceAtLeast(0)
        return IntOffset(
            x = (anchorBounds.left + relativeOffset.x).coerceIn(0, maxX),
            y = (anchorBounds.top + relativeOffset.y).coerceIn(0, maxY)
        )
    }
}

@file:OptIn(ExperimentalMaterial3Api::class)

package top.hasiy.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupPositionProvider

// GlassSearchBar / GlassDockedSearchBar 已移到 androidMain 的 GlassSearchBarComponents.kt
// （CMP material3 common 沒有 SearchBar / DockedSearchBar），見遷移方案 §4-E。

/**
 * 玻璃風格下拉選單（DropdownMenu）。
 *
 * @param expanded 是否展開
 * @param onDismissRequest 點擊外部或返回鍵時觸發
 * @param modifier 外部修飾符
 * @param config 玻璃主題參數
 * @param content 選單項目內容
 */
@Composable
fun GlassDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (config.native) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            content = content
        )
        return
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
            .glassOverlayBackdrop(shape = RoundedCornerShape(12.dp), config = config.asOverlaySurface())
            .glassSurface(shape = RoundedCornerShape(12.dp), config = config.asOverlaySurface()),
        shape = RoundedCornerShape(12.dp),
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        content = content
    )
}

/**
 * 玻璃風格下拉選單項目（DropdownMenuItem）。
 *
 * @param text 項目文字
 * @param onClick 點擊回呼
 * @param modifier 外部修飾符
 * @param config 玻璃主題參數
 * @param leadingIcon 前導圖示
 * @param trailingIcon 尾端圖示
 * @param enabled 是否可點擊
 */
@Composable
fun GlassDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    if (config.native) {
        DropdownMenuItem(
            text = { Text(text) },
            onClick = onClick,
            modifier = modifier,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            enabled = enabled
        )
        return
    }

    val alpha = if (enabled) 1f else 0.38f
    val contentColor = config.contentColor.copy(alpha = alpha)

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Row(
            // clickable 放在 glassSurface 之後，漣漪才會被 clip 進 8.dp 圓角。
            modifier = modifier
                .fillMaxWidth()
                .glassSurface(shape = RoundedCornerShape(8.dp), config = config.asControlSurface())
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            leadingIcon?.invoke()
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                fontSize = 15.sp,
                color = contentColor
            )
            trailingIcon?.invoke()
        }
    }
}

/**
 * 玻璃風格外露下拉選單容器（ExposedDropdownMenuBox）。
 *
 * @param expanded 是否展開
 * @param onExpandedChange 展開狀態變更回呼
 * @param modifier 外部修飾符
 * @param config 玻璃主題參數
 * @param content 內部內容，通常包含輸入框與 [GlassDropdownMenu]
 */
@Composable
fun GlassExposedDropdownMenuBox(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    content: @Composable androidx.compose.material3.ExposedDropdownMenuBoxScope.() -> Unit,
) {
    val boxModifier = if (config.native) {
        modifier
    } else {
        modifier.glassSurface(shape = RoundedCornerShape(16.dp), config = config.asControlSurface())
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = boxModifier,
        content = content
    )
}

/**
 * 玻璃風格提示框（TooltipBox）。
 *
 * @param modifier 外部修飾符
 * @param config 玻璃主題參數
 * @param tooltip 提示內容
 * @param state Tooltip 狀態
 * @param positionProvider 提示定位提供者
 * @param content 被提示的目標內容
 */
@Composable
fun GlassTooltipBox(
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    tooltip: @Composable () -> Unit,
    state: TooltipState,
    positionProvider: PopupPositionProvider,
    content: @Composable () -> Unit,
) {
    val tooltipContent: @Composable () -> Unit = if (config.native) {
        tooltip
    } else {
        {
            Surface(
                modifier = Modifier
                    .glassOverlayBackdrop(shape = RoundedCornerShape(8.dp), config = config.asOverlaySurface())
                    .glassSurface(shape = RoundedCornerShape(8.dp), config = config.asOverlaySurface()),
                color = Color.Transparent,
                contentColor = config.contentColor,
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(modifier = Modifier.padding(8.dp)) {
                    tooltip()
                }
            }
        }
    }

    TooltipBox(
        positionProvider = positionProvider,
        tooltip = { tooltipContent() },
        state = state,
        modifier = modifier,
        content = content
    )
}

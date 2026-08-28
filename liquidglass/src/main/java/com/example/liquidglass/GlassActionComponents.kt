package com.example.liquidglass

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val FAB_SHAPE: Shape = CircleShape
private val EXTENDED_FAB_SHAPE: Shape = RoundedCornerShape(16.dp)
private val CHIP_SHAPE: Shape = RoundedCornerShape(8.dp)
private val BADGE_SHAPE: Shape = RoundedCornerShape(8.dp)
private val SNACKBAR_SHAPE: Shape = RoundedCornerShape(8.dp)

private val FAB_SIZE = 56.dp
private val CHIP_ICON_SIZE = 18.dp
private val CHIP_HORIZONTAL_PADDING = 12.dp
private val CHIP_VERTICAL_PADDING = 6.dp

/**
 * 玻璃浮動操作按鈕（FAB）。
 *
 * @param onClick 點擊回呼
 * @param modifier 外部修飾符
 * @param config 玻璃主題參數
 * @param shape 按鈕形狀，默認圓形
 * @param content 按鈕內圖示內容
 */
@Composable
fun GlassFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    shape: Shape = FAB_SHAPE,
    content: @Composable () -> Unit,
) {
    if (config.native) {
        FloatingActionButton(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            content = content
        )
        return
    }

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        color = Color.Transparent,
        contentColor = config.contentColor,
    ) {
        Box(
            modifier = Modifier
                .glassSurface(shape = shape, config = config)
                .defaultMinSize(minWidth = FAB_SIZE, minHeight = FAB_SIZE),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

/**
 * 玻璃擴展浮動操作按鈕（Extended FAB）。
 *
 * @param onClick 點擊回呼
 * @param modifier 外部修飾符
 * @param config 玻璃主題參數
 * @param text 按鈕文字
 * @param icon 前置圖示（可為 null）
 */
@Composable
fun GlassExtendedFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    text: String,
    icon: ImageVector? = null,
) {
    if (config.native) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            modifier = modifier,
            text = { Text(text = text) },
            icon = {
                if (icon != null) {
                    Icon(imageVector = icon, contentDescription = null)
                }
            }
        )
        return
    }

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = EXTENDED_FAB_SHAPE,
        color = Color.Transparent,
        contentColor = config.contentColor,
    ) {
        Box(
            modifier = Modifier
                .glassSurface(shape = EXTENDED_FAB_SHAPE, config = config)
                .defaultMinSize(minHeight = 48.dp)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * 玻璃徽標（Badge），可顯示小圓點或帶數字/文字內容。
 *
 * @param modifier 外部修飾符
 * @param config 玻璃主題參數
 * @param content 徽標內容（可為 null，為 null 時顯示小圓點）
 */
@Composable
fun GlassBadge(
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    content: @Composable (RowScope.() -> Unit)? = null,
) {
    if (config.native) {
        Badge(
            modifier = modifier,
            content = content
        )
        return
    }

    Box(
        modifier = modifier
            .glassSurface(shape = BADGE_SHAPE, config = config)
            .defaultMinSize(minWidth = 6.dp, minHeight = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        if (content != null) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

/**
 * 玻璃輔助晶片（Assist Chip）。
 *
 * @param onClick 點擊回呼
 * @param label 晶片文字
 * @param modifier 外部修飾符
 * @param config 玻璃主題參數
 * @param leadingIcon 前置圖示（可為 null）
 */
@Composable
fun GlassAssistChip(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    leadingIcon: ImageVector? = null,
) {
    if (config.native) {
        AssistChip(
            onClick = onClick,
            label = { Text(text = label) },
            modifier = modifier,
            leadingIcon = if (leadingIcon != null) {
                { Icon(imageVector = leadingIcon, contentDescription = null) }
            } else null
        )
        return
    }

    GlassChipSurface(
        onClick = onClick,
        modifier = modifier,
        config = config,
        leadingIcon = leadingIcon
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 玻璃建議晶片（Suggestion Chip）。
 *
 * @param onClick 點擊回呼
 * @param label 晶片文字
 * @param modifier 外部修飾符
 * @param config 玻璃主題參數
 * @param icon 前置圖示（可為 null）
 */
@Composable
fun GlassSuggestionChip(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    icon: ImageVector? = null,
) {
    if (config.native) {
        SuggestionChip(
            onClick = onClick,
            label = { Text(text = label) },
            modifier = modifier,
            icon = if (icon != null) {
                { Icon(imageVector = icon, contentDescription = null) }
            } else null
        )
        return
    }

    GlassChipSurface(
        onClick = onClick,
        modifier = modifier,
        config = config,
        leadingIcon = icon
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 玻璃篩選晶片（Filter Chip）。
 *
 * @param selected 是否選中
 * @param onClick 點擊回呼
 * @param label 晶片文字
 * @param modifier 外部修飾符
 * @param config 玻璃主題參數
 * @param leadingIcon 前置圖示（可為 null）
 */
@Composable
fun GlassFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    leadingIcon: ImageVector? = null,
) {
    if (config.native) {
        FilterChip(
            selected = selected,
            onClick = onClick,
            label = { Text(text = label) },
            modifier = modifier,
            leadingIcon = if (leadingIcon != null) {
                { Icon(imageVector = leadingIcon, contentDescription = null) }
            } else null
        )
        return
    }

    val effectiveConfig = if (selected) {
        config.copy(
            highlightInnerAlpha = (config.highlightInnerAlpha * 1.4f).coerceAtMost(0.5f)
        )
    } else {
        config
    }

    GlassChipSurface(
        onClick = onClick,
        modifier = modifier,
        config = effectiveConfig,
        leadingIcon = leadingIcon
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 玻璃輸入晶片（Input Chip）。
 *
 * @param label 晶片文字
 * @param onClick 點擊回呼
 * @param modifier 外部修飾符
 * @param config 玻璃主題參數
 * @param leadingIcon 前置圖示（可為 null）
 * @param trailingIcon 後置圖示（可為 null）
 * @param enabled 是否可用
 */
@Composable
fun GlassInputChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
) {
    if (config.native) {
        InputChip(
            selected = false,
            onClick = onClick,
            label = { Text(text = label) },
            modifier = modifier,
            enabled = enabled,
            leadingIcon = if (leadingIcon != null) {
                { Icon(imageVector = leadingIcon, contentDescription = null) }
            } else null,
            trailingIcon = if (trailingIcon != null) {
                { Icon(imageVector = trailingIcon, contentDescription = null) }
            } else null
        )
        return
    }

    val effectiveConfig = if (enabled) config else config.copy(
        bodyTopAlpha = config.bodyTopAlpha * 0.5f,
        bodyBottomAlpha = config.bodyBottomAlpha * 0.5f,
        borderTopAlpha = config.borderTopAlpha * 0.5f,
        contentColor = config.contentColor.copy(alpha = 0.5f)
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = CHIP_SHAPE,
        color = Color.Transparent,
        contentColor = effectiveConfig.contentColor,
    ) {
        Box(
            modifier = Modifier
                .glassSurface(shape = CHIP_SHAPE, config = effectiveConfig)
                .defaultMinSize(minHeight = 32.dp)
                .padding(horizontal = CHIP_HORIZONTAL_PADDING, vertical = CHIP_VERTICAL_PADDING),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                leadingIcon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        modifier = Modifier.size(CHIP_ICON_SIZE)
                    )
                }
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                trailingIcon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        modifier = Modifier.size(CHIP_ICON_SIZE)
                    )
                }
            }
        }
    }
}

/**
 * 玻璃底部提示條（Snackbar）。
 *
 * @param modifier 外部修飾符
 * @param config 玻璃主題參數
 * @param action 操作按鈕內容（可為 null）
 * @param dismissAction 關閉按鈕內容（可為 null）
 * @param content 提示文字內容
 */
@Composable
fun GlassSnackbar(
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    action: @Composable (() -> Unit)? = null,
    dismissAction: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (config.native) {
        Snackbar(
            modifier = modifier,
            action = action,
            dismissAction = dismissAction,
            content = content
        )
        return
    }

    Surface(
        modifier = modifier,
        shape = SNACKBAR_SHAPE,
        color = Color.Transparent,
        contentColor = config.contentColor,
    ) {
        Box(
            modifier = Modifier
                .glassOverlayBackdrop(shape = SNACKBAR_SHAPE, config = config)
                .glassSurface(shape = SNACKBAR_SHAPE, config = config)
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    content()
                }
                if (action != null) {
                    Spacer(modifier = Modifier.width(12.dp))
                    action()
                }
                if (dismissAction != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    dismissAction()
                }
            }
        }
    }
}

@Composable
private fun GlassChipSurface(
    onClick: () -> Unit,
    modifier: Modifier,
    config: GlassConfig,
    leadingIcon: ImageVector?,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = CHIP_SHAPE,
        color = Color.Transparent,
        contentColor = config.contentColor,
    ) {
        Box(
            modifier = Modifier
                .glassSurface(shape = CHIP_SHAPE, config = config)
                .defaultMinSize(minHeight = 32.dp)
                .padding(horizontal = CHIP_HORIZONTAL_PADDING, vertical = CHIP_VERTICAL_PADDING),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                leadingIcon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        modifier = Modifier.size(CHIP_ICON_SIZE)
                    )
                }
                content()
            }
        }
    }
}

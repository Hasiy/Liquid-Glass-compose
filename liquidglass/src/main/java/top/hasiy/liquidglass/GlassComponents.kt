package top.hasiyliquidglass

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import top.hasiyliquidglass.R

private val BUTTON_SHAPE = RoundedCornerShape(22.dp)
private val CARD_SHAPE = RoundedCornerShape(26.dp)

/**
 * 玻璃按鈕：套用 [Modifier.glassSurface] 的玻璃質感，含標準 ripple 與點擊語意。
 *
 * @param text 按鈕文字
 * @param onClick 點擊回呼
 * @param modifier 外部修飾符
 * @param config 玻璃主題參數
 * @param shape 按鈕形狀
 * @param minHeight 最小高度
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    shape: Shape = BUTTON_SHAPE,
    minHeight: Dp = 48.dp,
) {
    // 原生模式：Material3 原生按鈕
    if (config.native) {
        Button(onClick = onClick, modifier = modifier) {
            Text(text = text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
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
                .glassSurface(shape = shape, config = config.asControlSurface())
                .defaultMinSize(minHeight = minHeight)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * 玻璃卡片：容器型玻璃元件，可包任意內容。
 *
 * @param modifier 外部修飾符
 * @param config 玻璃主題參數
 * @param shape 卡片形狀
 * @param content 內容
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    shape: Shape = CARD_SHAPE,
    content: @Composable () -> Unit,
) {
    // 原生模式：Material3 原生卡片
    if (config.native) {
        Card(modifier = modifier) {
            Box(modifier = Modifier.padding(20.dp)) { content() }
        }
        return
    }
    Surface(
        modifier = modifier,
        shape = shape,
        color = Color.Transparent,
        contentColor = config.contentColor,
    ) {
        Box(
            modifier = Modifier
                .glassSurface(shape = shape, config = config)
                .padding(20.dp)
        ) {
            content()
        }
    }
}

/**
 * 玻璃圖示按鈕：小型圓形玻璃按鈕。
 *
 * @param icon 圖示
 * @param contentDescription 無障礙描述
 * @param onClick 點擊回呼
 * @param modifier 外部修飾符
 * @param config 玻璃主題參數
 * @param size 按鈕直徑
 */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    size: Dp = 48.dp,
) {
    val circleShape = RoundedCornerShape(size / 2)
    // 原生模式：Material3 原生圖示按鈕
    if (config.native) {
        IconButton(onClick = onClick, modifier = modifier) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = LocalContentColor.current
            )
        }
        return
    }
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = circleShape,
        color = Color.Transparent,
        contentColor = config.contentColor,
    ) {
        Box(
            modifier = Modifier
                .glassSurface(shape = circleShape, config = config.asControlSurface())
                .size(size),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}

/**
 * 玻璃主題切換列：在 Drop / Neutral / Dark 之間選擇，供設定用途。
 *
 * @param selected 目前選中的主題
 * @param onSelect 選擇回呼
 * @param modifier 外部修飾符
 */
@Composable
fun GlassThemeSelector(
    selected: GlassConfig,
    onSelect: (GlassConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 用 themed() 而非 All：配色以 colors.xml 為準，使用端覆蓋同名資源即可換色
        GlassPresets.themed().forEachIndexed { index, preset ->
            val isSelected = preset == selected
            // 主題預覽鈕的比例約為 2:1，公共層只會部分淡出方向性光效，深色主題仍會
            // 留下一條可見的橫向色帶。此處需要忠實預覽「底色」而不是光照方向，因此
            // 使用均勻透明度並關閉高光與陰影；選中態的描邊仍由下方配置保留。
            val previewConfig = if (isSelected) {
                preset.copy(
                    borderColor = if (preset.accentEnabled) {
                        preset.accentColor
                    } else {
                        preset.contentColor
                    },
                    borderWidth = 2.dp,
                    borderTopAlpha = 1f,
                    borderBottomAlpha = 0.75f,
                )
            } else {
                preset
            }
            val previewBodyAlpha =
                (previewConfig.bodyTopAlpha + previewConfig.bodyBottomAlpha) / 2f
            GlassButton(
                text = stringResource(PRESET_NAME_RES[index]),
                onClick = { onSelect(preset) },
                modifier = Modifier.weight(1f),
                config = previewConfig.copy(
                    bodyTopAlpha = previewBodyAlpha,
                    bodyBottomAlpha = previewBodyAlpha,
                    highlightInnerAlpha = 0f,
                    highlightOuterAlpha = 0f,
                    shadowElevation = 0.dp,
                ),
                minHeight = 40.dp
            )
        }
    }
}

/**
 * 玻璃選色盤：一排可點選的圓形色塊，選中的一顆套上同色外環。
 *
 * @param colors 候選顏色
 * @param selected 目前選中的顏色
 * @param onSelect 選擇回呼
 * @param modifier 外部修飾符
 * @param config 玻璃主題參數
 * @param swatchSize 色塊直徑
 */
@Composable
fun GlassColorPicker(
    colors: List<Color>,
    selected: Color,
    onSelect: (Color) -> Unit,
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    swatchSize: Dp = 32.dp,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        colors.forEach { color ->
            val isSelected = color == selected
            Box(
                modifier = Modifier
                    .size(swatchSize + if (isSelected) COLOR_SWATCH_RING_WIDTH * 2 else 0.dp)
                    .then(
                        // 選中的色塊套一圈同色外環，靠尺寸差異也能一眼看出
                        if (isSelected) {
                            Modifier.border(
                                width = COLOR_SWATCH_RING_WIDTH,
                                color = color.copy(alpha = 0.45f),
                                shape = CircleShape
                            )
                        } else {
                            Modifier
                        }
                    )
                    .padding(if (isSelected) COLOR_SWATCH_RING_WIDTH + 2.dp else 0.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(swatchSize)
                        .glassSurface(
                            shape = CircleShape,
                            config = config.copy(
                                baseColor = color,
                                bodyTopAlpha = 1f,
                                bodyBottomAlpha = 1f,
                                highlightInnerAlpha = 0f,
                                highlightOuterAlpha = 0f,
                                borderTopAlpha = 0f,
                                borderBottomAlpha = 0f,
                                innerShadowAlpha = 0f,
                                shadowElevation = 0.dp,
                            )
                        )
                        .clickable { onSelect(color) }
                )
            }
        }
    }
}

/** 選中色塊的外環寬度 */
private val COLOR_SWATCH_RING_WIDTH = 2.dp

/**
 * 主題顯示名稱，順序與 [GlassPresets.themed] 一致。
 *
 * 用索引而不是拿 [GlassConfig] 去比對：配色由 colors.xml 決定，使用端覆蓋後
 * themed() 的值就不再等於靜態的 [GlassPresets.Drop] 等欄位，比對會失準。
 */
private val PRESET_NAME_RES = listOf(
    R.string.glass_theme_drop,
    R.string.glass_theme_neutral,
    R.string.glass_theme_dark,
    R.string.glass_theme_native,
)

/**
 * 玻璃頂欄（TopBar）：玻璃質感的頂部工具列。
 *
 * @param title 標題文字
 * @param modifier 外部修飾符
 * @param config 玻璃主題參數
 * @param navigationIcon 左側導航圖示（可為 null）
 * @param actions 右側操作區
 */
@Composable
fun GlassTopBar(
    title: String,
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    // 原生模式：透明底的原生頂欄（無玻璃背景）
    if (config.native) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (navigationIcon != null) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    navigationIcon()
                }
            }
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            actions()
        }
        return
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .glassSurface(shape = RoundedCornerShape(28.dp), config = config)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (navigationIcon != null) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                navigationIcon()
            }
        }
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            color = config.contentColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        actions()
    }
}

/**
 * 玻璃開關（Switch）：玻璃質感的切換開關。
 *
 * @param checked 是否開啟
 * @param onCheckedChange 切換回呼
 * @param modifier 外部修飾符
 * @param config 玻璃主題參數
 */
@Composable
fun GlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
) {
    // 原生模式：Material3 原生開關
    if (config.native) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier
        )
        return
    }
    val trackWidth = 54.dp
    val trackHeight = 34.dp
    val thumbSize = trackHeight - 8.dp
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) trackWidth - trackHeight else 0.dp,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.6f),
        label = "glassSwitchThumb"
    )
    // 開啟時的軌道見 asSelectedSurface；未開啟時用 asControlSurface，
    // 否則淺色主題的近白軌道會融進同樣是淺色的卡片或頁面
    val trackConfig = if (checked) {
        config.asSelectedSurface(strong = true)
    } else {
        config.asControlSurface()
    }
    Box(
        modifier = modifier
            .size(trackWidth, trackHeight)
            .glassSurface(
                shape = RoundedCornerShape(trackHeight / 2),
                config = trackConfig
            )
            .clickable { onCheckedChange(!checked) }
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // thumb：始終是原本的玻璃圓鈕，不跟著軌道變色，才有「綠軌 + 亮鈕」的對比
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .glassSurface(
                    shape = CircleShape,
                    config = config.asControlSurface().copy(highlightRadiusFactor = 1.2f)
                )
        )
    }
}

/**
 * 玻璃列表項（ListItem）：玻璃質感的單行列表項目。
 *
 * @param title 主標題
 * @param modifier 外部修飾符
 * @param config 玻璃主題參數
 * @param subtitle 副標題（可為 null）
 * @param leadingIcon 前導圖示（可為 null，以玻璃小圓呈現）
 * @param trailing 尾端文字（可為 null）
 * @param onClick 點擊回呼（null 表示不可點擊）
 */
@Composable
fun GlassListItem(
    title: String,
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    trailing: String? = null,
    onClick: (() -> Unit)? = null,
) {
    // 原生模式：Material3 原生列表項（無玻璃背景，用主題顏色）
    if (config.native) {
        val nativeRow: @Composable () -> Unit = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = onClick != null) { onClick?.invoke() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (trailing != null) {
                    Text(
                        text = trailing,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Surface(
            modifier = modifier,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            nativeRow()
        }
        return
    }
    val shape = RoundedCornerShape(16.dp)
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .glassSurface(
                            shape = CircleShape,
                            config = config.copy(shadowElevation = 0.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = config.contentColor
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = config.contentColor.copy(alpha = 0.6f)
                    )
                }
            }
            if (trailing != null) {
                Text(
                    text = trailing,
                    fontSize = 13.sp,
                    color = config.contentColor.copy(alpha = 0.7f)
                )
            }
        }
    }

    // List items are much wider than they are tall. Reusing the directional highlight here
    // stretches it into a horizontal band, while the item shadow darkens the small gap between
    // adjacent rows. Keep the translucent glass body and border, but render the row surface
    // uniformly so stacked items do not produce repeating light/dark strips.
    val controlConfig = config.asControlSurface()
    val listItemConfig = controlConfig.copy(
        bodyTopAlpha = (controlConfig.bodyTopAlpha + controlConfig.bodyBottomAlpha) / 2f,
        bodyBottomAlpha = (controlConfig.bodyTopAlpha + controlConfig.bodyBottomAlpha) / 2f,
        highlightInnerAlpha = 0f,
        highlightOuterAlpha = 0f,
        shadowElevation = 0.dp,
    )
    val glassModifier = Modifier.glassSurface(shape = shape, config = listItemConfig)
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = Color.Transparent,
            contentColor = config.contentColor,
        ) {
            Box(modifier = glassModifier) { content() }
        }
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = Color.Transparent,
            contentColor = config.contentColor,
        ) {
            Box(modifier = glassModifier) { content() }
        }
    }
}

/**
 * 玻璃輸入框（TextField）：玻璃質感的單行文字輸入框。
 *
 * @param value 目前文字
 * @param onValueChange 文字變更回呼
 * @param modifier 外部修飾符
 * @param config 玻璃主題參數
 * @param placeholder 佔位提示（無輸入時顯示）
 */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    placeholder: String = "",
) {
    // 原生模式：Material3 原生輸入框
    if (config.native) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(placeholder) }
        )
        return
    }
    val contentColor = config.contentColor
    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(shape = RoundedCornerShape(18.dp), config = config.asControlSurface())
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = LocalTextStyle.current.copy(
                color = contentColor,
                fontSize = 15.sp
            ),
            singleLine = true,
            cursorBrush = SolidColor(contentColor),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = contentColor.copy(alpha = 0.4f),
                        fontSize = 15.sp
                    )
                }
                innerTextField()
            }
        )
    }
}

/**
 * 玻璃進度條（ProgressBar）：玻璃質感的進度指示。
 *
 * - [indeterminate] 為 false：顯示 [progress]（0.0~1.0）的完成進度
 * - [indeterminate] 為 true：亮段左右循環移動，表示載入中
 *
 * @param progress 進度（0.0~1.0），僅 [indeterminate] 為 false 時使用
 * @param modifier 外部修飾符
 * @param config 玻璃主題參數
 * @param height 進度條高度
 * @param indeterminate 是否為不確定（載入中）模式
 */
@Composable
fun GlassProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    height: Dp = 12.dp,
    indeterminate: Boolean = false,
) {
    // 原生模式：Material3 原生進度條
    if (config.native) {
        if (indeterminate) {
            LinearProgressIndicator(modifier = modifier.fillMaxWidth())
        } else {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = modifier.fillMaxWidth()
            )
        }
        return
    }
    val trackShape = RoundedCornerShape(height / 2)
    // 完成段：見 GlassConfig.asFillSurface——啟用強調色時是實色，否則是玻璃質感
    val fillConfig = config.asFillSurface()

    if (indeterminate) {
        val transition = rememberInfiniteTransition(label = "glassProgress")
        val slide by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            // 來回滑動（Reverse）且速度放慢：亮段滑到右端後往回滑，不會瞬間跳回起點
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = androidx.compose.animation.core.LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glassProgressSlide"
        )
        BoxWithConstraints(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .glassSurface(
                    shape = trackShape,
                    config = config.asControlSurface().copy(shadowElevation = 0.dp)
                )
        ) {
            val segmentWidth = maxWidth * 0.35f
            val slideX = (maxWidth - segmentWidth) * slide
            Box(
                modifier = Modifier
                    .offset(x = slideX)
                    .width(segmentWidth)
                    .fillMaxHeight()
                    .padding(2.dp)
                    .glassSurface(shape = trackShape, config = fillConfig)
            )
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .glassSurface(
                    shape = trackShape,
                    config = config.asControlSurface().copy(shadowElevation = 0.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                    .padding(2.dp)
                    .glassSurface(shape = trackShape, config = fillConfig)
            )
        }
    }
}

/**
 * 玻璃對話框（Dialog）：毛玻璃質感的 AlertDialog 樣例。
 *
 * @param onDismissRequest 點擊對話框外部或返回鍵時觸發
 * @param title 對話框標題
 * @param message 對話框內文
 * @param onConfirm 確認按鈕回呼
 * @param onDismiss 取消按鈕回呼（null 時不顯示取消按鈕）
 * @param confirmText 確認按鈕文字
 * @param dismissText 取消按鈕文字
 * @param modifier 外部修飾符
 * @param config 玻璃主題參數
 */
@Composable
fun GlassDialog(
    onDismissRequest: () -> Unit,
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: (() -> Unit)? = null,
    confirmText: String = stringResource(R.string.glass_dialog_confirm),
    dismissText: String = stringResource(R.string.glass_dialog_dismiss),
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
) {
    // 原生模式：Material3 AlertDialog
    if (config.native) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = {
                TextButton(onClick = onConfirm) { Text(confirmText) }
            },
            dismissButton = if (onDismiss != null) {
                { TextButton(onClick = onDismiss) { Text(dismissText) } }
            } else null,
            title = { Text(title) },
            text = { Text(message) }
        )
        return
    }
    // 使用 Popup 而非 Dialog，避免系統預設的灰色 scrim 把背景變暗/模糊，
    // 讓後方頁面內容保持清晰，只有玻璃卡片浮於其上。
    Popup(
        onDismissRequest = onDismissRequest,
        alignment = Alignment.Center,
        properties = PopupProperties(focusable = true)
    ) {
        val dialogShape = RoundedCornerShape(24.dp)
        GlassCard(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .glassOverlayBackdrop(shape = dialogShape, config = config),
            config = config,
            shape = dialogShape
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = config.contentColor.copy(alpha = 0.85f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onDismiss != null) {
                        TextButton(
                            onClick = {
                                onDismiss()
                                onDismissRequest()
                            }
                        ) {
                            Text(
                                text = dismissText,
                                color = config.contentColor.copy(alpha = 0.8f)
                            )
                        }
                    }
                    TextButton(
                        onClick = {
                            onConfirm()
                            onDismissRequest()
                        }
                    ) {
                        Text(
                            text = confirmText,
                            color = config.contentColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * 玻璃彈窗（Popup）：輕量級玻璃質感提示框，可透過 [alignment] 與 [offset] 相對於父佈局定位。
 *
 * @param onDismissRequest 點擊外部或返回鍵時觸發
 * @param alignment 彈窗在父佈局中的對齊方式
 * @param offset 相對於對齊點的像素偏移
 * @param content 彈窗內容
 * @param modifier 外部修飾符
 * @param config 玻璃主題參數
 */
@Composable
fun GlassPopup(
    onDismissRequest: () -> Unit,
    alignment: Alignment = Alignment.TopCenter,
    offset: IntOffset = IntOffset(0, 0),
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    content: @Composable () -> Unit,
) {
    Popup(
        onDismissRequest = onDismissRequest,
        alignment = alignment,
        offset = offset,
        properties = PopupProperties(focusable = true)
    ) {
        val popupShape = RoundedCornerShape(20.dp)
        GlassCard(
            modifier = modifier
                .padding(16.dp)
                .glassOverlayBackdrop(shape = popupShape, config = config),
            config = config,
            shape = popupShape
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

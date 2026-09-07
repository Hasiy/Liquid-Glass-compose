package top.hasiy.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/** 固定槽位中的導航內容；key 在同一個 Tab Bar 內必須唯一。 */
@Immutable
data class GlassLensTabItem(val key: String, val label: String, val icon: ImageVector)

/** 新版鏡片參數，與原始 DynamicLightTabBarLegacyConfig 完全獨立。 */
object GlassLensTabBarDefaults {
    val Height = 62.dp
    const val MAGNIFICATION = 1.22f
    /** 鏡片相對於命中 Tab 槽位四個方向各延伸的固定距離。 */
    val LensOverflow = 8.dp
    val LensRimWidth = 0.75.dp
    const val LENS_RIM_ALPHA = 0.58f
    val ContainerBorderColor = Color(0xFFC9CBCE)
    val ContentColor = Color(0xFFF6F7F8)
}

/** 返回鏡片中心所在的視覺槽位，支援 RTL。 */
internal fun lensSlotAtPosition(
    positionPx: Float,
    barWidthPx: Float,
    slotCount: Int,
    isRtl: Boolean,
): Int? {
    if (positionPx.isNaN() || barWidthPx <= 0f || slotCount <= 0) return null
    val physicalFraction = positionPx.coerceIn(0f, barWidthPx) / barWidthPx
    val logicalFraction = if (isRtl) 1f - physicalFraction else physicalFraction
    return (logicalFraction * slotCount).toInt().coerceIn(0, slotCount - 1)
}

private fun slotCenterFraction(slot: Int, slotCount: Int, isRtl: Boolean): Float {
    val logicalFraction = (slot + 0.5f) / slotCount
    return if (isRtl) 1f - logicalFraction else logicalFraction
}

/**
 * 透明放大鏡導航列。所有 Item 只佈局一次，鏡片內外使用同一份繪圖來源。
 *
 * 拖動只改變採樣中心；放開或取消時選中鏡片下最近的 Item，並貼合其原始槽位。
 * 鏡片只放大此元件的內容；軌道本身不填色，背景完全由呼叫端承載，彩色鏡緣是元件本身的
 * 材質，不宣稱對任意頁面背景做光學折射。
 *
 * @param items 固定順序的導航項
 * @param selectedIndex 選中索引（不包含中央操作）
 * @param onSelect 點擊導航項的回呼
 * @param centerAction 可選中央操作，由呼叫端依裝置寬度／方向決定是否提供
 * @param centerActionSelected 中央操作是否為目前選中槽位
 * @param onCenterActionClick 中央操作點擊回呼
 * @param centerActionDescription 中央操作的無障礙名稱
 * @param lensEnabled 是否開啟鏡片內容放大；關閉時只顯示原始內容與透明軌道輪廓
 * @param contentColor 圖示與文字顏色，不改變鏡片亮面材質
 */
@Composable
fun GlassLensTabBar(
    items: List<GlassLensTabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    centerAction: (@Composable () -> Unit)? = null,
    centerActionSelected: Boolean = false,
    onCenterActionClick: () -> Unit = {},
    centerActionDescription: String? = null,
    lensEnabled: Boolean = true,
    contentColor: Color = GlassLensTabBarDefaults.ContentColor,
) {
    if (items.isEmpty()) return
    val hasCenter = centerAction != null
    val slotCount = items.size + if (hasCenter) 1 else 0
    val centerSlot = items.size / 2
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val selected = selectedIndex.coerceIn(items.indices)
    val selectedSlot = if (hasCenter && centerActionSelected) {
        centerSlot
    } else {
        selected + if (hasCenter && selected >= centerSlot) 1 else 0
    }
    val physicalFraction = slotCenterFraction(selectedSlot, slotCount, rtl)
    var widthPx by remember { mutableFloatStateOf(0f) }
    // 尺寸、選中項或模式改變時丟棄舊手勢，避免旋轉後保留過時像素位置。
    var dragCenterPx by remember(widthPx, slotCount, lensEnabled, rtl) {
        mutableFloatStateOf(Float.NaN)
    }
    val restingCenter = remember(slotCount, rtl) { Animatable(physicalFraction) }
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnCenterActionClick by rememberUpdatedState(onCenterActionClick)
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(physicalFraction) {
        restingCenter.animateTo(
            targetValue = physicalFraction,
            animationSpec = spring(dampingRatio = 1f, stiffness = 500f),
        )
    }
    val finishDrag: () -> Unit = {
        val releaseCenterPx = dragCenterPx
        val targetSlot = lensSlotAtPosition(
            positionPx = releaseCenterPx,
            barWidthPx = widthPx,
            slotCount = slotCount,
            isRtl = rtl,
        )
        if (targetSlot == null) {
            dragCenterPx = Float.NaN
        } else {
            coroutineScope.launch {
                restingCenter.snapTo(releaseCenterPx.coerceIn(0f, widthPx) / widthPx)
                dragCenterPx = Float.NaN
                if (hasCenter && targetSlot == centerSlot) {
                    currentOnCenterActionClick()
                } else {
                    val targetIndex = targetSlot - if (hasCenter && targetSlot > centerSlot) 1 else 0
                    currentOnSelect(targetIndex)
                }
                restingCenter.animateTo(
                    targetValue = slotCenterFraction(targetSlot, slotCount, rtl),
                    animationSpec = spring(dampingRatio = 1f, stiffness = 500f),
                )
            }
        }
    }
    val source = rememberGraphicsLayer()
    val lensPath = remember { Path() }
    val barPath = remember { Path() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(GlassLensTabBarDefaults.Height)
            .onSizeChanged { widthPx = it.width.toFloat() }
            .selectableGroup()
            .pointerInput(widthPx, slotCount, lensEnabled, rtl) {
                try {
                    detectDragGestures(
                        onDragStart = { dragCenterPx = it.x },
                        onDrag = { change, _ ->
                            change.consume()
                            dragCenterPx = change.position.x
                        },
                        onDragEnd = finishDrag,
                        onDragCancel = finishDrag,
                    )
                } finally {
                    dragCenterPx = Float.NaN
                }
            }
            .drawWithContent {
                // 只記錄固定 Row。記錄中不含鏡片材質，也不受外部裁剪/縮放影響。
                source.record { this@drawWithContent.drawContent() }
                val slotWidth = size.width / slotCount
                val centerX = if (dragCenterPx.isNaN()) restingCenter.value * size.width else {
                    dragCenterPx.coerceIn(slotWidth / 2f, size.width - slotWidth / 2f)
                }
                // 放大鏡以自身中心採樣：只有 lensPath 內的內容會隨中心放大，外部維持原圖。
                val lensPivot = Offset(centerX, size.height / 2f)
                val lensOverflow = GlassLensTabBarDefaults.LensOverflow.toPx()
                val lensWidth = slotWidth + lensOverflow * 2f
                val lensTop = -lensOverflow
                val lensBottom = size.height + lensOverflow
                val lensHeight = lensBottom - lensTop
                val barInset = 0.75.dp.toPx()
                barPath.reset()
                barPath.addRoundRect(
                    RoundRect(
                        barInset,
                        barInset,
                        size.width - barInset,
                        size.height - barInset,
                        CornerRadius((size.height - barInset * 2f) / 2f),
                    )
                )
                lensPath.reset()
                lensPath.addRoundRect(
                    RoundRect(
                        centerX - lensWidth / 2f, lensTop,
                        centerX + lensWidth / 2f, lensBottom,
                        CornerRadius(lensHeight / 2f),
                    )
                )
                // 只替換鏡片所覆蓋的像素，不隱藏整個 Item。跨兩項時兩項自然部分進鏡。
                if (lensEnabled) {
                    clipPath(lensPath, clipOp = ClipOp.Difference) { drawLayer(source) }
                } else {
                    drawLayer(source)
                }
                if (lensEnabled) {
                    clipPath(lensPath) {
                        // 只在鏡片範圍重繪放大的同一份內容，形成連續的放大鏡取樣。
                        scale(GlassLensTabBarDefaults.MAGNIFICATION, pivot = lensPivot) {
                            drawLayer(source)
                        }
                    }
                    // 鏡片本體不加濾色：內容保持原始顏色，視覺差異只來自局部放大。
                    // 這一道極淡白色反光只用來定義玻璃上緣，不改變內容色調。
                    drawPath(
                        lensPath,
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
                            startY = lensTop,
                            endY = lensTop + lensHeight * 0.46f,
                        ),
                    )
                    drawPath(
                        lensPath,
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF77E5FF),
                                Color(0xFF2E9FFF),
                                Color(0xFFDD8EFF),
                                Color(0xFFF0DA57),
                                Color(0xFF6BE7D2),
                            ),
                            start = Offset(centerX - lensWidth / 2f, 0f),
                            end = Offset(centerX + lensWidth / 2f, size.height),
                        ),
                        style = Stroke(GlassLensTabBarDefaults.LensRimWidth.toPx()),
                        alpha = GlassLensTabBarDefaults.LENS_RIM_ALPHA,
                    )
                }
                // 軌道邊界使用中性白，不與鏡片彩邊混在一起。
                drawPath(
                    barPath,
                    Brush.verticalGradient(
                        listOf(
                            GlassLensTabBarDefaults.ContainerBorderColor.copy(alpha = 0.82f),
                            GlassLensTabBarDefaults.ContainerBorderColor.copy(alpha = 0.36f),
                        )
                    ),
                    style = Stroke(1.dp.toPx()),
                )
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (slot in 0 until slotCount) {
            if (hasCenter && slot == centerSlot) {
                key("lens-center-action") {
                    val interactionSource = remember { MutableInteractionSource() }
                    Box(
                        Modifier.weight(1f).fillMaxHeight()
                            .selectable(
                                selected = centerActionSelected,
                                interactionSource = interactionSource,
                                indication = null,
                                role = Role.Tab,
                                onClick = {
                                    dragCenterPx = Float.NaN
                                    currentOnCenterActionClick()
                                },
                            )
                            .semantics {
                                centerActionDescription?.let { contentDescription = it }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        val ink = contentColor.copy(alpha = if (centerActionSelected) 1f else 0.78f)
                        CompositionLocalProvider(LocalContentColor provides ink) {
                            centerAction?.invoke()
                        }
                    }
                }
            } else {
                val index = slot - if (hasCenter && slot > centerSlot) 1 else 0
                val item = items[index]
                key(item.key) {
                    val interactionSource = remember { MutableInteractionSource() }
                    Box(
                        Modifier.weight(1f).fillMaxHeight()
                            .selectable(
                                selected = index == selected,
                                interactionSource = interactionSource,
                                indication = null,
                                role = Role.Tab,
                                onClick = { dragCenterPx = Float.NaN; onSelect(index) },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            val ink = contentColor.copy(alpha = if (index == selected) 1f else 0.78f)
                            Icon(item.icon, contentDescription = null, Modifier.size(22.dp), tint = ink)
                            Text(item.label, color = ink, fontSize = 13.sp, maxLines = 1,
                                fontWeight = if (index == selected) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }
}

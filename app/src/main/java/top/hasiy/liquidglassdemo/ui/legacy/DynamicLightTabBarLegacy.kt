package top.hasiyliquidglassdemo.ui.legacy

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import top.hasiy.designsystem.GlassConfig
import top.hasiy.designsystem.glassBackdrop
import top.hasiy.designsystem.isLightSurface

/** 淺色主題下 Bar 底相對表面色的壓暗比例：只需要一點層次，壓多了就變回深色條 */
private const val LIGHT_BAR_DARKEN = 0.10f

internal fun tabIndexAtPosition(
    touchX: Float,
    barWidthPx: Float,
    itemCount: Int,
): Int? {
    if (barWidthPx <= 0f || itemCount <= 0 || touchX.isNaN()) return null
    return ((touchX.coerceIn(0f, barWidthPx) / barWidthPx) * itemCount)
        .toInt()
        .coerceIn(0, itemCount - 1)
}

internal fun indicatorTransformOriginFor(fromIndex: Int, toIndex: Int): Float = when {
    toIndex > fromIndex -> 0f
    toIndex < fromIndex -> 1f
    else -> 0.5f
}

internal fun indicatorTargetOffsetPx(
    isDragging: Boolean,
    touchX: Float,
    itemWidthPx: Float,
    barWidthPx: Float,
    selectedIndex: Int,
): Float = if (isDragging && !touchX.isNaN() && itemWidthPx > 0f) {
    (touchX - itemWidthPx / 2f).coerceIn(0f, barWidthPx - itemWidthPx)
} else {
    selectedIndex * itemWidthPx
}

/**
 * 動態光照底部 Tab Bar（Apple Liquid Glass 風格）。
 *
 * - 拖動時選擇指示器即時跟隨手指位置，鬆手後自動吸附到最近的 Item 並回呼
 * - AGSL RuntimeShader 產生動態光影（API 33+），低版本自動降級為漸層模擬
 * - Haze 毛玻璃背景（API 31+，低版本磨砂回退）、Haptic Feedback 與縮放反饋
 *
 * @param items Tab 項目文字列表
 * @param selectedIndex 當前選中索引
 * @param onSelect 選中回呼，拖動結束或點擊時觸發
 * @param showWaterHighlight 是否顯示水滴頂部高光光暈（含感應器跟隨），預設由 Config 控制
 * @param pillGlassConfig 選中 Pill 的玻璃主題（null 時使用 [DynamicLightTabBarConfig] 的預設水滴效果）
 * @param modifier 外部修飾符
 */
@Composable
fun DynamicLightTabBarLegacy(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    showWaterHighlight: Boolean = DynamicLightTabBarLegacyConfig.WATER_DROP_ENABLED,
    pillGlassConfig: GlassConfig? = null,
    modifier: Modifier = Modifier
) {
    // 主題派生色：這個 Bar 原本整套是為深色背景寫死的（白反光、白描邊、白字）。
    // 淺色主題（Neutral）下白色全部不可見，因此改由 pillGlassConfig 推導。
    val isLight = pillGlassConfig?.isLightSurface == true
    // 反光/高光用色與混合模式：深色底提亮，淺色底壓暗
    val glassInk = pillGlassConfig?.highlightColor ?: Color.White
    val glassBlend = pillGlassConfig?.highlightBlendMode ?: BlendMode.Screen
    val glassBorderInk = pillGlassConfig?.borderColor ?: Color.White
    val tabTextColor = pillGlassConfig?.contentColor ?: Color.White
    // 淺色主題的 Bar 底：從表面色壓深一階，取代寫死的深色 BACKGROUND_COLOR_HEX
    val barBackgroundColor = if (isLight && pillGlassConfig != null) {
        lerp(pillGlassConfig.baseColor, Color.Black, LIGHT_BAR_DARKEN)
    } else {
        Color(DynamicLightTabBarLegacyConfig.BACKGROUND_COLOR_HEX)
    }

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    var touchX by remember { mutableFloatStateOf(Float.NaN) }
    var touchY by remember { mutableFloatStateOf(Float.NaN) }
    var isTouching by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }

    var barWidthPx by remember { mutableFloatStateOf(0f) }
    var barHeightPx by remember { mutableFloatStateOf(0f) }
    var lastHapticIndex by remember { mutableIntStateOf(-1) }

    // 選中項切換：先朝目標方向拉伸，再到位壓縮，最後用 spring 恢復原形。
    val indicatorScaleX = remember { Animatable(1f) }
    val indicatorScaleY = remember { Animatable(1f) }
    var indicatorTransformOriginX by remember { mutableFloatStateOf(0.5f) }
    var previousSelectedIndex by remember { mutableIntStateOf(selectedIndex) }
    LaunchedEffect(selectedIndex) {
        val fromIndex = previousSelectedIndex
        if (fromIndex == selectedIndex) return@LaunchedEffect

        previousSelectedIndex = selectedIndex
        indicatorTransformOriginX = indicatorTransformOriginFor(fromIndex, selectedIndex)

        coroutineScope {
            launch {
                indicatorScaleX.snapTo(1f)
                indicatorScaleX.animateTo(
                    targetValue = DynamicLightTabBarLegacyConfig.INDICATOR_STRETCH_SCALE_X,
                    animationSpec = tween(DynamicLightTabBarLegacyConfig.INDICATOR_STRETCH_DURATION_MS)
                )
                indicatorScaleX.animateTo(
                    targetValue = DynamicLightTabBarLegacyConfig.INDICATOR_SETTLE_SCALE_X,
                    animationSpec = tween(DynamicLightTabBarLegacyConfig.INDICATOR_SETTLE_DURATION_MS)
                )
                indicatorScaleX.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        stiffness = DynamicLightTabBarLegacyConfig.SCALE_SPRING_STIFFNESS,
                        dampingRatio = DynamicLightTabBarLegacyConfig.SCALE_SPRING_DAMPING
                    )
                )
            }
            launch {
                indicatorScaleY.snapTo(1f)
                indicatorScaleY.animateTo(
                    targetValue = DynamicLightTabBarLegacyConfig.INDICATOR_STRETCH_SCALE_Y,
                    animationSpec = tween(DynamicLightTabBarLegacyConfig.INDICATOR_STRETCH_DURATION_MS)
                )
                indicatorScaleY.animateTo(
                    targetValue = DynamicLightTabBarLegacyConfig.INDICATOR_SETTLE_SCALE_Y,
                    animationSpec = tween(DynamicLightTabBarLegacyConfig.INDICATOR_SETTLE_DURATION_MS)
                )
                indicatorScaleY.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        stiffness = DynamicLightTabBarLegacyConfig.SCALE_SPRING_STIFFNESS,
                        dampingRatio = DynamicLightTabBarLegacyConfig.SCALE_SPRING_DAMPING
                    )
                )
            }
        }
    }

    // 注意：Compose 沒有「模糊背後內容」的 API。
    // 若將 blurEffect 套在自身 graphicsLayer，整個 Tab Bar（含文字）都會被糊掉、無法辨識，
    // 因此改用半透明深色背景模擬玻璃感；真實毛玻璃需底層截圖（見 md 注意事項 #4）。
    // 整個 Bar 的環境光（Liquid Glass）：光暈從手指位置向整個 Bar 擴散（md「進階」章節）。
    val barShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RuntimeShader(INDICATOR_SHADER_SRC)
        } else null
    }
    val barRenderEffect = if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        barShader != null &&
        isDragging &&
        !touchX.isNaN() &&
        barWidthPx > 0 &&
        barHeightPx > 0
    ) {
        barShader.setFloatUniform("resolution", barWidthPx, barHeightPx)
        // 用絕對座標 (touchX, touchY)，光斑真正跟隨手指任意方向移動（含垂直）
        barShader.setFloatUniform("touchPos", touchX, touchY)
        // 光暈半徑以「單一 item 寬度」為基準，讓它不超過 item 大小；
        // 依 item 數均分 bar 寬度（items 至少 1）
        val itemW = if (items.isNotEmpty()) barWidthPx / items.size else barWidthPx
        barShader.setFloatUniform("intensity", DynamicLightTabBarLegacyConfig.BAR_GLOW_INTENSITY)
        barShader.setFloatUniform(
            "radius",
            itemW * DynamicLightTabBarLegacyConfig.BAR_GLOW_RADIUS_FACTOR
        )
        RenderEffect.createRuntimeShaderEffect(barShader, "content")
    } else null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(DynamicLightTabBarLegacyConfig.BAR_HEIGHT_DP.dp)
            .onSizeChanged {
                barWidthPx = it.width.toFloat()
                barHeightPx = it.height.toFloat()
            }
            .graphicsLayer {
                if (barRenderEffect != null) {
                    renderEffect = barRenderEffect.asComposeRenderEffect()
                }
            }
            // 背景模糊：讓底下的頁面內容隱約透出來，而不是清晰穿透。
            // 需要頁面根節點包一層 GlassBackdropHost，且這個 Bar 要放在它的 overlay 裡。
            .then(
                if (pillGlassConfig != null) {
                    Modifier.glassBackdrop(
                        shape = RoundedCornerShape(
                            DynamicLightTabBarLegacyConfig.BAR_CORNER_RADIUS_DP.dp
                        ),
                        config = pillGlassConfig
                    )
                } else {
                    Modifier
                }
            )
            .background(
                color = barBackgroundColor
                    .copy(alpha = DynamicLightTabBarLegacyConfig.BACKGROUND_ALPHA),
                shape = RoundedCornerShape(DynamicLightTabBarLegacyConfig.BAR_CORNER_RADIUS_DP.dp)
            )
            .shadow(
                elevation = DynamicLightTabBarLegacyConfig.OUTER_SHADOW_ELEVATION_DP.dp,
                shape = RoundedCornerShape(DynamicLightTabBarLegacyConfig.BAR_CORNER_RADIUS_DP.dp),
                ambientColor = Color(DynamicLightTabBarLegacyConfig.OUTER_SHADOW_AMBIENT_HEX)
                    .copy(alpha = DynamicLightTabBarLegacyConfig.OUTER_SHADOW_AMBIENT_ALPHA),
                spotColor = Color(DynamicLightTabBarLegacyConfig.OUTER_SHADOW_SPOT_HEX)
                    .copy(alpha = DynamicLightTabBarLegacyConfig.OUTER_SHADOW_SPOT_ALPHA)
            )
            // 玻璃邊緣描邊：頂部最亮、往底部漸淡，模擬玻璃上緣反光的邊框
            .then(
                if (DynamicLightTabBarLegacyConfig.GLASS_BORDER_ENABLED) {
                    Modifier.border(
                        width = DynamicLightTabBarLegacyConfig.GLASS_BORDER_WIDTH_DP.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                glassBorderInk.copy(alpha = DynamicLightTabBarLegacyConfig.GLASS_BORDER_TOP_ALPHA),
                                glassBorderInk.copy(alpha = DynamicLightTabBarLegacyConfig.GLASS_BORDER_BOTTOM_ALPHA)
                            )
                        ),
                        shape = RoundedCornerShape(DynamicLightTabBarLegacyConfig.BAR_CORNER_RADIUS_DP.dp)
                    )
                } else Modifier
            )
            // 僅觀察按下狀態、不消費事件：靜態亮斑在觸摸期間隱藏，
            // 原有的點擊與拖動手勢仍由後續 Modifier 正常處理。
            .pointerInput(Unit) {
                try {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            isTouching = event.changes.any { it.pressed }
                        }
                    }
                } finally {
                    isTouching = false
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        touchX = offset.x.coerceIn(0f, barWidthPx)
                        touchY = offset.y.coerceIn(0f, barHeightPx)
                        isDragging = true
                        lastHapticIndex = -1
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        touchX = change.position.x.coerceIn(0f, barWidthPx)
                        touchY = change.position.y.coerceIn(0f, barHeightPx)

                        // 跨過 item 邊界時給 tick 反饋
                        tabIndexAtPosition(touchX, barWidthPx, items.size)?.let { index ->
                            if (index != lastHapticIndex) {
                                // 1.7.x 未提供 SegmentTick，依文件建議降級為 TextHandleMove
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                lastHapticIndex = index
                            }
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        tabIndexAtPosition(touchX, barWidthPx, items.size)?.let { index ->
                            onSelect(index)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        touchX = Float.NaN
                        touchY = Float.NaN
                        lastHapticIndex = -1
                    },
                    onDragCancel = {
                        isDragging = false
                        touchX = Float.NaN
                        touchY = Float.NaN
                        lastHapticIndex = -1
                    }
                )
            }
    ) {
        val itemWidthPx = if (items.isNotEmpty()) barWidthPx / items.size else 0f
        val itemWidthDp = with(density) { itemWidthPx.toDp() }

        // 玻璃反光層：頂部白色高光漸層，模擬磨砂玻璃的反光質感（覆於整個 Bar）
        if (DynamicLightTabBarLegacyConfig.GLASS_REFLECTION_ENABLED) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                glassInk.copy(alpha = DynamicLightTabBarLegacyConfig.GLASS_REFLECTION_TOP_ALPHA),
                                glassInk.copy(alpha = DynamicLightTabBarLegacyConfig.GLASS_REFLECTION_BOTTOM_ALPHA)
                            ),
                            startY = 0f
                        ),
                        shape = RoundedCornerShape(DynamicLightTabBarLegacyConfig.BAR_CORNER_RADIUS_DP.dp)
                    )
            )
        }

        // API < 33 的後備：整個 Bar 的光暈
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && !touchX.isNaN()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    glassInk.copy(alpha = 0.06f),
                                    glassInk.copy(alpha = 0.015f),
                                    Color.Transparent
                                ),
                                center = Offset(touchX, size.height / 2),
                                radius = size.width * 0.35f
                            ),
                            center = Offset(touchX, size.height / 2),
                            radius = size.width * 0.35f,
                            blendMode = glassBlend
                        )
                    }
            )
        }

        // 選中指示器位置
        val targetOffsetPx = indicatorTargetOffsetPx(
            isDragging = isDragging,
            touchX = touchX,
            itemWidthPx = itemWidthPx,
            barWidthPx = barWidthPx,
            selectedIndex = selectedIndex,
        )
        val animatedOffsetPx by animateFloatAsState(
            targetValue = targetOffsetPx,
            animationSpec = spring(
                stiffness = DynamicLightTabBarLegacyConfig.OFFSET_SPRING_STIFFNESS,
                dampingRatio = DynamicLightTabBarLegacyConfig.OFFSET_SPRING_DAMPING
            ),
            label = "indicatorOffset"
        )

        // 動態光照 Shader（API 33+）
        val indicatorShader = remember {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                RuntimeShader(INDICATOR_SHADER_SRC)
            } else null
        }
        val indicatorRenderEffect = if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            indicatorShader != null &&
            isDragging &&
            !touchX.isNaN() &&
            itemWidthPx > 0 &&
            barHeightPx > 0
        ) {
            indicatorShader.setFloatUniform("resolution", itemWidthPx, barHeightPx)
            // 手指相對於 Pill 左上角：pill 頂點 = animatedOffsetPx，故用絕對座標相減，
            // 且加大半徑/調低強度，讓它在 Pill 內呈柔和圓形擴散、不形成固定亮點
            indicatorShader.setFloatUniform(
                "touchPos",
                touchX - animatedOffsetPx,
                touchY
            )
            indicatorShader.setFloatUniform(
                "intensity",
                DynamicLightTabBarLegacyConfig.INDICATOR_GLOW_INTENSITY
            )
            indicatorShader.setFloatUniform(
                "radius",
                itemWidthPx * DynamicLightTabBarLegacyConfig.INDICATOR_GLOW_RADIUS_FACTOR
            )
            RenderEffect.createRuntimeShaderEffect(indicatorShader, "content")
        } else null

        // 選中 Pill
        val indicatorShape = RoundedCornerShape(
            DynamicLightTabBarLegacyConfig.INDICATOR_CORNER_RADIUS_DP.dp
        )
        // 玻璃主題解析：傳入 pillGlassConfig 時採用之，否則回退 DynamicLightTabBarConfig 的預設水滴效果
        val pillBaseColor = pillGlassConfig?.baseColor
            ?: Color(DynamicLightTabBarLegacyConfig.INDICATOR_BACKGROUND_HEX)
        val pillBodyTopAlpha = pillGlassConfig?.bodyTopAlpha
            ?: DynamicLightTabBarLegacyConfig.INDICATOR_BODY_TOP_ALPHA
        val pillBodyBottomAlpha = pillGlassConfig?.bodyBottomAlpha
            ?: DynamicLightTabBarLegacyConfig.INDICATOR_BODY_BOTTOM_ALPHA
        val pillHighlightInner = pillGlassConfig?.highlightInnerAlpha
            ?: DynamicLightTabBarLegacyConfig.SPECKLE_INNER_ALPHA
        val pillHighlightOuter = pillGlassConfig?.highlightOuterAlpha
            ?: DynamicLightTabBarLegacyConfig.SPECKLE_OUTER_ALPHA
        val pillHighlightCenterX = pillGlassConfig?.highlightCenterX
            ?: DynamicLightTabBarLegacyConfig.WATER_DROP_HIGHLIGHT_CENTER_X_FRACTION
        val pillHighlightCenterY = pillGlassConfig?.highlightCenterY
            ?: DynamicLightTabBarLegacyConfig.WATER_DROP_HIGHLIGHT_CENTER_Y_FRACTION
        val pillHighlightRadius = pillGlassConfig?.highlightRadiusFactor
            ?: DynamicLightTabBarLegacyConfig.WATER_DROP_GLOW_RADIUS_FACTOR
        val pillBorderTop = pillGlassConfig?.borderTopAlpha
            ?: DynamicLightTabBarLegacyConfig.INDICATOR_BORDER_TOP_ALPHA
        val pillBorderBottom = pillGlassConfig?.borderBottomAlpha
            ?: DynamicLightTabBarLegacyConfig.INDICATOR_BORDER_BOTTOM_ALPHA

        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffsetPx.toInt(), 0) }
                .width(itemWidthDp)
                .fillMaxHeight()
                .padding(4.dp)
                .graphicsLayer {
                    scaleX = indicatorScaleX.value
                    scaleY = indicatorScaleY.value
                    transformOrigin = TransformOrigin(indicatorTransformOriginX, 0.5f)
                    if (indicatorRenderEffect != null) {
                        renderEffect = indicatorRenderEffect.asComposeRenderEffect()
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // 陰影留在 Pill 外側，內部漸層與高光則裁切成獨立膠囊，
                    // 避免矩形底色延伸到整個 Tab item。
                    .shadow(
                        elevation = DynamicLightTabBarLegacyConfig.INDICATOR_SHADOW_ELEVATION_DP.dp,
                        shape = indicatorShape,
                        ambientColor = Color.Black.copy(alpha = DynamicLightTabBarLegacyConfig.INDICATOR_SHADOW_AMBIENT_ALPHA),
                        spotColor = Color.Black.copy(alpha = DynamicLightTabBarLegacyConfig.INDICATOR_SHADOW_SPOT_ALPHA)
                    )
                    .clip(indicatorShape)
                    // 半透明白玻璃水滴：讓底下背景透出，呈現通透玻璃感
                    .drawBehind {
                        // 1) 玻璃基質（半透明白，透出底下深色 → 磨砂玻璃）
                        drawRect(
                            brush = Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to pillBaseColor.copy(alpha = pillBodyTopAlpha),
                                    1f to pillBaseColor.copy(alpha = pillBodyBottomAlpha)
                                )
                            ),
                            topLeft = Offset.Zero,
                            size = size
                        )
                        // 2) 左上角柔光：鋪滿 Pill 後自然衰減，避免橢圓邊界形成生硬分界
                        if (showWaterHighlight && !isTouching) {
                            drawRect(
                                brush = Brush.radialGradient(
                                    colorStops = arrayOf(
                                        0f to glassInk.copy(alpha = pillHighlightInner),
                                        0.45f to glassInk.copy(alpha = pillHighlightOuter),
                                        1f to Color.Transparent
                                    ),
                                    center = Offset(
                                        size.width * pillHighlightCenterX,
                                        size.height * pillHighlightCenterY
                                    ),
                                    radius = size.width * pillHighlightRadius
                                ),
                                topLeft = Offset.Zero,
                                size = size,
                                blendMode = glassBlend
                            )
                        }
                    }
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                glassBorderInk.copy(alpha = pillBorderTop),
                                glassBorderInk.copy(alpha = pillBorderBottom)
                            )
                        ),
                        shape = indicatorShape
                    )
            )
        }

        // Tab Items
        Row(modifier = Modifier.fillMaxSize().selectableGroup()) {
            items.forEachIndexed { index, label ->
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val selected = index == selectedIndex
                val itemScale by animateFloatAsState(
                    targetValue = if (isPressed) DynamicLightTabBarLegacyConfig.PRESSED_SCALE else 1f,
                    animationSpec = spring(
                        stiffness = DynamicLightTabBarLegacyConfig.PRESSED_SPRING_STIFFNESS,
                        dampingRatio = DynamicLightTabBarLegacyConfig.PRESSED_SPRING_DAMPING
                    ),
                    label = "itemScale"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .graphicsLayer {
                            scaleX = itemScale
                            scaleY = itemScale
                        }
                        .selectable(
                            selected = selected,
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.Tab,
                            onClick = {
                                onSelect(index)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = label,
                        style = LocalTextStyle.current.copy(
                            color = if (selected) tabTextColor
                            else tabTextColor.copy(alpha = DynamicLightTabBarLegacyConfig.TAB_TEXT_UNSELECTED_ALPHA),
                            fontSize = DynamicLightTabBarLegacyConfig.TAB_TEXT_SIZE_SP.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    )
                }
            }
        }
    }
}

/**
 * AGSL 動態光照 Shader：
 * 手電筒式柔和光暈——內圈平坦、邊緣平滑收斂，跟隨 touchPos 移動，無明顯亮點。
 */
private const val INDICATOR_SHADER_SRC = """
uniform shader content;
uniform float2 resolution;
uniform float2 touchPos;
uniform float intensity;
uniform float radius;

const half3 kLightColor = half3(1.0, 1.0, 1.0);
const half3 kAmbient    = half3(0.10, 0.10, 0.16);

half4 main(float2 coord) {
    half4 base = content.eval(coord);

    float dist = length(coord - touchPos);
    float norm = clamp(dist / radius, 0.0, 1.0);

    // 從觸點開始連續衰減，不截平中心亮度，避免出現可辨識的圓形亮核。
    // 半徑由外部放大、強度同步降低，讓整片光暈更寬、更柔和。
    float glow = pow(1.0 - norm, 1.7);
    glow *= intensity;

    float2 uv = coord / resolution;
    float edgeX = min(uv.x, 1.0 - uv.x) * 2.0;
    float edgeY = min(uv.y, 1.0 - uv.y) * 2.0;
    float fresnel = pow(1.0 - min(edgeX, edgeY), 2.0) * 0.10;

    half3 lighting = kLightColor * glow + kAmbient * fresnel;
    half3 result = base.rgb + lighting * base.a;
    return half4(result, base.a);
}
"""
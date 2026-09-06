package top.hasiyliquidglassdemo.ui

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import top.hasiy.designsystem.GlassConfig
import top.hasiy.designsystem.glassBackdrop
import top.hasiy.designsystem.isLightSurface

/** 淺色主題下 Bar 底相對表面色的壓暗比例：只需要一點層次，壓多了就變回深色條 */
private const val LIGHT_BAR_DARKEN = 0.10f

/**
 * Tab Bar 的單一導航項：穩定 key + 文字標籤 + 圖標。
 *
 * @param key 穩定標識，供 Compose 在列表變動時正確复用 Item 狀態
 * @param label 標籤文字（由呼叫端透過字串資源提供，不在元件內寫死業務文案）
 * @param icon 圖標，顯示於標籤上方
 */
@Immutable
data class DynamicLightTabItem(
    val key: String,
    val label: String,
    val icon: ImageVector,
)

/**
 * 視覺槽位：Bar 從左到右的實際排列位置。
 *
 * 中央操作按鈕（若存在）只佔一個視覺槽位，不佔業務索引；
 * [selectedIndex] 永遠只對應 [DynamicLightTabBar] 的 `items`。
 */
internal enum class TabSlot { Tab, Center }

/** 依 items 數量與是否有中央按鈕，生成從左到右的槽位序列 */
internal fun tabSlots(itemCount: Int, hasCenterAction: Boolean): List<TabSlot> {
    val left = itemCount / 2
    val right = itemCount - left
    return buildList(itemCount + if (hasCenterAction) 1 else 0) {
        repeat(left) { add(TabSlot.Tab) }
        if (hasCenterAction) add(TabSlot.Center)
        repeat(right) { add(TabSlot.Tab) }
    }
}

/** 槽位序列 → 業務索引；中央槽位為 null。索引只對應 `items`，與視覺位置解耦 */
internal fun slotItemIndices(slots: List<TabSlot>): List<Int?> {
    var itemIndex = 0
    return slots.map { slot ->
        when (slot) {
            TabSlot.Tab -> itemIndex++
            TabSlot.Center -> null
        }
    }
}

/**
 * 拖動/點擊命中：以「普通 Tab 槽位的中心」決定選中項。
 *
 * 中央按鈕不參與選擇：拖動經過其範圍時保持最近一次普通 Tab，
 * 越過其中心後才切換到另一側的 Item（由中心點比較自然實現）。
 */
internal fun tabIndexAtPosition(
    touchX: Float,
    barWidthPx: Float,
    slots: List<TabSlot>,
): Int? {
    if (barWidthPx <= 0f || slots.isEmpty() || touchX.isNaN()) return null
    val slotWidth = barWidthPx / slots.size
    var bestIndex: Int? = null
    var bestDistance = Float.MAX_VALUE
    var itemIndex = 0
    slots.forEachIndexed { slotIndex, slot ->
        when (slot) {
            TabSlot.Tab -> {
                val center = slotWidth * slotIndex + slotWidth / 2f
                val distance = kotlin.math.abs(touchX - center)
                if (distance < bestDistance) {
                    bestDistance = distance
                    bestIndex = itemIndex
                }
                itemIndex++
            }
            TabSlot.Center -> Unit
        }
    }
    return bestIndex
}

internal fun indicatorTransformOriginFor(fromIndex: Int, toIndex: Int): Float = when {
    toIndex > fromIndex -> 0f
    toIndex < fromIndex -> 1f
    else -> 0.5f
}

/**
 * Indicator 目標位移（以 Bar 左緣為基準）。
 *
 * Indicator 寬度與「普通 Tab 槽位」等寬；有中央按鈕時，
 * 右半側 Item 的槽位序號天然包含中央槽位，位移自動跳過它。
 */
internal fun indicatorTargetOffsetPx(
    isDragging: Boolean,
    touchX: Float,
    itemWidthPx: Float,
    barWidthPx: Float,
    selectedIndex: Int,
    slots: List<TabSlot>,
): Float = if (isDragging && !touchX.isNaN() && itemWidthPx > 0f) {
    // 拖動時跟手，並限制在 Bar 範圍內（Indicator 寬 = 普通槽位寬）
    (touchX - itemWidthPx / 2f).coerceIn(0f, barWidthPx - itemWidthPx)
} else {
    val slotIndex = slotItemIndices(slots).indexOf(selectedIndex)
    if (slotIndex >= 0) slotIndex * itemWidthPx else 0f
}

/**
 * Lens 左緣位置（以 Bar 左緣為基準）。
 *
 * Lens 中心永遠對齊「選中槽位中心」，寬度 = 槽位寬 × [lensWidthFactor]；
 * 首尾 Item 處允許 Lens 向 Bar 兩側伸出少許（父層未裁切），
 * 以確保中心與選中項一致，避免折射內容與底層錯位產生重影。
 * 若真要嚴守 Bar 邊界，需改為「中心對齊」而非「左緣夾 0」。
 */
internal fun lensLeftOffsetPx(
    animatedOffsetPx: Float,
    itemWidthPx: Float,
    barWidthPx: Float,
    lensWidthFactor: Float,
): Float {
    if (itemWidthPx <= 0f || barWidthPx <= 0f) return 0f
    val lensWidth = itemWidthPx * lensWidthFactor
    val center = animatedOffsetPx + itemWidthPx / 2f
    return center - lensWidth / 2f
}

/**
 * 動態光照底部 Tab Bar（Apple Liquid Glass 風格）。
 *
 * - 拖動時選擇指示器即時跟隨手指位置，鬆手後自動吸附到最近的 Item 並回呼
 * - AGSL RuntimeShader 產生動態光影（API 33+），低版本自動降級為漸層模擬
 * - Haze 毛玻璃背景（API 31+，低版本磨砂回退）、Haptic Feedback 與縮放反饋
 *
 * 槽位規則：`items` 均分視覺槽位；提供 [centerAction] 時，中央插入一個
 * 獨立操作按鈕槽位，它不參與 [selectedIndex]，拖動經過時保持最近一次普通 Tab。
 *
 * @param items Tab 項目列表（圖標在上、標籤在下）
 * @param selectedIndex 當前選中索引，只對應 [items]，不包含中央按鈕
 * @param onSelect 選中回呼，拖動結束或點擊時觸發
 * @param centerAction 中央主操作按鈕內容；為 null 時不佔槽位
 * @param onCenterActionClick 中央按鈕點擊回呼（含觸覺反饋與按壓動畫由元件提供）
 * @param centerActionDescription 中央按鈕的無障礙描述
 * @param showWaterHighlight 是否顯示水滴頂部高光光暈（含感應器跟隨），預設由 Config 控制
 * @param pillGlassConfig 選中 Pill 的玻璃主題（null 時使用 [DynamicLightTabBarConfig] 的預設水滴效果）
 * @param modifier 外部修飾符
 */
@Composable
fun DynamicLightTabBar(
    items: List<DynamicLightTabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    centerAction: (@Composable () -> Unit)? = null,
    onCenterActionClick: (() -> Unit)? = null,
    centerActionDescription: String? = null,
    showWaterHighlight: Boolean = DynamicLightTabBarConfig.WATER_DROP_ENABLED,
    pillGlassConfig: GlassConfig? = null,
    lensRefractionEnabled: Boolean = DynamicLightTabBarConfig.LENS_REFRACTION_ENABLED_DEFAULT,
    modifier: Modifier = Modifier
) {
    // 主題派生色：這個 Bar 原本整套是為深色背景寫死的（白反光、白描邊、白字）。
    // 淺色主題（Neutral）下白色全部不可見，因此改由 pillGlassConfig 推導。
    // 反光/高光用色與混合模式：深色底提亮，淺色底壓暗
    val glassInk = pillGlassConfig?.highlightColor ?: Color.White
    val glassBlend = pillGlassConfig?.highlightBlendMode ?: BlendMode.Screen
    val glassBorderInk = pillGlassConfig?.borderColor ?: Color.White
    val tabTextColor = pillGlassConfig?.contentColor ?: Color.White
    // 淺色主題的 Bar 底：從表面色壓深一階，取代寫死的深色 BACKGROUND_COLOR_HEX
    val barBackgroundColor = if (pillGlassConfig?.isLightSurface == true) {
        lerp(pillGlassConfig.baseColor, Color.Black, LIGHT_BAR_DARKEN)
    } else {
        Color(DynamicLightTabBarConfig.BACKGROUND_COLOR_HEX)
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

    // 視覺槽位：items 均分；有中央按鈕時在中間插入一個 Center 槽位。
    // 業務索引（selectedIndex）只對應 items，與槽位序號透過 slotItemIndices 映射。
    val hasCenterAction = centerAction != null
    val slots = remember(items.size, hasCenterAction) {
        tabSlots(items.size, hasCenterAction)
    }
    val itemIndexBySlot = remember(slots) { slotItemIndices(slots) }

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
                    targetValue = DynamicLightTabBarConfig.INDICATOR_STRETCH_SCALE_X,
                    animationSpec = tween(DynamicLightTabBarConfig.INDICATOR_STRETCH_DURATION_MS)
                )
                indicatorScaleX.animateTo(
                    targetValue = DynamicLightTabBarConfig.INDICATOR_SETTLE_SCALE_X,
                    animationSpec = tween(DynamicLightTabBarConfig.INDICATOR_SETTLE_DURATION_MS)
                )
                indicatorScaleX.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        stiffness = DynamicLightTabBarConfig.SCALE_SPRING_STIFFNESS,
                        dampingRatio = DynamicLightTabBarConfig.SCALE_SPRING_DAMPING
                    )
                )
            }
            launch {
                indicatorScaleY.snapTo(1f)
                indicatorScaleY.animateTo(
                    targetValue = DynamicLightTabBarConfig.INDICATOR_STRETCH_SCALE_Y,
                    animationSpec = tween(DynamicLightTabBarConfig.INDICATOR_STRETCH_DURATION_MS)
                )
                indicatorScaleY.animateTo(
                    targetValue = DynamicLightTabBarConfig.INDICATOR_SETTLE_SCALE_Y,
                    animationSpec = tween(DynamicLightTabBarConfig.INDICATOR_SETTLE_DURATION_MS)
                )
                indicatorScaleY.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        stiffness = DynamicLightTabBarConfig.SCALE_SPRING_STIFFNESS,
                        dampingRatio = DynamicLightTabBarConfig.SCALE_SPRING_DAMPING
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
            try {
                RuntimeShader(INDICATOR_SHADER_SRC)
            } catch (e: Exception) {
                null
            }
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
        // 光暈半徑以「單一槽位寬度」為基準，讓它不超過槽位大小
        val itemW = if (slots.isNotEmpty()) barWidthPx / slots.size else barWidthPx
        barShader.setFloatUniform("intensity", DynamicLightTabBarConfig.BAR_GLOW_INTENSITY)
        barShader.setFloatUniform(
            "radius",
            itemW * DynamicLightTabBarConfig.BAR_GLOW_RADIUS_FACTOR
        )
        RenderEffect.createRuntimeShaderEffect(barShader, "content")
    } else null

    // 外層容器高度按 Lens 計算：Lens 可上下突出 Bar。
    // Bar 在容器內垂直居中，Lens 是 Bar 的覆蓋層（兄弟級），不受 Bar Shape 裁切。
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(DynamicLightTabBarConfig.LENS_HEIGHT_DP.dp)
    ) {
        // ===== 計算（Bar 與 Lens 共用）=====
        // 槽位寬度 = Bar 寬 / 槽位數（含中央按鈕槽位）；Lens 與普通 Tab 槽位相關。
        val itemWidthPx = if (slots.isNotEmpty()) barWidthPx / slots.size else 0f

        // 選中指示器位置（Lens 中心基準）
        val targetOffsetPx = indicatorTargetOffsetPx(
            isDragging = isDragging,
            touchX = touchX,
            itemWidthPx = itemWidthPx,
            barWidthPx = barWidthPx,
            selectedIndex = selectedIndex,
            slots = slots,
        )
        val animatedOffsetPx by animateFloatAsState(
            targetValue = targetOffsetPx,
            animationSpec = spring(
                stiffness = DynamicLightTabBarConfig.OFFSET_SPRING_STIFFNESS,
                dampingRatio = DynamicLightTabBarConfig.OFFSET_SPRING_DAMPING
            ),
            label = "indicatorOffset"
        )

        // 選中 Lens 材質解析：傳入 pillGlassConfig 時採用之，否則回退 Config 的預設暗色玻璃
        val lensShape = RoundedCornerShape(
            DynamicLightTabBarConfig.LENS_CORNER_RADIUS_DP.dp
        )
        val pillBaseColor = pillGlassConfig?.baseColor
            ?: Color(DynamicLightTabBarConfig.INDICATOR_BACKGROUND_HEX)
        val pillBodyTopAlpha = pillGlassConfig?.bodyTopAlpha
            ?: DynamicLightTabBarConfig.INDICATOR_BODY_TOP_ALPHA
        val pillBodyBottomAlpha = pillGlassConfig?.bodyBottomAlpha
            ?: DynamicLightTabBarConfig.INDICATOR_BODY_BOTTOM_ALPHA
        val pillHighlightInner = pillGlassConfig?.highlightInnerAlpha
            ?: DynamicLightTabBarConfig.SPECKLE_INNER_ALPHA
        val pillHighlightOuter = pillGlassConfig?.highlightOuterAlpha
            ?: DynamicLightTabBarConfig.SPECKLE_OUTER_ALPHA
        val pillHighlightCenterX = pillGlassConfig?.highlightCenterX
            ?: DynamicLightTabBarConfig.WATER_DROP_HIGHLIGHT_CENTER_X_FRACTION
        val pillHighlightCenterY = pillGlassConfig?.highlightCenterY
            ?: DynamicLightTabBarConfig.WATER_DROP_HIGHLIGHT_CENTER_Y_FRACTION
        val pillHighlightRadius = pillGlassConfig?.highlightRadiusFactor
            ?: DynamicLightTabBarConfig.WATER_DROP_GLOW_RADIUS_FACTOR
        val pillBorderTop = pillGlassConfig?.borderTopAlpha
            ?: DynamicLightTabBarConfig.INDICATOR_BORDER_TOP_ALPHA
        val pillBorderBottom = pillGlassConfig?.borderBottomAlpha
            ?: DynamicLightTabBarConfig.INDICATOR_BORDER_BOTTOM_ALPHA

        // 選中 Lens 幾何：寬於單一槽位（約 1.45×），並在 Bar 上下突出。
        // 首尾 Item 依 Lens 自身寬度限位，避免越界或被父層推平。
        val lensWidthPx = itemWidthPx * DynamicLightTabBarConfig.LENS_WIDTH_FACTOR
        val lensHeightDp = DynamicLightTabBarConfig.LENS_HEIGHT_DP.dp
        val lensHeightPx = with(density) { lensHeightDp.toPx() }
        val lensLeftPx = lensLeftOffsetPx(
            animatedOffsetPx = animatedOffsetPx,
            itemWidthPx = itemWidthPx,
            barWidthPx = barWidthPx,
            lensWidthFactor = DynamicLightTabBarConfig.LENS_WIDTH_FACTOR,
        )
        // 外層容器高度即 Lens 高度，故 Lens 填滿容器即與 Bar 同中心、
        // 上下對稱突出（上下各 (LENS_HEIGHT_DP-BAR_HEIGHT_DP)/2）。
        val lensTopPx = 0f

        // 折射 Shader（AGSL 採樣）：僅在 lensRefractionEnabled 時啟用，做內容放大、徑向扭曲、
        // RGB 分通道色散與 Fresnel；個別 GPU 編譯失敗時降級為 null（僅玻璃鏡面）。
        // 關閉時 refractionRenderEffect 為 null，Lens 只保留純玻璃效果（預設、無重影）。
        val refractionShader = remember(lensRefractionEnabled) {
            if (lensRefractionEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    RuntimeShader(LENS_REFRACTION_SHADER_SRC)
                } catch (e: Exception) {
                    null
                }
            } else null
        }
        val refractionRenderEffect = if (
            lensRefractionEnabled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            refractionShader != null &&
            lensWidthPx > 0 &&
            lensHeightPx > 0
        ) {
            refractionShader.setFloatUniform("lensSize", lensWidthPx, lensHeightPx)
            refractionShader.setFloatUniform(
                "lensCenter",
                lensWidthPx / 2f,
                lensHeightPx / 2f
            )
            refractionShader.setFloatUniform(
                "magnification",
                DynamicLightTabBarConfig.LENS_MAGNIFICATION
            )
            refractionShader.setFloatUniform(
                "chromaticPx",
                DynamicLightTabBarConfig.LENS_CHROMATIC_PX
            )
            refractionShader.setFloatUniform(
                "distortion",
                DynamicLightTabBarConfig.LENS_DISTORTION
            )
            refractionShader.setFloatUniform(
                "fresnelStrength",
                DynamicLightTabBarConfig.LENS_FRESNEL_STRENGTH
            )
            refractionShader.setFloatUniform(
                "edgeChromatic",
                DynamicLightTabBarConfig.LENS_EDGE_CHROMATIC_STRENGTH
            )
            RenderEffect.createRuntimeShaderEffect(refractionShader, "content")
        } else null

        // ===== Bar 層：毛玻璃、暗色填充、描邊、陰影、手勢、內容 =====
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(DynamicLightTabBarConfig.BAR_HEIGHT_DP.dp)
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
                                DynamicLightTabBarConfig.BAR_CORNER_RADIUS_DP.dp
                            ),
                            config = pillGlassConfig
                        )
                    } else {
                        Modifier
                    }
                )
                .background(
                    color = barBackgroundColor
                        .copy(alpha = DynamicLightTabBarConfig.BACKGROUND_ALPHA),
                    shape = RoundedCornerShape(DynamicLightTabBarConfig.BAR_CORNER_RADIUS_DP.dp)
                )
                .shadow(
                    elevation = DynamicLightTabBarConfig.OUTER_SHADOW_ELEVATION_DP.dp,
                    shape = RoundedCornerShape(DynamicLightTabBarConfig.BAR_CORNER_RADIUS_DP.dp),
                    ambientColor = Color(DynamicLightTabBarConfig.OUTER_SHADOW_AMBIENT_HEX)
                        .copy(alpha = DynamicLightTabBarConfig.OUTER_SHADOW_AMBIENT_ALPHA),
                    spotColor = Color(DynamicLightTabBarConfig.OUTER_SHADOW_SPOT_HEX)
                        .copy(alpha = DynamicLightTabBarConfig.OUTER_SHADOW_SPOT_ALPHA)
                )
                // 玻璃邊緣描邊：頂部最亮、往底部漸淡，模擬玻璃上緣反光的邊框
                .then(
                    if (DynamicLightTabBarConfig.GLASS_BORDER_ENABLED) {
                        Modifier.border(
                            width = DynamicLightTabBarConfig.GLASS_BORDER_WIDTH_DP.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    glassBorderInk.copy(alpha = DynamicLightTabBarConfig.GLASS_BORDER_TOP_ALPHA),
                                    glassBorderInk.copy(alpha = DynamicLightTabBarConfig.GLASS_BORDER_BOTTOM_ALPHA)
                                )
                            ),
                            shape = RoundedCornerShape(DynamicLightTabBarConfig.BAR_CORNER_RADIUS_DP.dp)
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
                            tabIndexAtPosition(touchX, barWidthPx, slots)?.let { index ->
                                if (index != lastHapticIndex) {
                                    // 1.7.x 未提供 SegmentTick，依文件建議降級為 TextHandleMove
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    lastHapticIndex = index
                                }
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            tabIndexAtPosition(touchX, barWidthPx, slots)?.let { index ->
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
        // 玻璃反光層：頂部白色高光漸層，模擬磨砂玻璃的反光質感（覆於整個 Bar）
        if (DynamicLightTabBarConfig.GLASS_REFLECTION_ENABLED) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                glassInk.copy(alpha = DynamicLightTabBarConfig.GLASS_REFLECTION_TOP_ALPHA),
                                glassInk.copy(alpha = DynamicLightTabBarConfig.GLASS_REFLECTION_BOTTOM_ALPHA)
                            ),
                            startY = 0f
                        ),
                        shape = RoundedCornerShape(DynamicLightTabBarConfig.BAR_CORNER_RADIUS_DP.dp)
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

        // Tab Items 與中央按鈕：按視覺槽位從左到右排列。
        // 中央按鈕只佔槽位、不佔業務索引；點擊區域以完整槽位計算。
        Row(modifier = Modifier.fillMaxSize().selectableGroup()) {
            slots.forEachIndexed { slotIndex, slot ->
                when (slot) {
                    TabSlot.Tab -> {
                        val itemIndex = itemIndexBySlot[slotIndex] ?: return@forEachIndexed
                        val item = items[itemIndex]
                        val interactionSource = remember(item.key) { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val selected = itemIndex == selectedIndex
                        val itemScale by animateFloatAsState(
                            targetValue = if (isPressed) DynamicLightTabBarConfig.PRESSED_SCALE else 1f,
                            animationSpec = spring(
                                stiffness = DynamicLightTabBarConfig.PRESSED_SPRING_STIFFNESS,
                                dampingRatio = DynamicLightTabBarConfig.PRESSED_SPRING_DAMPING
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
                                        onSelect(itemIndex)
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            TabItemContent(
                                item = item,
                                selected = selected,
                                tabTextColor = tabTextColor
                            )
                        }
                    }
                    TabSlot.Center -> {
                        // 獨立主操作按鈕：不參與 selectedIndex，有自己的點擊回呼、
                        // 觸覺反饋、按壓動畫與 Button 無障礙語義。
                        val centerInteractionSource = remember { MutableInteractionSource() }
                        val centerPressed by centerInteractionSource.collectIsPressedAsState()
                        val centerScale by animateFloatAsState(
                            targetValue = if (centerPressed) DynamicLightTabBarConfig.PRESSED_SCALE else 1f,
                            animationSpec = spring(
                                stiffness = DynamicLightTabBarConfig.PRESSED_SPRING_STIFFNESS,
                                dampingRatio = DynamicLightTabBarConfig.PRESSED_SPRING_DAMPING
                            ),
                            label = "centerActionScale"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(DynamicLightTabBarConfig.CENTER_ACTION_SIZE_DP.dp)
                                    .graphicsLayer {
                                        scaleX = centerScale
                                        scaleY = centerScale
                                    }
                                    .clip(CircleShape)
                                    .background(
                                        pillBaseColor.copy(alpha = pillBodyTopAlpha)
                                    )
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                glassBorderInk.copy(alpha = pillBorderTop),
                                                glassBorderInk.copy(alpha = pillBorderBottom)
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                                    .clickable(
                                        interactionSource = centerInteractionSource,
                                        indication = null,
                                        role = Role.Button,
                                        onClick = {
                                            onCenterActionClick?.invoke()
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    )
                                    .semantics {
                                        if (centerActionDescription != null) {
                                            contentDescription = centerActionDescription
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                centerAction?.invoke()
                            }
                        }
                    }
                }
            }
        }

        }
        // ===== SelectionLensOverlay：覆蓋在導航內容之上的獨立選中透鏡 =====
        // 作為外層容器的直接子元素（不受 Bar 的 72dp 約束），允許上下突出 Bar；
        // 首尾依 Lens 自身寬度限位。
        Box(
            modifier = Modifier
                .offset { IntOffset(lensLeftPx.roundToInt(), lensTopPx.roundToInt()) }
                .width(with(density) { lensWidthPx.toDp() })
                .height(lensHeightDp)
                .graphicsLayer {
                    scaleX = indicatorScaleX.value
                    scaleY = indicatorScaleY.value
                    transformOrigin = TransformOrigin(indicatorTransformOriginX, 0.5f)
                }
        ) {
            // ===== RefractedContentLayer（可選折射模式）=====
            // 僅在開啟 lensRefractionEnabled 且折射 Shader 可用時繪製：居中渲染選中項內容，
            // 與 Lens 同軸，放大/扭曲/色散由 shader 處理；玻璃層覆蓋其上以遮住多餘底層。
            if (lensRefractionEnabled && refractionRenderEffect != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            renderEffect = refractionRenderEffect.asComposeRenderEffect()
                        }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        items.getOrNull(selectedIndex)?.let { selectedItem ->
                            TabItemContent(
                                item = selectedItem,
                                selected = true,
                                tabTextColor = tabTextColor
                            )
                        }
                    }
                }
            }
            // ===== Lens 玻璃材質（含陰影、漸層、高光、邊緣 Fresnel/色散、描邊）=====
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // 陰影留在 Lens 外側，內部漸層與高光則裁切成獨立膠囊，
                    // 避免矩形底色延伸到整個 Tab item。
                    .shadow(
                        elevation = DynamicLightTabBarConfig.INDICATOR_SHADOW_ELEVATION_DP.dp,
                        shape = lensShape,
                        ambientColor = Color.Black.copy(alpha = DynamicLightTabBarConfig.INDICATOR_SHADOW_AMBIENT_ALPHA),
                        spotColor = Color.Black.copy(alpha = DynamicLightTabBarConfig.INDICATOR_SHADOW_SPOT_ALPHA)
                    )
                    .clip(lensShape)
                    // 暗色玻璃 Lens：讓底下深色背景透出，形成通透玻璃感
                    .drawBehind {
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
                        // 邊緣 Fresnel 反光：中心透明、邊緣微亮，凸顯玻璃輪廓
                        drawRect(
                            brush = Brush.radialGradient(
                                colorStops = arrayOf(
                                    0f to Color.Transparent,
                                    0.72f to Color.Transparent,
                                    1f to glassInk.copy(alpha = DynamicLightTabBarConfig.LENS_FRESNEL_STRENGTH)
                                ),
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = maxOf(size.width, size.height) * 0.62f
                            ),
                            topLeft = Offset.Zero,
                            size = size,
                            blendMode = glassBlend
                        )
                        // 邊緣色散：極細的青/藍色帶貼近 Lens 上下邊，模擬玻璃色散
                        drawRect(
                            brush = Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to Color.Transparent,
                                    0.04f to Color(0xFF66CCFF).copy(alpha = DynamicLightTabBarConfig.LENS_EDGE_CHROMATIC_STRENGTH),
                                    0.16f to Color.Transparent,
                                    0.84f to Color.Transparent,
                                    0.96f to Color(0xFFFF88CC).copy(alpha = DynamicLightTabBarConfig.LENS_EDGE_CHROMATIC_STRENGTH),
                                    1f to Color.Transparent
                                )
                            ),
                            topLeft = Offset.Zero,
                            size = size,
                            blendMode = glassBlend
                        )
                        // 頂部柔和高光：僅在 Lens 高於 Bar 的突出部分更明亮
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
                        shape = lensShape
                    )
            ) {
            }
        }
    }
}

/**
 * 單一 Tab 項的圖標 + 標籤縱向內容。
 *
 * 供「正常導航內容」與「Lens 折射內容副本」共用，確保兩者排版尺寸、
 * 字體、圖標與顏色完全一致，避免折射層與底層錯位造成重影。
 */
@Composable
private fun TabItemContent(
    item: DynamicLightTabItem,
    selected: Boolean,
    tabTextColor: Color,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            DynamicLightTabBarConfig.TAB_ICON_LABEL_SPACING_DP.dp,
            Alignment.CenterVertically
        )
    ) {
        Icon(
            imageVector = item.icon,
            // 裝飾性圖標不重複朗讀：標籤已由 selectable 提供
            contentDescription = null,
            tint = if (selected) tabTextColor
            else tabTextColor.copy(alpha = DynamicLightTabBarConfig.TAB_TEXT_UNSELECTED_ALPHA),
            modifier = Modifier.size(DynamicLightTabBarConfig.TAB_ICON_SIZE_DP.dp)
        )
        BasicText(
            text = item.label,
            style = LocalTextStyle.current.copy(
                color = if (selected) tabTextColor
                else tabTextColor.copy(alpha = DynamicLightTabBarConfig.TAB_TEXT_UNSELECTED_ALPHA),
                fontSize = DynamicLightTabBarConfig.TAB_TEXT_SIZE_SP.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        )
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

/**
 * 選中 Lens 折射 Shader（AGSL 採樣，僅 lensRefractionEnabled 時生效）。
 *
 * 以「膠囊/橢圓」有符號距離計算 Lens 邊緣帶，把視覺能量集中在 Lens 邊緣：
 * - 中心內容輕微放大 + 徑向扭曲（凸透鏡感）
 * - 沿法線做 RGB 分通道採樣，產生克制的邊緣色散
 * - 邊緣疊加 Fresnel 高光與低透明度青/藍折射帶
 * 此處刻意不在中心補光，避免出現可辨識的圓形亮核。
 */
private const val LENS_REFRACTION_SHADER_SRC = """
uniform shader content;
uniform float2 lensSize;
uniform float2 lensCenter;
uniform float magnification;
uniform float chromaticPx;
uniform float distortion;
uniform float fresnelStrength;
uniform float edgeChromatic;

half4 main(float2 coord) {
    half4 base = content.eval(coord);

    float2 c = coord - lensCenter;
    float radial = length(c);
    float2 dir = float2(0.0, 0.0);
    if (radial > 0.0001) { dir = c / radial; }

    // 放大 + 輕微徑向扭曲：產生凸透鏡彎曲感
    float2 warped = lensCenter + c / magnification
        + dir * (distortion * 40.0 * radial / max(lensSize.x, 1.0));

    // 橢圓有符號距離（近似），用於邊緣帶；能量集中在 Lens 邊緣而非中心亮核
    float2 e = (coord - lensCenter) / max(lensSize * 0.5, float2(1.0, 1.0));
    float eLen = length(e) - 1.0;
    float edgeMask = 1.0 - clamp(abs(eLen) * max(lensSize.x, lensSize.y) * 0.25, 0.0, 1.0);

    // RGB 分通道採樣：沿法線偏移，產生克制的邊緣色散
    float2 rgbOff = dir * chromaticPx * edgeMask;
    float r = content.eval(warped + rgbOff).r;
    float g = content.eval(warped).g;
    float b = content.eval(warped - rgbOff).b;
    half3 refracted = half3(r, g, b);

    // 邊緣 Fresnel 高光 + 邊緣彩色折射帶
    half3 fresnelColor = half3(0.85, 0.95, 1.0) * (fresnelStrength * edgeMask);
    half3 chromEdge = half3(0.10 * edgeChromatic, 0.60 * edgeChromatic, 1.00 * edgeChromatic) * edgeMask;

    half3 result = refracted + fresnelColor + chromEdge;
    return half4(result, base.a);
}
"""

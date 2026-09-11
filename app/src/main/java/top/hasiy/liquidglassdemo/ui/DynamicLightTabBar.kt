package top.hasiyliquidglassdemo.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.fillMaxWidth
import top.hasiy.designsystem.GlassConfig
import top.hasiy.designsystem.GlassLensTabBar
import top.hasiy.designsystem.LocalGlassConfig
import top.hasiy.designsystem.GlassLensTabItem

/** 舊 Demo 呼叫端的型別相容入口，實作位於 SDK。 */
typealias DynamicLightTabItem = GlassLensTabItem

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
 * Lens / Indicator 目標位移（以 Bar 左緣為基準）。
 *
 * 拖動時回傳 Lens 跟手位置；未拖動時回傳 selectedIndex 對應的槽位。
 * 寬度與「普通 Tab 槽位」等寬；有中央按鈕時，右半側 Item 的槽位序號
 * 天然包含中央槽位，位移自動跳過它。
 */
internal fun indicatorTargetOffsetPx(
    isDragging: Boolean,
    touchX: Float,
    itemWidthPx: Float,
    barWidthPx: Float,
    selectedIndex: Int,
    slots: List<TabSlot>,
): Float = if (isDragging && !touchX.isNaN() && itemWidthPx > 0f) {
    // 拖動時只移動 Lens，並限制 Lens 中心位於 Bar 內；不改變 selectedIndex。
    (touchX - itemWidthPx / 2f).coerceIn(0f, barWidthPx - itemWidthPx)
} else {
    val slotIndex = slotItemIndices(slots).indexOf(selectedIndex)
    if (slotIndex >= 0) slotIndex * itemWidthPx else 0f
}

/**
 * Lens 左緣位置（以 Bar 左緣為基準）。
 *
 * Lens 中心對齊「選中槽位中心」，寬度 = 槽位寬 × [lensWidthFactor]；
 * 首尾 Item 允許 Lens 超出 Bar 內容區，避免被左右邊距夾住後造成中心偏移。
 * 外層容器的 safe drawing / horizontal padding 負責保護螢幕邊界。
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
 * 舊 Demo API 相容入口。新版鏡片的繪圖與手勢全部由 SDK GlassLensTabBar 處理。
 * showWaterHighlight 保留作原呼叫端相容參數；新版有獨立的亮面材質。
 */
@Suppress("UNUSED_PARAMETER")
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
    widthFraction: Float = 1f,
    modifier: Modifier = Modifier,
) {
    val showCenter = LocalConfiguration.current.orientation != Configuration.ORIENTATION_PORTRAIT
    GlassLensTabBar(
        items = items,
        selectedIndex = selectedIndex,
        onSelect = onSelect,
        modifier = modifier.fillMaxWidth(widthFraction.coerceIn(0f, 1f)),
        centerAction = if (showCenter) centerAction else null,
        onCenterActionClick = { onCenterActionClick?.invoke() },
        centerActionDescription = centerActionDescription,
        lensEnabled = lensRefractionEnabled,
        // 傳 config 就好：contentColor 會從它取，軌道邊界與反光也跟著表面明暗走
        config = pillGlassConfig ?: LocalGlassConfig.current,
    )
}

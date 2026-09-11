package top.hasiyliquidglassdemo.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import top.hasiy.designsystem.GlassConfig
import top.hasiy.designsystem.LocalGlassConfig
import top.hasiy.designsystem.SevenSegmentText
import top.hasiy.designsystem.accentToneColor
import top.hasiy.designsystem.lineColor
import top.hasiy.designsystem.mutedContentColor
import top.hasiy.designsystem.quietAccentColor
import top.hasiy.designsystem.onAccentColor
import top.hasiy.designsystem.panelColor
import top.hasiy.designsystem.screenContentColor
import top.hasiy.designsystem.tactileKeycap
import top.hasiy.designsystem.tokens.GlassVisualStyle
import top.hasiyliquidglassdemo.R

/** 抽屜從哪一側進來。 */
enum class PickerSide {
    /** 豎屏：底部抽屜。 */
    BOTTOM,

    /** 橫屏：從左側進。資料位在右半邊時用這個，才不會蓋住它。 */
    LEFT,

    /** 橫屏：從右側進。 */
    RIGHT,
}

/**
 * 指標編輯抽屜。
 *
 * 對應參考稿的 `.metric-picker`：豎屏是底部抽屜（`picker-portrait`），橫屏是側邊
 * 抽屜（`picker-landscape` + `drawer-left` / `drawer-right`）。抽屜出現在資料位的
 * **對側**——蓋住正在編輯的那一格就看不到自己在改什麼了。
 *
 * @param slot 正在編輯的資料位
 * @param state 當前狀態，決定候選項與哪些已被佔用
 * @param side 抽屜從哪一側進
 * @param config 玻璃主題參數
 * @param onPick 選中一個指標
 * @param onReset 恢復預設版面
 * @param onDismiss 關閉抽屜
 * @param modifier 外部修飾符
 */
@Composable
fun MetricPicker(
    slot: MetricSlot,
    state: WorkoutUiState,
    side: PickerSide,
    config: GlassConfig = LocalGlassConfig.current,
    onPick: (String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = state.metricAt(slot.key)
    var category by remember(slot.key) {
        mutableStateOf(current?.category ?: MetricCategory.REALTIME)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(config.panelColor.copy(alpha = SCRIM_ALPHA))
            // 點遮罩關閉，與參考稿一致；不要水波紋
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = stringResource(R.string.picker_scrim_desc),
                onClick = onDismiss,
            ),
        contentAlignment = when (side) {
            PickerSide.BOTTOM -> Alignment.BottomCenter
            PickerSide.LEFT -> Alignment.CenterStart
            PickerSide.RIGHT -> Alignment.CenterEnd
        },
    ) {
        PickerDialog(
            slot = slot,
            state = state,
            side = side,
            config = config,
            category = category,
            currentMetricId = current?.id,
            onCategoryChange = { category = it },
            onPick = onPick,
            onReset = onReset,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun PickerDialog(
    slot: MetricSlot,
    state: WorkoutUiState,
    side: PickerSide,
    config: GlassConfig = LocalGlassConfig.current,
    category: MetricCategory,
    currentMetricId: String?,
    onCategoryChange: (MetricCategory) -> Unit,
    onPick: (String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val portrait = side == PickerSide.BOTTOM
    val density = LocalDensity.current
    // 抽屜一開就把焦點收進來，實體按鍵／讀屏才不會還停在底下的格子上。
    // 參考稿聚焦的是選中的分類頁籤，這裡收在對話框本身，往下走一步就是頁籤。
    val dialogFocus = remember { FocusRequester() }
    LaunchedEffect(slot.key) { runCatching { dialogFocus.requestFocus() } }
    val thresholdPx = with(density) { CLOSE_DISTANCE.toPx() }
    // 拖動距離只用來畫位移與判斷是否關閉，不進 UiState——它在手勢結束後就沒有意義
    var dragDistance by remember(slot.key) { mutableFloatStateOf(0f) }

    val shape = when (side) {
        PickerSide.BOTTOM -> RoundedCornerShape(topStart = SHEET_CORNER, topEnd = SHEET_CORNER)
        PickerSide.LEFT -> RoundedCornerShape(topEnd = DRAWER_CORNER)
        PickerSide.RIGHT -> RoundedCornerShape(topStart = DRAWER_CORNER)
    }
    val sizing = if (portrait) {
        Modifier.fillMaxWidth().fillMaxHeight(SHEET_MAX_HEIGHT_FRACTION)
    } else {
        Modifier.fillMaxHeight().widthIn(min = DRAWER_MIN_WIDTH).fillMaxWidth(DRAWER_WIDTH_FRACTION)
    }

    Box(
        modifier = Modifier
            .focusRequester(dialogFocus)
            .focusGroup()
            .offset {
                if (portrait) {
                    IntOffset(0, dragDistance.roundToInt())
                } else {
                    // 往抽屜自己那一側推才是「收起來」
                    val sign = if (side == PickerSide.RIGHT) 1 else -1
                    IntOffset((dragDistance * sign).roundToInt(), 0)
                }
            }
            .then(sizing)
            .clip(shape)
            .background(config.panelColor)
            .border(1.dp, config.lineColor, shape)
            // 對話框自己吃掉點擊，否則會穿透到遮罩上把抽屜關掉
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {}
            // 底部抽屜只讓開導航列：它從螢幕中段往下鋪，根本碰不到狀態列，
            // 套整組 safeDrawing 會在頂上白白多出一截狀態列高度的空白。
            // 側邊抽屜是滿高的，那才需要連狀態列一起讓。
            // 只讓開真的需要讓的那幾邊。
            //
            // 豎屏底部抽屜：從螢幕中段往下鋪，碰不到狀態列，只讓導航列。
            // 側邊抽屜：橫屏時 safeDrawing 含旋轉後的狀態列／挖孔，整組套下去會在
            // 抽屜**內側**墊出一大條空白，和豎屏完全不一致。只讓上下，加上貼著
            // 螢幕邊那一側。
            .windowInsetsPadding(
                when (side) {
                    PickerSide.BOTTOM -> WindowInsets.navigationBars
                    PickerSide.LEFT -> WindowInsets.safeDrawing
                        .only(WindowInsetsSides.Vertical + WindowInsetsSides.Left)

                    PickerSide.RIGHT -> WindowInsets.safeDrawing
                        .only(WindowInsetsSides.Vertical + WindowInsetsSides.Right)
                }
            ),
    ) {
        val onHandleDrag: (Float) -> Unit = { delta ->
            // 只認「往關閉方向」的位移，往回拉不該把抽屜拉得更開
            val towardsClose = if (portrait || side == PickerSide.RIGHT) delta else -delta
            dragDistance = (dragDistance + towardsClose).coerceAtLeast(0f)
        }
        val onHandleRelease: () -> Unit = {
            if (dragDistance >= thresholdPx) onDismiss() else dragDistance = 0f
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                // 頂部收窄：把手本身還帶觸控高度，四邊同樣 15dp 的話上面會空出快一倍。
                // 側邊抽屜的把手在內側邊上，那一側要讓出它的寬度。
                .padding(
                    start = if (side == PickerSide.RIGHT) HANDLE_LANE else DIALOG_PADDING,
                    end = if (side == PickerSide.LEFT) HANDLE_LANE else DIALOG_PADDING,
                    bottom = DIALOG_PADDING,
                    top = if (portrait) DIALOG_TOP_PADDING else DIALOG_PADDING,
                ),
        ) {
        if (portrait) {
            DragHandle(
                portrait = true,
                config = config,
                onDrag = onHandleDrag,
                onRelease = onHandleRelease,
            )
        }

        PickerHead(
            slot = slot,
            current = currentMetricId,
            portrait = portrait,
            onReset = onReset,
            config = config,
        )
        CategoryTabs(selected = category, config = config, onSelect = onCategoryChange)

        MetricChoiceGrid(
            candidates = state.candidatesIn(category),
            state = state,
            slotKey = slot.key,
            currentMetricId = currentMetricId,
            config = config,
            onPick = onPick,
            modifier = Modifier.weight(1f).padding(top = 12.dp),
        )
        }

        // 側邊抽屜是**橫向**拖關的，把手就得是豎的、貼在要拖向的那一側。
        // 沿用豎屏那根橫條會讓人以為往下拖——手勢方向和把手方向要對得上。
        if (!portrait) {
            DragHandle(
                portrait = false,
                config = config,
                onDrag = onHandleDrag,
                onRelease = onHandleRelease,
                modifier = Modifier.align(
                    if (side == PickerSide.LEFT) Alignment.CenterEnd else Alignment.CenterStart
                ),
            )
        }
    }
}

/**
 * 拖動關閉的把手。
 *
 * 方向跟著手勢走：豎屏是底部抽屜、往下拖，畫一根橫條擺在頂上；橫屏是側邊抽屜、
 * 往側邊拖，畫一根豎條貼在要拖向的那一側。把手的方向就是在告訴人往哪拖，
 * 兩邊都用橫條的話橫屏會被讀成「往下拖」。
 *
 * @param portrait 是不是豎屏的底部抽屜
 * @param config 玻璃主題參數
 * @param onDrag 拖動回呼，正值代表往關閉方向
 * @param onRelease 鬆手回呼
 * @param modifier 外部修飾符
 */
@Composable
private fun DragHandle(
    portrait: Boolean,
    config: GlassConfig = LocalGlassConfig.current,
    onDrag: (Float) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gesture = Modifier.pointerInput(portrait) {
        if (portrait) {
            detectVerticalDragGestures(onDragEnd = onRelease, onDragCancel = onRelease) { change, delta ->
                change.consume()
                onDrag(delta)
            }
        } else {
            detectHorizontalDragGestures(onDragEnd = onRelease, onDragCancel = onRelease) { change, delta ->
                change.consume()
                onDrag(delta)
            }
        }
    }

    Box(
        modifier = modifier
            .then(
                if (portrait) {
                    Modifier.fillMaxWidth().height(HANDLE_LANE)
                } else {
                    Modifier.fillMaxHeight().width(HANDLE_LANE)
                }
            )
            .then(gesture),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(
                    width = if (portrait) HANDLE_LENGTH else HANDLE_THICKNESS,
                    height = if (portrait) HANDLE_THICKNESS else HANDLE_LENGTH,
                )
                .clip(RoundedCornerShape(999.dp))
                .background(config.screenContentColor.copy(alpha = HANDLE_ALPHA))
        )
    }
}

@Composable
private fun PickerHead(
    slot: MetricSlot,
    current: String?,
    portrait: Boolean,
    onReset: () -> Unit,
    config: GlassConfig = LocalGlassConfig.current,
) {
    val currentLabel = current
        ?.let { MetricCatalog.find(it) }
        ?.let { stringResource(it.labelRes) }
        .orEmpty()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(
                    if (portrait) R.string.picker_mode_portrait else R.string.picker_mode_landscape
                ),
                color = config.quietAccentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
            // 標題與「哪一格 · 當前是什麼」只在豎屏出現。
            //
            // 側邊抽屜只有 ~300dp 寬、卻要鋪滿整個屏高，那兩行會佔掉頂上快 80dp，
            // 候選項就得多捲一屏。而且橫屏的資料位就在抽屜旁邊、還標著虛線框，
            // 「在改哪一格」看得見，不需要再寫一遍。
            if (portrait) {
                Text(
                    text = stringResource(R.string.picker_title),
                    color = config.screenContentColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    text = stringResource(
                        R.string.picker_slot_current,
                        slotName(slot),
                        currentLabel,
                    ),
                    color = config.mutedContentColor,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
        }
        // 恢復默認擺在右上角，和標題同一行——放在頁腳的話它會和候選項搶最後一排的
        // 視線，橫屏更是被擠成兩行。
        //
        // 參考稿的頁腳還有一句「候選項由設備能力動態提供；未支援的指標不會出現，
        // 也不會用 0 佔位」。那是給看設計稿的人讀的說明，不是給使用者的：畫面上
        // 本來就不會出現不支援的指標，寫出來反而佔掉一整塊。規則記在
        // [WorkoutUiState.candidatesIn] 上。
        Text(
            text = stringResource(R.string.picker_reset),
            color = config.quietAccentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .tactileClickable(onClick = onReset)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

/** 三個分類頁籤，等寬。 */
@Composable
private fun CategoryTabs(
    selected: MetricCategory,
    config: GlassConfig = LocalGlassConfig.current,
    onSelect: (MetricCategory) -> Unit,
) {
    val tactile = config.visualStyle == GlassVisualStyle.TACTILE
    val shape = RoundedCornerShape(TABS_CORNER)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 11.dp)
            .clip(shape)
            .background(config.screenContentColor.copy(alpha = TABS_TRACK_ALPHA))
            .border(1.dp, config.lineColor, shape)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        MetricCategory.entries.forEach { category ->
            val active = category == selected
            val tabSurface = if (tactile) {
                Modifier.tactileKeycap(TAB_CORNER)
            } else {
                Modifier
                    .clip(RoundedCornerShape(TAB_CORNER))
                    .background(if (active) config.accentToneColor else Color.Transparent)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(TAB_HEIGHT)
                    .tactileClickable { onSelect(category) }
                    .then(tabSurface),
                contentAlignment = Alignment.Center,
            ) {
                // 文字在頁籤裡置中；指示燈**疊**在上緣，不佔文字的位置——
                // 用 Column 把兩者上下排，文字就會被燈推得偏下
                Text(
                    text = stringResource(category.labelRes),
                    color = when {
                        tactile -> config.screenContentColor
                        active -> config.onAccentColor
                        else -> config.mutedContentColor
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (tactile && active) {
                    TactileLamp(
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = TAB_LAMP_TOP),
                        // 頁籤比指標卡窄，燈也要跟著收一號
                        width = TAB_LAMP_WIDTH,
                    )
                }
            }
        }
    }
}

/** 候選項：兩欄網格。項目數會隨分類變，用 Lazy 版比較省。 */
@Composable
private fun MetricChoiceGrid(
    candidates: List<WorkoutMetric>,
    state: WorkoutUiState,
    slotKey: String,
    currentMetricId: String?,
    config: GlassConfig = LocalGlassConfig.current,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    // 只有真的捲得動才顯示指示條，與參考稿的 .visible 一致
    val scrollable by remember {
        derivedStateOf { gridState.canScrollForward || gridState.canScrollBackward }
    }
    // 指示條要跟著捲動位置移動，不然它只是一根裝飾。LazyGrid 量不到總長度，
    // 用「已捲過的項目數 / 可捲項目數」近似——格子等高，誤差看不出來。
    val scrollFraction by remember {
        derivedStateOf {
            val info = gridState.layoutInfo
            val total = info.totalItemsCount
            val visible = info.visibleItemsInfo.size
            if (total <= visible) 0f
            else (gridState.firstVisibleItemIndex.toFloat() / (total - visible)).coerceIn(0f, 1f)
        }
    }

    Box(modifier = modifier) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = Modifier.fillMaxSize().padding(end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            items(candidates, key = { it.id }) { metric ->
                MetricChoice(
                    metric = metric,
                    selected = metric.id == currentMetricId,
                    usedElsewhere = state.isMetricUsedElsewhere(slotKey, metric.id),
                    config = config,
                    onClick = { onPick(metric.id) },
                )
            }
        }
        if (scrollable) {
            BoxWithConstraints(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(4.dp),
            ) {
                val travel = (maxHeight - INDICATOR_HEIGHT - 14.dp).coerceAtLeast(0.dp)
                Box(
                    Modifier
                        .offset(y = travel * scrollFraction)
                        .size(width = 4.dp, height = INDICATOR_HEIGHT)
                        .clip(RoundedCornerShape(999.dp))
                        .background(config.accentToneColor.copy(alpha = INDICATOR_ALPHA))
                )
            }
        }
    }
}

/**
 * 資料位的顯示名。
 *
 * 豎屏格與 dock 格的格數隨網格規格變，名稱走帶序號的模板；
 * 側邊四格是固定方位、名稱具名，沒有序號可填。
 */
@Composable
private fun slotName(slot: MetricSlot): String =
    slot.nameIndex?.let { stringResource(slot.nameRes, it) } ?: stringResource(slot.nameRes)

@Composable
private fun MetricChoice(
    metric: WorkoutMetric,
    selected: Boolean,
    usedElsewhere: Boolean,
    config: GlassConfig = LocalGlassConfig.current,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(CHOICE_CORNER)
    val border = when {
        selected -> config.accentToneColor
        // 已被別的格子用掉的仍然可選，只是提醒一聲——參考稿沒有禁用它
        usedElsewhere -> config.accentToneColor.copy(alpha = USED_BORDER_ALPHA)
        else -> config.lineColor
    }
    val fill = if (selected) {
        config.accentToneColor.copy(alpha = SELECTED_FILL_ALPHA)
    } else {
        config.screenContentColor.copy(alpha = CHOICE_FILL_ALPHA)
    }
    // 抽屜裡的候選項和頁面上的指標卡是同一種東西，材質要一致——
    // 參考稿的 `.theme-tactile .metric-choice` 就是一顆小一號的膠帽。
    //
    // 選中態在 Tactile 下**不換底色**：實體面板上「選中」是同一顆按鍵把指示燈點亮，
    // 換成一塊綠底就不是按鍵了。
    val tactile = config.visualStyle == GlassVisualStyle.TACTILE
    val surface = if (tactile) {
        Modifier.tactileKeycap(CHOICE_CORNER)
    } else {
        Modifier.clip(shape).background(fill).border(1.dp, border, shape)
    }

    Column(
        modifier = Modifier
            // 固定高度會把數值切掉：中文標籤與數字的行高不像參考稿的 7px/14px
            // 那麼可控，給下限讓它自己撐開
            .heightIn(min = CHOICE_MIN_HEIGHT)
            .tactileClickable(onClick = onClick)
            .then(surface)
            .padding(horizontal = 9.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(metric.labelRes),
                color = config.mutedContentColor,
                fontSize = 10.sp,
                // 給指標名一份權重：不給的話行尾那句「已在其他位置顯示」會把它
                // 擠到換行，同一排兩張卡的標題就一高一低
                modifier = Modifier.weight(1f),
            )
            // 行尾的槽位**寬高都固定**，不管裡面有沒有東西。
            //
            // 「已在其他位置顯示」是六個字，在 ~120dp 寬的候選卡上必然折成兩行；
            // 讓它自己撐開的話，被佔用的那幾張卡就比別的高一截，整片候選項參差不齊。
            // 所以按兩行預留，空的時候也佔著。
            Box(
                modifier = Modifier.size(width = BADGE_SLOT_WIDTH, height = BADGE_SLOT_HEIGHT),
                contentAlignment = Alignment.TopEnd,
            ) {
                when {
                    tactile && selected -> TactileLamp()
                    usedElsewhere -> Text(
                        text = stringResource(R.string.picker_used_badge),
                        color = config.quietAccentColor,
                        fontSize = 8.sp,
                        lineHeight = BADGE_LINE_HEIGHT,
                        textAlign = TextAlign.End,
                        minLines = BADGE_LINES,
                        maxLines = BADGE_LINES,
                    )
                }
            }
        }
        Row(
            modifier = Modifier.padding(top = 5.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            // Digital 下所有核心讀數都走七段，參考稿的 decorateDigitalValues
            // 也把 `.metric-choice strong` 算在內——抽屜裡是普通字體的話，
            // 選完貼回卡片就變數碼管，前後對不上
            if (config.visualStyle == GlassVisualStyle.DIGITAL) {
                SevenSegmentText(
                    text = metric.value,
                    color = config.screenContentColor,
                    fontSize = 15.sp,
                )
            } else {
                Text(
                    text = metric.value,
                    color = config.screenContentColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.alignByBaseline(),
                )
            }
            if (metric.unitRes != null) {
                // 同指標卡：單位對數值的**基線**，不是盒底
                Text(
                    text = stringResource(metric.unitRes),
                    color = config.mutedContentColor,
                    fontSize = 9.sp,
                    modifier = Modifier
                        .padding(start = 3.dp)
                        .then(
                            if (config.visualStyle == GlassVisualStyle.DIGITAL) {
                                Modifier.padding(bottom = 1.dp)
                            } else {
                                Modifier.alignByBaseline()
                            }
                        ),
                )
            }
        }
    }
}

private val MetricCategory.labelRes: Int
    get() = when (this) {
        MetricCategory.REALTIME -> R.string.picker_category_realtime
        MetricCategory.AVERAGE -> R.string.picker_category_average
        MetricCategory.AGGREGATE -> R.string.picker_category_aggregate
    }

/** 抽屜拖動關閉的門檻，spec 3.4 的 44 px。 */
private val CLOSE_DISTANCE = 44.dp

private const val SCRIM_ALPHA = 0.58f
private val DIALOG_PADDING = 15.dp
private val DIALOG_TOP_PADDING = 6.dp
private val SHEET_CORNER = 25.dp
private val DRAWER_CORNER = 24.dp
private const val SHEET_MAX_HEIGHT_FRACTION = 0.76f
private const val DRAWER_WIDTH_FRACTION = 0.42f
private val DRAWER_MIN_WIDTH = 310.dp

/** 把手所佔的那條帶：豎屏是高度，橫屏是寬度。 */
private val HANDLE_LANE = 18.dp
private val HANDLE_LENGTH = 38.dp
private val HANDLE_THICKNESS = 4.dp
private const val HANDLE_ALPHA = 0.24f

private val TABS_CORNER = 11.dp
private val TAB_CORNER = 8.dp
/**
 * 頁籤高度。
 *
 * 文字在整格置中，指示燈疊在上緣——高度要夠，兩者之間才留得出空隙。
 */
private val TAB_HEIGHT = 38.dp

private const val TABS_TRACK_ALPHA = 0.04f

/** 候選卡行尾徽標的槽位：按「已在其他位置顯示」折成兩行預留。 */
private val BADGE_SLOT_WIDTH = 34.dp
private val BADGE_SLOT_HEIGHT = 22.dp
private val BADGE_LINE_HEIGHT = 10.sp
private const val BADGE_LINES = 2

private val CHOICE_CORNER = 12.dp
private val CHOICE_MIN_HEIGHT = 56.dp
private const val CHOICE_FILL_ALPHA = 0.035f
private const val SELECTED_FILL_ALPHA = 0.11f
private const val USED_BORDER_ALPHA = 0.2f
private const val INDICATOR_ALPHA = 0.72f
private val TAB_LAMP_TOP = 4.dp
private val TAB_LAMP_WIDTH = 14.dp
private val INDICATOR_HEIGHT = 18.dp

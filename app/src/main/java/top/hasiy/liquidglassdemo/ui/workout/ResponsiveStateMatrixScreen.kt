package top.hasiyliquidglassdemo.ui.workout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import top.hasiy.designsystem.DesignSystemTheme
import top.hasiy.designsystem.GlassConfig
import top.hasiy.designsystem.GlassFilterChip
import top.hasiy.designsystem.LocalGlassConfig
import top.hasiy.designsystem.inkColor
import top.hasiy.designsystem.lineColor
import top.hasiy.designsystem.mutedContentColor
import top.hasiy.designsystem.panelColor
import top.hasiy.designsystem.screenColor
import top.hasiy.designsystem.screenContentColor
import top.hasiy.designsystem.tactilePanel
import top.hasiy.designsystem.tactileScreenTexture
import top.hasiy.designsystem.tokens.GlassVisualStyle
import top.hasiy.designsystem.tokens.ThemePalette
import top.hasiyliquidglassdemo.R

/**
 * 響應式狀態矩陣主畫面。
 *
 * 參考稿把手機外框也畫了出來（`.phone` / `.land`），那是網頁要在桌面上示意
 * 裝置才需要的；跑在真機上時螢幕本身就是外框，所以這裡直接鋪滿，
 * 不複刻 `WorkoutDeviceFrame`。
 *
 * 橫豎屏共用同一份狀態，只換排法——[BoxWithConstraints] 依可用寬高決定。
 *
 * @param palette 當前主題色
 * @param onBack 返回上一頁
 * @param onToggleOrientation 要求切換橫豎屏；方向鎖由外層持有，見 [OrientationToggle]
 * @param modifier 外部修飾符
 * @param controller 狀態持有者
 * @param speedDelta 儀表下方那行「比 X 前 +N」的變化量（km/h）。這是**業務資料**，
 *   不是排版參數——要比多久前、差多少，都得由使用端算好傳進來。
 *   不傳（或 [speedDeltaWindow] 不傳）就整行不顯示
 * @param speedDeltaWindow 上面那個比較的時間窗文案，例如「1 分鐘」
 * @param onPaletteChange 換配色。傳了才會在標題欄畫出那顆 `⋯`——這是**驗收入口**，
 *   讓八組配色能就地切著看，不必退回目錄繞一圈。不傳就沒有這顆按鈕
 */
@Composable
fun ResponsiveStateMatrixScreen(
    palette: ThemePalette,
    onBack: () -> Unit,
    onToggleOrientation: () -> Unit = {},
    modifier: Modifier = Modifier,
    controller: WorkoutController = rememberWorkoutController(),
    speedDelta: Float? = null,
    speedDeltaWindow: String? = null,
    onPaletteChange: ((ThemePalette) -> Unit)? = null,
) {
    DesignSystemTheme(palette = palette) {
        val config = LocalGlassConfig.current
        val state = controller.uiState

        // 抽屜與調節浮層都要吃掉返回鍵，否則按返回會直接退出整頁——浮層開著時
        // 使用者想退的是浮層，不是頁面。
        //
        // 只註冊一個 handler：兩者不會同時開（beginEdit 會清掉 adjustingSlotKey），
        // 分兩個註冊反而要去記 Compose 的處理順序。
        //
        // 執行狀態浮層（暫停、斷線、已結束）不在此列：那是狀態不是彈層，
        // 按返回把它關掉會讓人以為運動繼續了。
        BackHandler(enabled = state.hasOverlay || state.editingLayout) {
            when {
                state.editingSlotKey != null -> controller.endEdit()
                state.adjustingSlotKey != null -> controller.endAdjust()
                // 浮層都關掉了還按返回，就是要退出版面編輯
                else -> controller.setLayoutEditing(false)
            }
        }

        // 抽屜與狀態浮層要蓋住**整個螢幕**：它們如果跟主體一樣被關在有 padding 的
        // 容器裡，遮罩就會在頂欄那裡切一條邊（頁面明顯分成深淺兩塊），
        // 底部抽屜也做不到左右滿寬。
        val screenLandscape = LocalConfiguration.current.screenWidthDp.dp >= LANDSCAPE_MIN_WIDTH
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(config.screenColor)
                // Tactile 的屏底不是一塊死平的黑，參考稿鋪了一層 8px 一格的極淡點陣，
                // 那是磨砂塑料面板的質感來源
                .tactileScreenTexture(config.visualStyle == GlassVisualStyle.TACTILE),
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = PAGE_PADDING),
        ) {
            MatrixTopBar(
                config = config,
                onBack = onBack,
                landscape = screenLandscape,
                onToggleOrientation = onToggleOrientation,
                palette = palette,
                onPaletteChange = onPaletteChange,
                portraitSpec = state.portraitSpec,
                dockSpec = state.dockSpec,
                sideSpec = state.sideSpec,
                alignStart = state.alignStart,
                editingLayout = state.editingLayout,
                onSelectSpec = controller::setGridSpec,
                onSelectSideSpec = controller::setSideSpec,
                onToggleAlignStart = { controller.setAlignStart(!state.alignStart) },
                onToggleEditing = { controller.setLayoutEditing(!state.editingLayout) },
            )

            // 浮層要和主體共用同一組約束——抽屜從哪一側進、儀表多大，
            // 都得看同一個 maxWidth / maxHeight
            // 調節浮層要貼著被調節的那張卡彈在它上方，所以每個格子都把自己的位置
            // 報上來；存的是根座標，用時減掉容器原點換成容器內的座標。
            val slotBounds = remember { mutableStateMapOf<String, Rect>() }
            var containerOrigin by remember { mutableStateOf(Offset.Zero) }

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onGloballyPositioned { containerOrigin = it.positionInRoot() },
            ) {
                val landscape = maxWidth >= LANDSCAPE_MIN_WIDTH
                // 內層 Box 的 BoxScope 會把 BoxWithConstraintsScope 遮住，先取出來
                val availableHeight = maxHeight
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        // 調節浮層開著時主體整個退到後面（參考稿的
                        // `.phone.active .p-gauge/.p-metrics/.p-session/.p-bottom`
                        // 是 `opacity: .42`）。少了這一層，浮層就像憑空貼在一張
                        // 亮度不變的頁面上，前後分不開。
                        //
                        // 參考稿還帶了 `filter: saturate(.7)`，那個沒做：Compose 要
                        // 上 RenderEffect 才有，API 31 以下沒有，而且 alpha 已經把
                        // 「退到後面」講清楚了。
                        .alpha(if (state.adjustingSlotKey != null) BACKDROP_DIM else 1f),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    // 參考稿是照手機畫的。在平板上鋪滿會讓側邊卡片寬到失真，
                    // 所以內容封一個最大寬度並置中，多出來的留白
                    val content = Modifier.widthIn(max = CONTENT_MAX_WIDTH)
                    val onSlotBounds: (String, Rect) -> Unit = { key, rect ->
                        slotBounds[key] = rect
                    }
                    if (landscape) {
                        LandscapeLayout(
                            state, config, controller, availableHeight, content, onSlotBounds,
                            speedDelta, speedDeltaWindow,
                        )
                    } else {
                        PortraitLayout(
                            state, config, controller, content, onSlotBounds,
                            speedDelta, speedDeltaWindow,
                        )
                    }
                }

                // 調節浮層貼底，Toast 壓在它上面，狀態遮罩蓋住整個主體。
                // 三者的疊放順序就是這裡的宣告順序。
                val adjusting = state.adjustingMetric
                val range = adjusting?.let { ControlRange.forMetric(it.id) }
                if (adjusting != null && range != null) {
                    val anchor = state.adjustingSlotKey?.let { slotBounds[it] }
                    var panelSize by remember(adjusting.id) { mutableStateOf(IntSize.Zero) }
                    val density = LocalDensity.current
                    val gapPx = with(density) { SECTION_GAP.toPx() }
                    val boxW = with(density) { maxWidth.toPx() }
                    val boxH = with(density) { maxHeight.toPx() }

                    // 浮層沒有關閉鈕，點旁邊就收：鋪一層透明攔截層接住外側的點擊，
                    // 浮層自己再把點擊吃掉。
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                // 跟 MetricPicker 的遮罩一致：不給標籤的話，讀屏遇到的
                                // 就是一個蓋滿整屏、可點但講不出用途的節點
                                onClickLabel = stringResource(R.string.matrix_adjust_scrim_desc),
                                onClick = controller::endAdjust,
                            ),
                    ) {
                        ControlFocusPanel(
                            metric = adjusting,
                            currentValue = state.valueOf(adjusting),
                            range = range,
                            config = config,
                            onCommit = { controller.writeValue(adjusting.id, it) },
                            onStep = { controller.stepValue(adjusting.id, it) },
                            modifier = Modifier
                                .onGloballyPositioned { panelSize = it.size }
                                .offset {
                                    focusPanelOffset(
                                        anchor = anchor,
                                        origin = containerOrigin,
                                        panel = panelSize,
                                        boxWidth = boxW,
                                        boxHeight = boxH,
                                        gap = gapPx,
                                    )
                                }
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {},
                        )
                    }
                }

                // 參考稿的 .control-toast 貼在頂部（top: 103px），不是浮在中間——
                // 調節浮層在下半屏，Toast 壓上去會擋住正在調的讀數
                ControlWriteToast(
                    state = state.controlWrite,
                    config = config,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = TOAST_TOP_PADDING),
                )

            }
        }

        // 抽屜出現在資料位的對側：蓋住正在編輯的那一格，就看不到自己在改什麼
        val editingSlot = state.editingSlot
        if (editingSlot != null) {
            MetricPicker(
                slot = editingSlot,
                state = state,
                side = pickerSideFor(editingSlot, screenLandscape),
                config = config,
                onPick = { controller.assignMetric(editingSlot.key, it) },
                onReset = controller::resetLayout,
                onDismiss = controller::endEdit,
            )
        }

        RuntimeStateOverlay(
            state = state,
            config = config,
            controller = controller,
            modifier = Modifier.fillMaxSize(),
        )
        }
    }
}

/** 豎屏：頂欄 + 儀表 + 2×3 指標格 + 換組。 */
@Composable
private fun PortraitLayout(
    state: WorkoutUiState,
    config: GlassConfig = LocalGlassConfig.current,
    controller: WorkoutController,
    modifier: Modifier = Modifier,
    onSlotBounds: (String, Rect) -> Unit = { _, _ -> },
    speedDelta: Float? = null,
    speedDeltaWindow: String? = null,
) {
    // 見橫屏那邊的說明：正在拖的那一塊要整塊提起來，才壓得住同層的兄弟節點
    var dragZone by remember { mutableStateOf(DragZone.NONE) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(SECTION_GAP),
    ) {
        WorkoutHeader(state = state, config = config) {
            SessionActions(state = state, config = config, controller = controller)
        }
        WorkoutGauge(
            speed = state.currentSpeed,
            config = config,
            modifier = Modifier.fillMaxWidth(GAUGE_WIDTH_FRACTION).align(Alignment.CenterHorizontally),
            live = state.runtime.isLive,
            target = state.targetSpeed,
            delta = speedDelta,
            deltaWindow = speedDeltaWindow,
        )
        MetricGrid(
            slots = state.portraitSlots,
            state = state,
            config = config,
            onAdjust = controller::beginAdjust,
            onEdit = controller::beginEdit,
            // 參考稿的預設是 2 ROWS × 3 COLUMNS，但欄數現在由規格決定
            columns = state.portraitSpec.columns,
            density = state.portraitSpec.density,
            // 豎屏一格只有半屏寬，對比卡的兩條橫進度壓成兩根短線就看不出差了，
            // 所以讓它佔兩欄。橫屏的格子本來就寬，一格夠用（見 MetricGrid.wideCells）
            wideCells = true,
            editingLayout = state.editingLayout,
            alignStart = state.alignStart,
            onSwap = controller::swapSlots,
                    onRemove = controller::clearSlot,
            onDraggingChange = { dragZone = if (it) DragZone.PORTRAIT else DragZone.NONE },
            modifier = Modifier.zIndex(
                if (dragZone == DragZone.PORTRAIT) DRAG_ZONE_LAYER else 0f
            ),
            onSlotBounds = onSlotBounds,
        )
        MetricGroupSwitcher(
            groupIndex = state.groupIndex,
            config = config,
            onSwitch = controller::switchGroup,
            slotsPerGroup = state.portraitSlots.size,
        )
        ScenarioStrip(state = state, config = config, controller = controller)
        Spacer(Modifier.height(PAGE_PADDING))
    }
}

/**
 * 橫屏：左右兩欄側卡夾一個放大的儀表，底下是 2×4 dock。
 *
 * @param availableHeight 可用高度，用來決定 hero 儀表多大
 */
@Composable
private fun LandscapeLayout(
    state: WorkoutUiState,
    config: GlassConfig = LocalGlassConfig.current,
    controller: WorkoutController,
    availableHeight: Dp,
    modifier: Modifier = Modifier,
    onSlotBounds: (String, Rect) -> Unit = { _, _ -> },
    speedDelta: Float? = null,
    speedDeltaWindow: String? = null,
) {
    // 捲動容器裡量不到高度（約束是無限大），所以由外層把可用高度傳進來
    val heroHeight = (availableHeight * HERO_HEIGHT_FRACTION)
        .coerceIn(HERO_HEIGHT_MIN, HERO_HEIGHT_MAX)

    // 哪一塊正在拖。
    //
    // zIndex 只在同一個 parent 內比較，而側邊與 dock 是 Column 的兩個兄弟——
    // 從側邊拖一張卡到 dock 上時，它會被後畫的 dock 面板蓋住。把「正在拖的那一塊」
    // 整塊提起來才壓得住另一塊。
    var dragZone by remember { mutableStateOf(DragZone.NONE) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(SECTION_GAP),
    ) {
        WorkoutHeader(state = state, config = config, compact = true) {
            SessionActions(state = state, config = config, controller = controller)
        }
        // 側邊兩列的寬度**跟著 dock 的格子走**：整個橫屏版面的 item 要一樣寬，
        // 側卡原本用 weight 平分剩餘寬度，dock 一改欄數兩邊就對不齊了。
        // dock 是 flat 排法、格與格之間不留縫，所以每格寬 = (內容寬 − 兩側內距) / 欄數。
        //
        // 這個寬度是**每一側的總寬**，跟側邊自己選了幾欄無關：兩欄版不是把側邊
        // 總寬加倍（那會把儀表擠到只剩一條縫），是在同一塊寬度裡切成兩條窄欄。
        // 下限按欄數加倍，兩欄時每欄至少還有 SIDE_CELL_MIN 那麼寬。
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val sideCellWidth = ((maxWidth - DOCK_PADDING * 2) / state.dockSpec.columns)
                .coerceAtLeast(SIDE_CELL_MIN * state.sideSpec.columns)
            Row(
                // 儀表是正方形，寬度給多少就長多高。橫屏可用寬度遠大於高度，
                // 不封頂的話它會把底部 dock 與換組整個推出畫面
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heroHeight)
                    .zIndex(if (dragZone == DragZone.SIDE) DRAG_ZONE_LAYER else 0f),
                // 兩側等寬，SpaceBetween 就把儀表推到正中間；用 spacedBy 的話
                // 儀表得靠 weight 佔位，而 weight 會給它一個確定的**寬度**約束，
                // aspectRatio 便優先用寬度算邊長，正方形直接溢出這一列的高度。
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 側邊左右各一塊，中間留給 hero 儀表。每塊佔的欄數由 sideSpec 決定——
                // 兩欄時 MetricGrid 自己把上面那個總寬切成兩條窄欄，這裡不用另外算。
                MetricGrid(
                    slots = state.sideSlots.take(state.sideSpec.slotCount),
                    state = state,
                    config = config,
                    onAdjust = controller::beginAdjust,
                    onEdit = controller::beginEdit,
                    columns = state.sideSpec.columns,
                    // 密度跟著**格子的實際大小**走，不跟 dock 規格走：側邊分成四格之後
                    // 每格只有 ~54dp 高，緊湊檔的 17sp 讀數要 56dp 才裝得下，數值會被裁掉
                    // 下半截；兩欄時寬度再減半，字號還要再收一檔。dock 那邊的格子寬而扁，
                    // 兩者該用哪一檔本來就不是同一回事。
                    density = sideDensity(heroHeight, state.sideSpec.columns),
                    // 四格把儀表那一行的高度分完，和 dock 一樣是等高的。
                    // 要先扣掉格與格之間的間距再除：直接除的話總高會多出
                    // (列數 − 1) 份間距，側卡整欄比儀表高一截，把 dock 往下頂。
                    cellHeight = sideCellHeight(heroHeight),
                    modifier = Modifier.width(sideCellWidth),
                    onSlotBounds = onSlotBounds,
                    editingLayout = state.editingLayout,
                    alignStart = state.alignStart,
                    onSwap = controller::swapSlots,
                    onRemove = controller::clearSlot,
                    onDraggingChange = { dragZone = if (it) DragZone.SIDE else DragZone.NONE },
                )
                WorkoutGauge(
                    speed = state.currentSpeed,
                    config = config,
                    // 讓高度決定邊長：橫屏的可用寬度遠大於高度，按寬度算會撐爆
                    modifier = Modifier.fillMaxHeight(),
                    live = state.runtime.isLive,
                    target = state.targetSpeed,
                    delta = speedDelta,
                    deltaWindow = speedDeltaWindow,
                )
                MetricGrid(
                    slots = state.sideSlots.drop(state.sideSpec.slotCount),
                    state = state,
                    config = config,
                    onAdjust = controller::beginAdjust,
                    onEdit = controller::beginEdit,
                    columns = state.sideSpec.columns,
                    density = sideDensity(heroHeight, state.sideSpec.columns),
                    cellHeight = sideCellHeight(heroHeight),
                    modifier = Modifier.width(sideCellWidth),
                    onSlotBounds = onSlotBounds,
                    editingLayout = state.editingLayout,
                    alignStart = state.alignStart,
                    onSwap = controller::swapSlots,
                    onRemove = controller::clearSlot,
                    onDraggingChange = { dragZone = if (it) DragZone.SIDE else DragZone.NONE },
                )
            }
        }
        // dock 是**一整塊**面板，八個格子在裡面靠分隔線分開——參考稿的 .l-dock
        // 有自己的描邊、圓角與底，裡面的 .metric 是透明的
        val dockShape = RoundedCornerShape(DOCK_CORNER)
        val dockSurface = if (config.visualStyle == GlassVisualStyle.TACTILE) {
            Modifier.tactilePanel(DOCK_CORNER)
        } else {
            Modifier
                // 用帶形狀的 background 而不是 clip + background：clip 會把子內容裁掉，
                // dock 裡的卡片就拖不出這塊面板的邊界。
                .background(config.panelColor, dockShape)
                .border(1.dp, config.lineColor, dockShape)
        }
        Box(
            modifier = Modifier
                .zIndex(if (dragZone == DragZone.DOCK) DRAG_ZONE_LAYER else 0f)
                .then(dockSurface)
                .padding(DOCK_PADDING)
        ) {
            MetricGrid(
                slots = state.dockSlots,
                state = state,
                config = config,
                onAdjust = controller::beginAdjust,
                onEdit = controller::beginEdit,
                columns = state.dockSpec.columns,
                density = state.dockSpec.density,
                // dock 是**等高網格**：每列的高度必須一樣，否則分隔線按等分算出來的
                // 位置就落不到真正的分界上——那條橫線會貼在第一列的數值下面。
                // 讓格子由內容撐開時，帶 ± 的阻力格與只有讀數的格子高度並不相同。
                cellHeight = dockCellHeight(state.dockSpec.density),
                editingLayout = state.editingLayout,
                alignStart = state.alignStart,
                onSwap = controller::swapSlots,
                    onRemove = controller::clearSlot,
                onSlotBounds = onSlotBounds,
                onDraggingChange = { dragZone = if (it) DragZone.DOCK else DragZone.NONE },
                flat = true,
            )
        }
        MetricGroupSwitcher(
            groupIndex = state.groupIndex,
            config = config,
            onSwitch = controller::switchGroup,
        )
        ScenarioStrip(state = state, config = config, controller = controller)
        Spacer(Modifier.height(PAGE_PADDING))
    }
}

@Composable
private fun MatrixTopBar(
    onBack: () -> Unit,
    landscape: Boolean,
    onToggleOrientation: () -> Unit,
    palette: ThemePalette,
    onPaletteChange: ((ThemePalette) -> Unit)?,
    portraitSpec: MetricGridSpec,
    dockSpec: MetricGridSpec,
    sideSpec: MetricGridSpec,
    alignStart: Boolean,
    editingLayout: Boolean,
    onSelectSpec: (MetricGridSpec, Boolean) -> Unit,
    onSelectSideSpec: (MetricGridSpec) -> Unit,
    onToggleAlignStart: () -> Unit,
    onToggleEditing: () -> Unit,
    config: GlassConfig = LocalGlassConfig.current,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.matrix_back_desc),
                // inkColor 是畫布上的字色，壓在屏內底色上對比不夠——返回鍵要跟
                // 屏內的內容色走，才和背景拉開
                tint = config.screenContentColor,
            )
        }
        Text(
            text = stringResource(R.string.matrix_title),
            color = config.screenContentColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.weight(1f))
        OrientationToggle(
            landscape = landscape,
            onToggle = onToggleOrientation,
            config = config,
        )
        LayoutMenuButton(
            landscape = landscape,
            current = if (landscape) dockSpec else portraitSpec,
            editingLayout = editingLayout,
            onSelectSpec = { onSelectSpec(it, !landscape) },
            onToggleEditing = onToggleEditing,
            config = config,
            // 側邊只在橫屏有意義；豎屏傳 null，選單就不畫那一段
            sideSpec = if (landscape) sideSpec else null,
            onSelectSideSpec = onSelectSideSpec,
            alignStart = alignStart,
            onToggleAlignStart = onToggleAlignStart,
        )
        // 只在使用端接了 onPaletteChange 時才出現：這顆是驗收入口，
        // 沒有換配色的能力就不該畫一顆點不動的按鈕
        if (onPaletteChange != null) {
            PaletteMenuButton(
                current = palette,
                onSelect = onPaletteChange,
                config = config,
            )
        }
    }
}

/**
 * 場景切換條。
 *
 * 五種執行狀態每一種都要能重複進入與退出，靠真機湊不出來（斷線、裝置拒絕
 * 都不是想觸發就觸發的），所以留一排開關直接切。
 *
 * 它跟著內容一起捲，不固定在底部：手機橫屏只有 ~457dp 高，固定住的話兩排晶片
 * 會把主體壓成一條縫。
 */
@Composable
private fun ScenarioStrip(
    state: WorkoutUiState,
    config: GlassConfig = LocalGlassConfig.current,
    controller: WorkoutController,
) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.matrix_scenario),
            color = config.mutedContentColor,
            fontSize = 11.sp,
        )
        Row(
            // 五顆晶片在窄機身上排不下，讓它捲，而不是擠成兩行
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            RuntimeChip(R.string.workout_status_active, state.runtime == WorkoutRuntimeState.ACTIVE, config) {
                controller.activate()
            }
            RuntimeChip(R.string.workout_status_paused, state.runtime == WorkoutRuntimeState.PAUSED, config) {
                controller.pause()
            }
            RuntimeChip(
                R.string.workout_status_disconnected,
                state.runtime == WorkoutRuntimeState.DISCONNECTED,
                config,
            ) { controller.disconnect() }
            RuntimeChip(
                R.string.workout_status_reconnecting,
                state.runtime == WorkoutRuntimeState.RECONNECTING,
                config,
            ) { controller.reconnecting() }
            RuntimeChip(R.string.workout_status_ended, state.runtime == WorkoutRuntimeState.ENDED, config) {
                controller.end()
            }
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassFilterChip(
                label = stringResource(R.string.matrix_scenario_read_only),
                selected = state.device.readOnly,
                // 只改自己那一個欄位。整個換掉 device 的話，幾顆場景開關會互相
                // 重置——先開「無心率帶」再開「只讀」，前一顆就被打回去了。
                onClick = { controller.useDevice(state.device.copy(readOnly = !state.device.readOnly)) },
                config = config,
            )
            // 阻力沒有電控的機器：那一格寫「手動」而不是一個數字
            GlassFilterChip(
                label = stringResource(R.string.scenario_manual_resistance),
                selected = state.device.controllableMetricIds.isEmpty() && !state.device.readOnly,
                onClick = {
                    controller.useDevice(
                        state.device.copy(
                            controllableMetricIds = if (state.device.controllableMetricIds.isEmpty()) {
                                WorkoutDeviceCapability.IndoorBike.controllableMetricIds
                            } else {
                                emptySet()
                            }
                        )
                    )
                },
                config = config,
            )
            // 心率這類要外接感測器的指標，沒配對上就沒有數據——那一格要顯示成
            // 一條橫線，不是 0。這顆開關讓那一態能反覆驗收。
            GlassFilterChip(
                label = stringResource(R.string.scenario_no_sensor),
                selected = !state.device.sensorConnected,
                onClick = {
                    controller.useDevice(
                        state.device.copy(sensorConnected = !state.device.sensorConnected)
                    )
                },
                config = config,
            )
            Text(
                text = stringResource(R.string.matrix_scenario_write),
                color = config.mutedContentColor,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 8.dp, end = 2.dp),
            )
            // 失敗與拒絕在真機上碰不到，但兩種 Toast 都要能反覆驗收
            WriteOutcome.entries.forEach { outcome ->
                GlassFilterChip(
                    label = stringResource(outcome.labelRes),
                    selected = controller.simulatedOutcome == outcome,
                    onClick = { controller.simulatedOutcome = outcome },
                    config = config,
                )
            }
        }
    }
}

@Composable
private fun RuntimeChip(
    labelRes: Int,
    selected: Boolean,
    config: GlassConfig = LocalGlassConfig.current,
    onClick: () -> Unit,
) {
    GlassFilterChip(
        label = stringResource(labelRes),
        selected = selected,
        onClick = onClick,
        config = config,
    )
}

/**
 * 抽屜從哪一側進。
 *
 * 豎屏一律底部。橫屏看資料位在左半還是右半——側卡 0/1 在左、2/3 在右，
 * dock 每列四格，前兩格算左。
 *
 * @param slot 正在編輯的資料位
 * @param landscape 當前是不是橫屏排法
 */
private fun pickerSideFor(slot: MetricSlot, landscape: Boolean): PickerSide {
    if (!landscape) return PickerSide.BOTTOM
    val index = slot.key.substringAfterLast('-').toIntOrNull() ?: 0
    val onRight = when {
        slot.key.startsWith("land-side-") -> index >= 2
        slot.key.startsWith("land-dock-") -> index % 4 >= 2
        else -> false
    }
    return if (onRight) PickerSide.LEFT else PickerSide.RIGHT
}

/**
 * 調節浮層的落點。
 *
 * 貼著被調節的那張卡彈在**它上方**；上面放不下就翻到卡片下面。水平以卡片中心
 * 對齊，再夾回容器內——靠邊的格子否則會把浮層推出畫面。
 *
 * @param anchor 卡片在根座標系的位置；還沒量到時退回底部置中
 * @param origin 容器在根座標系的原點
 * @param panel 浮層量到的尺寸；第一幀是 0，那一幀先按底部置中擺
 * @param boxWidth 容器寬（px）
 * @param boxHeight 容器高（px）
 * @param gap 浮層與卡片之間的間距（px）
 */
private fun focusPanelOffset(
    anchor: Rect?,
    origin: Offset,
    panel: IntSize,
    boxWidth: Float,
    boxHeight: Float,
    gap: Float,
): IntOffset {
    if (anchor == null || panel.height == 0) {
        return IntOffset(
            x = ((boxWidth - panel.width) / 2f).roundToInt(),
            y = (boxHeight - panel.height - gap).coerceAtLeast(0f).roundToInt(),
        )
    }
    val above = anchor.top - origin.y - panel.height - gap
    val below = anchor.bottom - origin.y + gap
    val y = if (above >= 0f) above else below.coerceAtMost(boxHeight - panel.height)
    val x = (anchor.center.x - origin.x - panel.width / 2f)
        .coerceIn(0f, (boxWidth - panel.width).coerceAtLeast(0f))
    return IntOffset(x.roundToInt(), y.coerceAtLeast(0f).roundToInt())
}

private val WriteOutcome.labelRes: Int
    get() = when (this) {
        WriteOutcome.SUCCESS -> R.string.matrix_write_success
        WriteOutcome.FAILED -> R.string.matrix_write_failed
        WriteOutcome.REJECTED -> R.string.matrix_write_rejected
    }

/*
 * 參考稿在儀表底下還有一條訓練階段條（`.phase-mini`，放松↔沖刺）。那條沒有做：
 * 一根光禿禿的線加兩個端點標籤，在畫面上讀不出它在表達什麼，而且沒有資料源。
 * 要它的話得先有「這次訓練分幾個階段、現在在第幾段」這組資料，再連標籤一起設計。
 */

/** 儀表讀的是「當前速度」，量程 0–30 km/h。 */
internal val WorkoutUiState.currentSpeed: Float
    get() = MetricCatalog.find("speed")?.let { valueOf(it).toFloatOrNull() } ?: 0f

/** 目標速度只有裝置支援調節時才畫標記。 */
private val WorkoutUiState.targetSpeed: Float?
    get() = MetricCatalog.find("target-speed")
        ?.takeIf { device.isControllable(it.id) }
        ?.let { valueOf(it).toFloatOrNull() }

private val PAGE_PADDING = 16.dp
/**
 * 側邊每一格的高度。
 *
 * 整欄要正好填滿儀表那一行：先扣掉 (列數 − 1) 份間距，再按列數等分。
 *
 * @param heroHeight 儀表那一行的高度
 */
private fun sideCellHeight(heroHeight: Dp): Dp {
    val gaps = SIDE_CELL_SPACING * (MetricSlots.SIDE_ROWS - 1)
    return ((heroHeight - gaps) / MetricSlots.SIDE_ROWS).coerceAtLeast(SIDE_CELL_MIN_HEIGHT)
}

/**
 * 側邊卡的密度檔。
 *
 * 按格子**算出來的高度**選，而不是照抄 dock 的規格：側邊是一欄四格的窄高卡、
 * dock 是一排寬扁卡，同一個規格對兩邊意味著完全不同的可用空間。
 *
 * 閾值留了餘量（各檔內容高度分別約 62 / 56 / 40dp）：卡到剛好會在字體縮放
 * 開大的機器上又裂開一次。
 *
 * 兩欄時強制至少收到緊湊檔：欄數翻倍等於寬度砍半，光看高度算出來的常規檔
 * 塞不下兩欄——常規檔的字號是照單欄的寬度調的。
 *
 * @param heroHeight 儀表那一行的高度
 * @param columns 側邊當前選了幾欄
 */
private fun sideDensity(heroHeight: Dp, columns: Int): MetricCardDensity {
    val cell = sideCellHeight(heroHeight)
    val byHeight = when {
        cell >= SIDE_REGULAR_MIN -> MetricCardDensity.REGULAR
        cell >= SIDE_COMPACT_MIN -> MetricCardDensity.COMPACT
        else -> MetricCardDensity.DENSE
    }
    return if (columns >= 2 && byHeight == MetricCardDensity.REGULAR) {
        MetricCardDensity.COMPACT
    } else {
        byHeight
    }
}

/** 各密度檔要求的最小格高。 */
private val SIDE_REGULAR_MIN = 76.dp
private val SIDE_COMPACT_MIN = 62.dp

/** 側邊格與格之間的間距，跟 [MetricGrid] 的預設值一致。 */
private val SIDE_CELL_SPACING = 8.dp

/** 再矮就裝不下「標籤 + 讀數」兩行了。 */
private val SIDE_CELL_MIN_HEIGHT = 44.dp

private val SECTION_GAP = 12.dp

/** 儀表在豎屏佔的寬度比例，兩側留給呼吸空間。 */
/**
 * 儀表佔內容寬的比例。
 *
 * 參考稿的錶盤 238 配內容寬 270（屏寬 300 減兩側 15 的內距），也就是 0.88。
 * 之前給 0.72，錶盤在一排卡片旁邊明顯縮了一圈。
 */
/** 側卡寬度的下限：dock 開到 5 欄時每格很窄，再窄下去讀數就放不下了。 */
private val SIDE_CELL_MIN = 96.dp

private const val GAUGE_WIDTH_FRACTION = 238f / 270f

/** 參考稿 .l-dock：圓角 15、內距 5。 */
private val DOCK_CORNER = 15.dp
private val DOCK_PADDING = 5.dp

/** 橫屏 hero 區佔可用高度的比例，儀表以這個高度為邊長。 */
/**
 * 儀表那一行佔可用高度的比例，與它的上下限。
 *
 * 這個值卡在兩個約束之間，手機橫屏（~359dp 高）上兩者放不進同一屏：
 * - 側邊一欄四格，每格裝得下「標籤 + 讀數」最少要 50dp（最密檔，還得算上系統
 *   字體放大），四格連間距共 224dp。這一行給不到 240dp，讀數就被裁掉下半截
 * - dock 想在首屏露出第一行，需要 顶欄 48 + 間距 24 + 64dp
 *
 * 現在按前者取值（0.66 → 儀表約 240dp）：讀數被裁是硬缺陷，dock 捲一下能看到。
 * 要讓 dock 在首屏就露出來，得把側邊收回三格。
 */
private const val HERO_HEIGHT_FRACTION = 0.66f
private val HERO_HEIGHT_MIN = 240.dp
private val HERO_HEIGHT_MAX = 380.dp

/** 內容最大寬度：參考稿是手機版面，平板上不該把它拉開。 */
private val CONTENT_MAX_WIDTH = 1000.dp

/** Toast 貼在內容區頂部，讓開下半屏的調節浮層。 */
private val TOAST_TOP_PADDING = 8.dp

/** 超過這個寬度就按橫屏排。 */
private val LANDSCAPE_MIN_WIDTH = 600.dp

/** 調節浮層開著時，主體退到背後的不透明度。參考稿 `.phone.active` 是 .42。 */
private const val BACKDROP_DIM = 0.42f

/**
 * 哪一塊網格正在被拖。
 *
 * `zIndex` 只在同一個 parent 內比較，而幾塊網格是 Column 的兄弟節點——不把正在拖的
 * 那一塊整塊提起來，卡片拖過邊界就會被後畫的兄弟蓋住。
 */
private enum class DragZone { NONE, PORTRAIT, SIDE, DOCK }

/** 正在拖的那一塊提到同層最上面。 */
private const val DRAG_ZONE_LAYER = 1f

/**
 * dock 每格的高度。
 *
 * 固定值而不是由內容撐開：dock 的分隔線是按等分畫的，格子高度不一致的話那條橫線
 * 就落不到真正的分界上。內容結構是固定的（標籤 + 讀數），給一個夠放的高度即可。
 *
 * @param density 卡片密度檔
 */
private fun dockCellHeight(density: MetricCardDensity): Dp =
    density.pick(DOCK_CELL_REGULAR, DOCK_CELL_COMPACT, DOCK_CELL_DENSE)

private val DOCK_CELL_REGULAR = 72.dp
private val DOCK_CELL_COMPACT = 64.dp
private val DOCK_CELL_DENSE = 56.dp

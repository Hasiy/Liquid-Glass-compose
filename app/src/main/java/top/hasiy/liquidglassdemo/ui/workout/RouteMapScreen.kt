package top.hasiyliquidglassdemo.ui.workout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.hasiy.designsystem.DesignSystemTheme
import top.hasiy.designsystem.GlassConfig
import top.hasiy.designsystem.LocalGlassConfig
import top.hasiy.designsystem.accentToneColor
import top.hasiy.designsystem.lineColor
import top.hasiy.designsystem.mutedContentColor
import top.hasiy.designsystem.onAccentColor
import top.hasiy.designsystem.panelColor
import top.hasiy.designsystem.screenColor
import top.hasiy.designsystem.screenContentColor
import top.hasiy.designsystem.sunkColor
import top.hasiy.designsystem.tactilePanel
import top.hasiy.designsystem.tactileScreenTexture
import top.hasiy.designsystem.tokens.GlassVisualStyle
import top.hasiy.designsystem.tokens.ThemePalette
import top.hasiyliquidglassdemo.R

/** 這一頁的兩種顯示模式。 */
enum class RouteMapMode {
    /** 虛擬路線：開放折線，報剩餘距離。 */
    ROUTE,

    /** F1 賽道：閉合環路，報圈數與分段。 */
    CIRCUIT,
}

/**
 * 路線與賽道地圖。**平板專屬的顯示模式**，不與其它兩頁共用版面。
 *
 * 對應參考稿 09。骨架是五段豎排：
 * ```
 * 頂欄 / 兩表夾地圖 / 目標調節條 / 分段計時（僅賽道）/ 會話讀數
 * ```
 *
 * 跟雙表儀表舱（[DualGaugeClusterScreen]）的差別不只中間那一塊：09 把目標值的調節
 * 從表下方移到了獨立一條，表下方只留一顆唸得出來的目標胶囊，會話讀數則從中間
 * 挪到了底部鋪成 2×4。中間換一塊內容順帶把另外三段全改了，所以兩頁各寫一份骨架，
 * 只共用組件（[TargetGauge] / [TargetChip] / [TargetTuneBar]）。
 *
 * 兩種模式二選一，不是兩頁：它們的骨架完全一樣，差別在中間那張圖是開放折線還是
 * 閉合環路、底下報的是剩餘距離還是圈速。合在一頁還能讓使用者直接對比。
 *
 * @param palette 當前主題色
 * @param onBack 返回上一頁
 * @param modifier 外部修飾符
 * @param controller 狀態持有者
 * @param onPaletteChange 換主題色；不接就不畫那顆按鈕
 */
@Composable
fun RouteMapScreen(
    palette: ThemePalette,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    // 同雙表儀表舱：兩塊表分別調速度與阻力
    controller: WorkoutController = rememberWorkoutController(
        device = WorkoutDeviceCapability.DualTargetBike,
    ),
    onPaletteChange: ((ThemePalette) -> Unit)? = null,
) {
    DesignSystemTheme(palette = palette) {
        val config = LocalGlassConfig.current
        val state = controller.uiState

        BackHandler(enabled = state.hasOverlay || state.editingLayout) {
            when {
                state.editingSlotKey != null -> controller.endEdit()
                state.adjustingSlotKey != null -> controller.endAdjust()
                else -> controller.setLayoutEditing(false)
            }
        }

        var mode by rememberSaveable { mutableStateOf(RouteMapMode.ROUTE) }
        var speedTarget by rememberSaveable { mutableFloatStateOf(ROUTE_SPEED_TARGET) }
        var resistanceTarget by rememberSaveable { mutableFloatStateOf(ROUTE_RESISTANCE_TARGET) }

        val circuit = mode == RouteMapMode.CIRCUIT
        val speedState = gaugeStateOf(
            state.currentSpeed,
            speedTarget,
            !state.device.isControllable("target-speed"),
        )
        val resistanceState = gaugeStateOf(
            state.currentResistance,
            resistanceTarget,
            !state.device.isControllable("resistance"),
        )

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(config.screenColor)
                .tactileScreenTexture(config.visualStyle == GlassVisualStyle.TACTILE),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = ROUTE_PAGE_PADDING, vertical = ROUTE_PAGE_PADDING_V),
                verticalArrangement = Arrangement.spacedBy(ROUTE_SECTION_GAP),
            ) {
                RouteTopBar(
                    state = state,
                    config = config,
                    controller = controller,
                    palette = palette,
                    mode = mode,
                    onModeChange = { mode = it },
                    onBack = onBack,
                    onPaletteChange = onPaletteChange,
                )

                // 兩塊表夾一張圖。寬度按參考稿的 `204px 1fr 204px`——地圖是表的兩倍寬，
                // 壓窄了路線就只是一團線，讀不出走向。
                //
                // 三塊**等高**，高度統一算一次：參考稿裡兩塊表和地圖畫布都是 204，
                // 是同一條水平帶。各自按自己那一欄的寬度算的話，表會長到 300 多把
                // 地圖壓成一條扁縫——表那一欄的寬度跟它該有多大沒有關係。
                //
                // 上限跟著屏寬走而不是寫死 204dp：參考稿的 204 是 866 屏寬下的值，
                // 換到更寬的平板上寫死就顯得太小。
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                ) {
                    // 下限只保證不是負數，**不設一個寫死的最小值**：行高不夠時，
                    // 硬撐一個 120dp 的下限只會讓這一帶溢出去壓在調節條和頂欄上，
                    // 那是「壞得看不出原因」的那種壞。寧可表小一點，版面先站得住。
                    val bayHeight = minOf(
                        maxHeight - CHIP_BLOCK,
                        maxWidth * BAY_HEIGHT_RATIO,
                    ).coerceIn(0.dp, maxHeight)
                Row(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                    horizontalArrangement = Arrangement.spacedBy(ROUTE_SECTION_GAP),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GaugeWithChip(
                        label = stringResource(R.string.metric_speed),
                        current = state.currentSpeed,
                        target = speedTarget,
                        max = ROUTE_SPEED_MAX,
                        majorStep = ROUTE_SPEED_MAJOR,
                        unit = stringResource(R.string.unit_kmh),
                        decimals = 1,
                        gaugeState = speedState,
                        side = bayHeight,
                        config = config,
                        modifier = Modifier.weight(1f),
                    )

                    MapStage(
                        mode = mode,
                        config = config,
                        modifier = Modifier.weight(MAP_WEIGHT).height(bayHeight),
                    )

                    GaugeWithChip(
                        label = stringResource(R.string.metric_resistance),
                        current = state.currentResistance,
                        target = resistanceTarget,
                        max = ROUTE_RESISTANCE_MAX,
                        majorStep = ROUTE_RESISTANCE_MAJOR,
                        unit = stringResource(R.string.unit_level),
                        decimals = 0,
                        gaugeState = resistanceState,
                        side = bayHeight,
                        config = config,
                        modifier = Modifier.weight(1f),
                    )
                }
                }

                TargetTuneBar(
                    config = config,
                    left = {
                        TuneUnit(
                            label = stringResource(R.string.tune_target_speed),
                            value = speedTarget,
                            unit = stringResource(R.string.unit_kmh),
                            decimals = 1,
                            state = speedState,
                            onStep = { dir ->
                                speedTarget =
                                    (speedTarget + dir * ROUTE_SPEED_STEP).coerceIn(0f, ROUTE_SPEED_MAX)
                            },
                            stepDownLabel = stringResource(R.string.focus_step_down),
                            stepUpLabel = stringResource(R.string.focus_step_up),
                            config = config,
                        )
                    },
                    center = {
                        // 兩種模式報的不是同一件事：開放路線報整段計劃走了多少，
                        // 閉環賽道報本圈走了多少加全程幾圈。
                        if (circuit) {
                            TuneProgress(
                                fraction = CIRCUIT_LAP_PROGRESS,
                                startText = stringResource(
                                    R.string.circuit_lap_progress,
                                    (CIRCUIT_LAP_PROGRESS * 100).toInt(),
                                ),
                                endText = stringResource(R.string.circuit_lap_total, CIRCUIT_LAP_TOTAL),
                                config = config,
                            )
                        } else {
                            TuneProgress(
                                fraction = ROUTE_PLAN_FRACTION,
                                startText = stringResource(R.string.cluster_plan_done, ROUTE_PLAN_DONE),
                                endText = stringResource(R.string.cluster_plan_total, ROUTE_PLAN_TOTAL),
                                config = config,
                            )
                        }
                    },
                    right = {
                        TuneUnit(
                            label = stringResource(R.string.tune_target_resistance),
                            value = resistanceTarget,
                            unit = stringResource(R.string.unit_level),
                            decimals = 0,
                            state = resistanceState,
                            onStep = { dir ->
                                resistanceTarget = (resistanceTarget + dir * ROUTE_RESISTANCE_STEP)
                                    .coerceIn(0f, ROUTE_RESISTANCE_MAX)
                            },
                            stepDownLabel = stringResource(R.string.focus_step_down),
                            stepUpLabel = stringResource(R.string.focus_step_up),
                            config = config,
                        )
                    },
                )

                if (circuit) {
                    SectorRow(splits = CIRCUIT_SPLITS, config = config)
                    SectorLegend(config = config)
                }

                // 底部會話讀數。走 [MetricGrid] 而不是自己排一遍 Text：這樣它就跟
                // 其它頁面的格子一樣可以拖著換位、可以移除，編輯模式對整個 App 是
                // 同一套操作。
                //
                // 兩種模式各一族資料位：路線模式八格報會話讀數，賽道模式四格報圈速，
                // 各自記得自己的樣子，切模式不會把對方的版面截掉。
                MetricGrid(
                    slots = if (circuit) MetricSlots.CircuitBay else state.baySlots,
                    state = state,
                    config = config,
                    // 這一排是**讀數**，參考稿的 `.metric` 沒有調節交互——那兩塊表下面
                    // 已經有專門的調節條了，同一個目標值給兩個入口只會讓人以為是兩件事。
                    onAdjust = {},
                    onEdit = controller::beginEdit,
                    columns = BAY_COLUMNS,
                    // 最密那一檔：參考稿的 `.metric` 是一張很扁的卡（內距 5×9、
                    // 標籤 8px、讀數 16px，兩行共約 41px）。緊湊檔的 17px 讀數配上
                    // 7dp 內距要 52dp 高，兩行就吃掉下半屏，把兩塊表壓小了。
                    //
                    // 高度不另外定死：定死了讀數會被裁掉一半——卡片內容需要多高
                    // 由字號決定，該調的是字號那一檔。
                    density = MetricCardDensity.DENSE,
                    editingLayout = state.editingLayout,
                    onSwap = controller::swapSlots,
                    onRemove = controller::clearSlot,
                )
            }

            val editingSlot = state.editingSlot
            if (editingSlot != null) {
                // 從左側進：這一排格子橫跨整個底部，底部抽屜會把它整排蓋住，
                // 看不到自己在改哪一格。
                MetricPicker(
                    slot = editingSlot,
                    state = state,
                    side = PickerSide.LEFT,
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

/**
 * 一塊表加它下方的目標胶囊。
 *
 * 邊長由外面算好傳進來（[side]）：三塊等高這件事只能在同一層決定，
 * 每塊自己按自己那一欄的寬度算就不會等高了。
 *
 * @param side 表的邊長
 */
@Composable
private fun GaugeWithChip(
    label: String,
    current: Float,
    target: Float,
    max: Float,
    majorStep: Float,
    unit: String,
    decimals: Int,
    gaugeState: GaugeState,
    side: Dp,
    config: GlassConfig,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CHIP_GAP),
        ) {
            TargetGauge(
                label = label,
                current = current,
                target = target,
                max = max,
                majorStep = majorStep,
                unit = unit,
                decimals = decimals,
                readOnly = gaugeState == GaugeState.READ_ONLY,
                config = config,
                modifier = Modifier.size(side),
            )
            TargetChip(
                value = target,
                decimals = decimals,
                state = gaugeState,
                config = config,
            )
        }
    }
}

/** 地圖那一塊：抬頭、畫布、腳註三段。 */
@Composable
private fun MapStage(mode: RouteMapMode, config: GlassConfig, modifier: Modifier = Modifier) {
    val circuit = mode == RouteMapMode.CIRCUIT
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MAP_ROW_GAP),
    ) {
        // 抬頭與腳註都是**兩端對齊**：左邊說這是什麼，右邊說是哪一條。
        // 擠在一起的話兩個層級的文字會讀成一句話。
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = stringResource(
                    if (circuit) R.string.route_mode_circuit else R.string.route_mode_route
                ),
                color = config.mutedContentColor,
                fontSize = MAP_CAPTION_SIZE,
                modifier = Modifier.alignByBaseline(),
            )
            Text(
                text = if (circuit) {
                    stringResource(R.string.circuit_name, CIRCUIT_LAP_KM)
                } else {
                    stringResource(R.string.route_name, ROUTE_TOTAL_KM)
                },
                color = config.screenContentColor,
                fontSize = MAP_TITLE_SIZE,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.alignByBaseline(),
            )
        }

        // 畫布有自己的底與描邊（參考稿 `.map-canvas`），比面板更沉一檔——
        // 地圖是「看進去」的東西，不是浮在上面的卡片。
        val canvasShape = RoundedCornerShape(MAP_CANVAS_CORNER)
        val canvasSurface = if (config.visualStyle == GlassVisualStyle.TACTILE) {
            Modifier.tactilePanel(MAP_CANVAS_CORNER)
        } else {
            Modifier
                .clip(canvasShape)
                .background(config.sunkColor)
                .border(1.dp, config.lineColor, canvasShape)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .then(canvasSurface),
        ) {
            if (circuit) {
                CircuitMap(progress = CIRCUIT_LAP_PROGRESS, config = config)
            } else {
                RouteMap(progress = ROUTE_PROGRESS, config = config)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            // 閉環上必須報圈數：一個點在環上轉，不報圈數看不出跑了多久。
            Text(
                text = if (circuit) {
                    stringResource(
                        R.string.circuit_lap,
                        CIRCUIT_LAP_INDEX,
                        (CIRCUIT_LAP_PROGRESS * 100).toInt(),
                    )
                } else {
                    stringResource(R.string.route_done, ROUTE_DONE_KM)
                },
                color = config.screenContentColor,
                fontSize = MAP_FOOT_SIZE,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.alignByBaseline(),
            )
            Text(
                text = if (circuit) {
                    stringResource(R.string.circuit_best_lap, CIRCUIT_BEST_LAP)
                } else {
                    stringResource(R.string.route_left, ROUTE_LEFT_KM)
                },
                color = config.mutedContentColor,
                fontSize = MAP_CAPTION_SIZE,
                modifier = Modifier.alignByBaseline(),
            )
        }
    }
}

/** 頂欄：共用的那一條，右側掛這一頁自己的操作。 */
@Composable
private fun RouteTopBar(
    state: WorkoutUiState,
    config: GlassConfig,
    controller: WorkoutController,
    palette: ThemePalette,
    mode: RouteMapMode,
    onModeChange: (RouteMapMode) -> Unit,
    onBack: () -> Unit,
    onPaletteChange: ((ThemePalette) -> Unit)?,
) {
    ClusterHeader(state = state, onBack = onBack, config = config) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RouteModeSwitch(mode = mode, onModeChange = onModeChange, config = config)
            LayoutEditToggle(
                editing = state.editingLayout,
                onToggle = { controller.setLayoutEditing(!state.editingLayout) },
                config = config,
            )
            if (onPaletteChange != null) {
                PaletteMenuButton(current = palette, onSelect = onPaletteChange, config = config)
            }
            SessionActions(state = state, config = config, controller = controller)
        }
    }
}

/**
 * 路線 / 賽道的切換。
 *
 * 兩顆胶囊而不是下拉菜單：只有兩個選項，而且切過去長什麼樣直接看得見，
 * 藏進菜單裡反而要點兩次才知道。
 */
@Composable
private fun RouteModeSwitch(
    mode: RouteMapMode,
    onModeChange: (RouteMapMode) -> Unit,
    config: GlassConfig,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        RouteMapMode.entries.forEach { candidate ->
            val selected = candidate == mode
            val shape = RoundedCornerShape(MODE_CHIP_CORNER)
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(if (selected) config.accentToneColor else config.panelColor)
                    .tactileClickable(
                        onClickLabel = stringResource(R.string.route_mode_switch_desc),
                        onClick = { onModeChange(candidate) },
                    )
                    .padding(horizontal = MODE_CHIP_PADDING_H, vertical = MODE_CHIP_PADDING_V),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(
                        if (candidate == RouteMapMode.CIRCUIT) R.string.route_mode_circuit
                        else R.string.route_mode_route
                    ),
                    color = if (selected) config.onAccentColor else config.mutedContentColor,
                    fontSize = MODE_CHIP_SIZE,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/**
 * 這一頁的示例路線、賽道與目標值。
 *
 * 接真機時路線來自賽道庫、進度由累計里程算，圈速由每次過起跑線的時間差算，
 * 目標值來自 FTMS 的目標值特徵。
 */
private const val ROUTE_SPEED_TARGET = 28f
private const val ROUTE_RESISTANCE_TARGET = 11f
private const val ROUTE_PROGRESS = 0.62f
private const val ROUTE_TOTAL_KM = "10.0"
private const val ROUTE_DONE_KM = "6.24"
private const val ROUTE_LEFT_KM = "3.76"
private const val ROUTE_PLAN_FRACTION = 0.62f
private const val ROUTE_PLAN_DONE = "18:42"
private const val ROUTE_PLAN_TOTAL = "30:00"

private const val CIRCUIT_LAP_KM = "5.2"
private const val CIRCUIT_LAP_PROGRESS = 0.43f
private const val CIRCUIT_LAP_INDEX = 3
private const val CIRCUIT_LAP_TOTAL = 4
private const val CIRCUIT_BEST_LAP = "7:42"

/** 參考稿賽道模式的三段成績：S1 破紀錄、S2 本次最佳且正在跑、S3 沒刷新。 */
private val CIRCUIT_SPLITS = listOf(
    SectorSplit("S1", "0:36.9", "0:37.4", "0:38.9", SectorGrade.RECORD),
    SectorSplit("S2", "1:12.6", "1:11.8", "1:13.4", SectorGrade.BEST, current = true),
    SectorSplit("S3", "0:52.1", "0:51.4", "0:52.8", SectorGrade.PLAIN),
)

private const val ROUTE_SPEED_MAX = 30f
private const val ROUTE_SPEED_MAJOR = 5f
private const val ROUTE_SPEED_STEP = 0.5f
private const val ROUTE_RESISTANCE_MAX = 16f
private const val ROUTE_RESISTANCE_MAJOR = 2f
private const val ROUTE_RESISTANCE_STEP = 1f

/** 地圖是表的兩倍寬：參考稿 `204px 1fr 204px` 於 834 的內容寬，中間約 398。 */
private const val MAP_WEIGHT = 1.95f

/**
 * 表與地圖那一帶的高度上限，佔屏寬的比例。
 *
 * 參考稿在 866 的屏寬下給了 204，即 23.5%。
 */
private const val BAY_HEIGHT_RATIO = 0.235f

/** 底部那一排是 4 欄：八格排成 2×4，賽道模式的四格排成一列。 */
private const val BAY_COLUMNS = 4


private val ROUTE_PAGE_PADDING = 16.dp
private val ROUTE_PAGE_PADDING_V = 10.dp
private val ROUTE_SECTION_GAP = 7.dp
private val MAP_ROW_GAP = 6.dp
private val MAP_CANVAS_CORNER = 14.dp
private val MAP_CAPTION_SIZE = 8.sp
private val MAP_TITLE_SIZE = 11.sp
private val MAP_FOOT_SIZE = 12.sp

/** 表下方那顆胶囊占的高度（參考稿 `padding-bottom: 34px`）。 */
private val CHIP_BLOCK = 34.dp
private val CHIP_GAP = 8.dp

private val MODE_CHIP_CORNER = 99.dp
private val MODE_CHIP_PADDING_H = 9.dp
private val MODE_CHIP_PADDING_V = 4.dp
private val MODE_CHIP_SIZE = 8.sp

package top.hasiyliquidglassdemo.ui.workout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import top.hasiy.designsystem.mutedContentColor
import top.hasiy.designsystem.screenColor
import top.hasiy.designsystem.screenContentColor
import top.hasiy.designsystem.tactileScreenTexture
import top.hasiy.designsystem.tokens.GlassVisualStyle
import top.hasiy.designsystem.tokens.ThemePalette
import top.hasiy.designsystem.trackColor
import top.hasiyliquidglassdemo.R

/**
 * 雙表儀表舱。**平板專屬的顯示模式**，不與響應式狀態矩陣共用版面。
 *
 * 對應參考稿 08。照搬汽車組合儀表的分工：
 * ```
 * 左表 = 速度（車速表）   中間 = 本次運動的會話資訊（導航區）   右表 = 阻力（轉速表）
 * ```
 *
 * 為什麼單獨寫一頁而不是給狀態矩陣加個開關：這一頁的骨架跟那邊沒有一處相同——
 * 那邊是「一個仪表 + 可配置網格」，這邊是「兩塊等大的表夾一塊固定的會話區」，
 * 表本身還帶目標弧與步進器。硬塞進同一個 composable 只會讓兩邊都長出一堆
 * 「如果是雙表模式就……」的分支。
 *
 * 只在平板上提供：兩塊 250dp 的表加中間的會話區至少要 800dp 寬，手機橫屏放不下。
 *
 * @param palette 當前主題色
 * @param onBack 返回上一頁
 * @param modifier 外部修飾符
 * @param controller 狀態持有者
 * @param onPaletteChange 換主題色；不接就不畫那顆按鈕
 */
@Composable
fun DualGaugeClusterScreen(
    palette: ThemePalette,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    // 這一頁的兩塊表分別調速度與阻力，所以要一台兩者都能調的機器。
    // 預設的 IndoorBike 只支援阻力，速度表會整塊退成只讀——參考稿 08 的第一個
    // 案例正是「室內單車 · 速度 + 阻力」，它假設速度也可調。
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

        // 兩塊表各自的目標值。這一頁的目標值是**本頁的顯示狀態**，不是設備狀態——
        // 真機接上之後由 FTMS 的目標值特徵提供，這裡先用 rememberSaveable 扛住轉屏。
        var speedTarget by rememberSaveable { mutableFloatStateOf(DEMO_SPEED_TARGET) }
        var resistanceTarget by rememberSaveable { mutableFloatStateOf(DEMO_RESISTANCE_TARGET) }

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
                    .padding(horizontal = CLUSTER_PAGE_PADDING),
            ) {
                ClusterTopBar(
                    state = state,
                    config = config,
                    controller = controller,
                    palette = palette,
                    onBack = onBack,
                    onPaletteChange = onPaletteChange,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = CLUSTER_SECTION_GAP),
                    horizontalArrangement = Arrangement.spacedBy(CLUSTER_SECTION_GAP),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GaugeColumn(
                        label = stringResource(R.string.metric_speed),
                        current = state.currentSpeed,
                        target = speedTarget,
                        max = SPEED_MAX_VALUE,
                        majorStep = SPEED_MAJOR,
                        unit = stringResource(R.string.unit_kmh),
                        decimals = 1,
                        readOnly = !state.device.isControllable("target-speed"),
                        config = config,
                        onStep = { dir ->
                            speedTarget = (speedTarget + dir * SPEED_STEP).coerceIn(0f, SPEED_MAX_VALUE)
                        },
                        modifier = Modifier.weight(1f),
                    )

                    SessionBay(
                        state = state,
                        config = config,
                        controller = controller,
                        modifier = Modifier.weight(SESSION_WEIGHT),
                    )

                    GaugeColumn(
                        label = stringResource(R.string.metric_resistance),
                        current = state.currentResistance,
                        target = resistanceTarget,
                        max = RESISTANCE_MAX_VALUE,
                        majorStep = RESISTANCE_MAJOR,
                        unit = stringResource(R.string.unit_level),
                        decimals = 0,
                        readOnly = !state.device.isControllable("resistance"),
                        config = config,
                        onStep = { dir ->
                            resistanceTarget =
                                (resistanceTarget + dir * RESISTANCE_STEP).coerceIn(0f, RESISTANCE_MAX_VALUE)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            val editingSlot = state.editingSlot
            if (editingSlot != null) {
                // 會話區在畫面正中，抽屜從側邊進才看得到自己在改哪一格
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
 * 一塊表加它下方的步進器。
 *
 * 步進器不畫在表盤裡：帶上它共四行時中心內容會頂到刻度數字。這裡用一個 Column
 * 把兩者豎排，表本身仍是正方形。
 *
 * Column 明確取表的邊長為寬（而不是讓它被最寬的子項撐開）：步進器比表寬的時候，
 * 撐開的那一份寬度會把整欄推歪，兩塊表就不再對稱——步進器要對齊的是它上面那塊表，
 * 不是它自己那點內容寬。
 */
@Composable
private fun GaugeColumn(
    label: String,
    current: Float,
    target: Float,
    max: Float,
    majorStep: Float,
    unit: String,
    decimals: Int,
    readOnly: Boolean,
    config: GlassConfig,
    onStep: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 表的邊長取「欄寬」與「行高減掉步進器」的較小值，自己算而不是交給
    // aspectRatio——它只能二選一，選錯一邊就是撐爆欄寬或把步進器頂出畫面。
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        // 下限只保證不是負數：行高不夠時硬撐一個寫死的最小值，表就會溢出去
        // 壓在頂欄與會話區上。寧可表小一點，版面先站得住。
        val side: Dp = minOf(maxWidth, maxHeight - STEPPER_BLOCK).coerceIn(0.dp, maxHeight)
        Column(
            modifier = Modifier.width(side),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CLUSTER_SECTION_GAP),
        ) {
            TargetGauge(
                label = label,
                current = current,
                target = target,
                max = max,
                majorStep = majorStep,
                unit = unit,
                decimals = decimals,
                readOnly = readOnly,
                config = config,
                modifier = Modifier.size(side),
            )
            TargetStepper(
                value = target,
                unit = unit,
                decimals = decimals,
                state = gaugeStateOf(current, target, readOnly),
                onStep = onStep,
                stepDownLabel = stringResource(R.string.focus_step_down_of, label),
                stepUpLabel = stringResource(R.string.focus_step_up_of, label),
                config = config,
            )
        }
    }
}

/**
 * 中間的會話資訊。
 *
 * 參考稿是 2 欄 × 4 列共八格加一條計劃進度條。格數固定——這一塊不參與版面配置，
 * 它就是「本次運動」這件事的固定讀數。
 *
 * 格子本身走 [MetricGrid]：這樣它跟其它頁面的格子一樣可以長按換指標、可以拖著
 * 換位、可以移除，編輯模式對整個 App 是同一套操作。
 */
@Composable
private fun SessionBay(
    state: WorkoutUiState,
    config: GlassConfig,
    controller: WorkoutController,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.cluster_session),
                color = config.mutedContentColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.cluster_session_eyebrow),
                color = config.mutedContentColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        MetricGrid(
            slots = state.baySlots,
            state = state,
            config = config,
            // 這八格是**讀數**：參考稿的會話格沒有調節交互，兩塊表下面已經有
            // 專門的步進器了，同一個目標值給兩個入口只會讓人以為是兩件事。
            onAdjust = {},
            onEdit = controller::beginEdit,
            columns = SESSION_COLUMNS,
            spacing = SESSION_SPACING,
            density = MetricCardDensity.COMPACT,
            editingLayout = state.editingLayout,
            onSwap = controller::swapSlots,
            onRemove = controller::clearSlot,
        )

        PlanProgress(config = config, modifier = Modifier.padding(top = 5.dp))
    }
}

/** 計劃進度：這次運動走了多少、計劃多少。 */
@Composable
private fun PlanProgress(config: GlassConfig, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(PROGRESS_HEIGHT)
                .clip(RoundedCornerShape(PROGRESS_HEIGHT / 2))
                .background(config.trackColor),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(DEMO_PLAN_FRACTION)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(PROGRESS_HEIGHT / 2))
                    .background(config.accentToneColor),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.cluster_plan_done, DEMO_PLAN_DONE),
                color = config.mutedContentColor,
                fontSize = 9.sp,
            )
            Text(
                text = stringResource(R.string.cluster_plan_total, DEMO_PLAN_TOTAL),
                color = config.mutedContentColor,
                fontSize = 9.sp,
            )
        }
    }
}

/** 頂欄：共用的那一條，右側掛這一頁自己的操作。 */
@Composable
private fun ClusterTopBar(
    state: WorkoutUiState,
    config: GlassConfig,
    controller: WorkoutController,
    palette: ThemePalette,
    onBack: () -> Unit,
    onPaletteChange: ((ThemePalette) -> Unit)?,
) {
    ClusterHeader(
        state = state,
        onBack = onBack,
        config = config,
        modifier = Modifier.padding(vertical = 6.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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

/** 儀表讀的是「當前阻力」，量程 0–16 級。 */
internal val WorkoutUiState.currentResistance: Float
    get() = MetricCatalog.find("resistance")?.let { valueOf(it).toFloatOrNull() } ?: 0f

/**
 * 這一頁的示例目標值。
 *
 * 接真機時由 FTMS 的目標值特徵提供；本階段只做 UI，留在這裡當初值。
 */
private const val DEMO_SPEED_TARGET = 28f
private const val DEMO_RESISTANCE_TARGET = 11f
private const val DEMO_PLAN_FRACTION = 0.62f
private const val DEMO_PLAN_DONE = "18:42"
private const val DEMO_PLAN_TOTAL = "30:00"

/** 速度量程 0–30，每 5 標一個數字，步進 0.5。 */
private const val SPEED_MAX_VALUE = 30f
private const val SPEED_MAJOR = 5f
private const val SPEED_STEP = 0.5f

/** 阻力量程 0–16，每 2 標一個數字（共 9 個，跟轉速表的密度一致），步進 1。 */
private const val RESISTANCE_MAX_VALUE = 16f
private const val RESISTANCE_MAJOR = 2f
private const val RESISTANCE_STEP = 1f

/**
 * 中間會話區占的寬度權重。
 *
 * 兩塊表各 1 份、中間 1.15 份：參考稿是 250 / 1fr / 250 於 744 的內容寬，
 * 中間約 244，跟表幾乎等寬。
 */
private const val SESSION_WEIGHT = 1.15f

/** 參考稿的八格會話讀數，2 欄 × 4 列。 */
private const val SESSION_COLUMNS = 2
private val SESSION_SPACING = 5.dp

private val CLUSTER_PAGE_PADDING = 16.dp
private val CLUSTER_SECTION_GAP = 10.dp

/** 步進器那一塊佔的高度（按鍵命中區 44 + 內距與間距）。 */
private val STEPPER_BLOCK = 64.dp

private val PROGRESS_HEIGHT = 6.dp
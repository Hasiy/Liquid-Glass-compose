package top.hasiyliquidglassdemo.ui.workout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 模擬控制器的時間常數，取自參考稿的觀感而非真機時序。 */
private const val WRITE_LATENCY_MS = 900L
private const val TOAST_DWELL_MS = 2200L

/** 滑動結束的完成閾值，與參考稿一致。 */
const val SLIDE_END_THRESHOLD = 0.78f

/**
 * 主畫面的狀態持有者。
 *
 * 專案沿用 Compose state holder，沒有 ViewModel——這裡跟著既有架構走。
 *
 * 這是**模擬**控制器：寫入延遲、成功與失敗都是編出來的，用來讓每一種狀態
 * 都能重複進入與退出。接真機的 FTMS runtime 屬於另一個任務，不在本階段。
 *
 * @param scope 用來跑寫入延遲與 Toast 自動收起的協程作用域
 * @param initialDevice 初始裝置能力
 */
@Stable
class WorkoutController(
    private val scope: CoroutineScope,
    initialDevice: WorkoutDeviceCapability = WorkoutDeviceCapability.IndoorBike,
) {
    var uiState by mutableStateOf(WorkoutUiState(device = initialDevice))
        private set

    /** 進行中的寫入；換一個指標調節時要先取消上一個，否則舊的 Toast 會蓋掉新的 */
    private var writeJob: Job? = null

    /**
     * 模擬的寫入結果——**這是給 UI 驗收用的開關，不是業務狀態**。
     *
     * ### 為什麼存在
     * 這一頁要複刻寫入的三種收尾：成功、失敗、被裝置拒絕。後兩種在真機上不是想觸發
     * 就觸發的（得把跑步機拔線、或送一個超出量程的值過去），但三種樣子都得能反覆比對，
     * 所以留一個欄位直接指定「這次寫入假裝是什麼結果」。
     *
     * ### 誰在用
     * [writeValue] 與 [stepValue] 的 `outcome` 參數預設讀它。頁面上目前沒有任何入口
     * 會改它，實際只有兩種用法：單元測試逐一指定三種結果，以及驗收時手動改預設值
     * 重跑一遍。所以正常跑起來永遠是 [WriteOutcome.SUCCESS]。
     *
     * ### 接真機時要做什麼
     * 整個刪掉：連同 [WriteOutcome]、[writeValue] 的 `outcome` 參數一起。真實結果
     * 來自 FTMS 的 Control Point 回應碼，不該再有一個能從外面撥的開關——留著就變成
     * 一條能讓 UI 顯示與裝置實況對不上的旁路。
     */
    var simulatedOutcome by mutableStateOf(WriteOutcome.SUCCESS)

    // ---------- 執行狀態 ----------

    fun pause() {
        if (uiState.runtime != WorkoutRuntimeState.ACTIVE) return
        // 暫停時關掉調節浮層：凍結狀態下不該還留著一個能拖的滑桿
        uiState = uiState.copy(
            runtime = WorkoutRuntimeState.PAUSED,
            adjustingSlotKey = null,
        )
    }

    fun resume() {
        if (uiState.runtime != WorkoutRuntimeState.PAUSED) return
        uiState = uiState.copy(runtime = WorkoutRuntimeState.ACTIVE)
    }

    /** 斷線。保留最後數值，關掉調節入口。 */
    fun disconnect() {
        writeJob?.cancel()
        uiState = uiState.copy(
            runtime = WorkoutRuntimeState.DISCONNECTED,
            adjustingSlotKey = null,
            controlWrite = ControlWriteState.Idle,
        )
    }

    /** 進入重連。表現與斷線相同，只有文案不同。 */
    fun reconnecting() {
        if (!uiState.runtime.isOffline) return
        uiState = uiState.copy(runtime = WorkoutRuntimeState.RECONNECTING)
    }

    fun reconnected() {
        if (!uiState.runtime.isOffline) return
        uiState = uiState.copy(runtime = WorkoutRuntimeState.ACTIVE)
    }

    /** 滑動結束達到閾值後真的結束會話。 */
    fun end() {
        writeJob?.cancel()
        uiState = uiState.copy(
            runtime = WorkoutRuntimeState.ENDED,
            adjustingSlotKey = null,
            editingSlotKey = null,
            controlWrite = ControlWriteState.Idle,
        )
    }

    /**
     * 從任何狀態回到執行中。
     *
     * [resume] 與 [reconnected] 都只認自己的來源狀態，[ENDED] 更是沒有回頭路；
     * 但狀態矩陣要求每一種狀態都能重複進入與退出，所以留一個總的入口。
     */
    fun activate() {
        writeJob?.cancel()
        uiState = uiState.copy(
            runtime = WorkoutRuntimeState.ACTIVE,
            adjustingSlotKey = null,
            editingSlotKey = null,
            controlWrite = ControlWriteState.Idle,
            slideEndProgress = 0f,
        )
    }

    /** 切換裝置能力，用來演示只讀裝置。 */
    fun useDevice(device: WorkoutDeviceCapability) {
        uiState = uiState.copy(device = device, adjustingSlotKey = null)
    }

    // ---------- 滑動結束 ----------

    /**
     * 更新滑動進度。
     *
     * @param progress 0..1；達到 [SLIDE_END_THRESHOLD] 才算完成
     * @return 是否已完成
     */
    fun updateSlideEnd(progress: Float): Boolean {
        val clamped = progress.coerceIn(0f, 1f)
        uiState = uiState.copy(slideEndProgress = clamped)
        return clamped >= SLIDE_END_THRESHOLD
    }

    /** 鬆手：達標就結束，沒達標就彈回起點。 */
    fun releaseSlideEnd() {
        if (uiState.slideEndProgress >= SLIDE_END_THRESHOLD) {
            uiState = uiState.copy(slideEndProgress = 1f)
            end()
        } else {
            uiState = uiState.copy(slideEndProgress = 0f)
        }
    }

    // ---------- 調節 ----------

    /** 開啟某個資料位的調節浮層；不可調時不做任何事。 */
    fun beginAdjust(slotKey: String) {
        if (!uiState.canAdjustAt(slotKey)) return
        uiState = uiState.copy(adjustingSlotKey = slotKey, editingSlotKey = null)
    }

    fun endAdjust() {
        uiState = uiState.copy(adjustingSlotKey = null)
    }

    /**
     * 寫入一個新值。
     *
     * 樂觀更新：先把值顯示出來並進入 Writing，延遲後再依 [outcome] 收尾。
     * 失敗與拒絕都要回彈——參考稿明確要求「已回彈」「保留原值」。
     *
     * @param metricId 指標 ID
     * @param value 目標值
     * @param outcome 模擬的寫入結果
     */
    fun writeValue(
        metricId: String,
        value: Float,
        outcome: WriteOutcome = simulatedOutcome,
    ) {
        val metric = MetricCatalog.find(metricId) ?: return
        val range = ControlRange.forMetric(metricId) ?: return
        if (!uiState.device.canAdjust(metricId, uiState.runtime)) return

        val previous = uiState.valueOf(metric)
        val target = range.format(range.snap(value))

        writeJob?.cancel()
        uiState = uiState.copy(
            overrides = uiState.overrides + (metricId to target),
            controlWrite = ControlWriteState.Writing(metricId, target),
        )

        writeJob = scope.launch {
            delay(WRITE_LATENCY_MS)
            uiState = when (outcome) {
                WriteOutcome.SUCCESS -> uiState.copy(controlWrite = ControlWriteState.Idle)
                // 回彈：把樂觀寫進去的值換回原值
                WriteOutcome.FAILED -> uiState.copy(
                    overrides = uiState.overrides + (metricId to previous),
                    controlWrite = ControlWriteState.Failed(metricId, previous),
                )
                WriteOutcome.REJECTED -> uiState.copy(
                    overrides = uiState.overrides + (metricId to previous),
                    controlWrite = ControlWriteState.Rejected(metricId, previous),
                )
            }
            if (outcome != WriteOutcome.SUCCESS) {
                delay(TOAST_DWELL_MS)
                uiState = uiState.copy(controlWrite = ControlWriteState.Idle)
            }
        }
    }

    /** 精調：在當前值上加減一個步進。 */
    fun stepValue(metricId: String, direction: Int, outcome: WriteOutcome = simulatedOutcome) {
        val metric = MetricCatalog.find(metricId) ?: return
        val range = ControlRange.forMetric(metricId) ?: return
        val current = uiState.valueOf(metric).toFloatOrNull() ?: return
        writeValue(metricId, current + direction * range.step, outcome)
    }

    // ---------- 版面 ----------

    /** 長按開啟編輯抽屜。 */
    fun beginEdit(slotKey: String) {
        uiState = uiState.copy(editingSlotKey = slotKey, adjustingSlotKey = null)
    }

    fun endEdit() {
        uiState = uiState.copy(editingSlotKey = null)
    }

    /** 把某個指標放進**當前組**的資料位，並關閉抽屜。 */
    fun assignMetric(slotKey: String, metricId: String) {
        uiState = uiState.copy(
            layout = uiState.layout + (uiState.layoutKeyOf(slotKey) to metricId),
            editingSlotKey = null,
        )
    }

    /** 恢復預設版面。 */
    /**
     * 換一種網格規格。
     *
     * 版面覆蓋不清：規格縮小只是少渲染幾格，改過的內容留在 [WorkoutUiState.layout] 裡，
     * 切回大規格原樣回來。
     *
     * @param spec 新規格
     * @param portrait true 改豎屏那一組，false 改橫屏 dock
     */
    fun setGridSpec(spec: MetricGridSpec, portrait: Boolean) {
        uiState = if (portrait) {
            uiState.copy(portraitSpec = spec)
        } else {
            uiState.copy(dockSpec = spec)
        }
    }

    /**
     * 換橫屏側邊的規格。
     *
     * 跟 [setGridSpec] 分開寫而不是塞進同一個方法：側邊是獨立的第三個維度
     * （只選欄數，行數固定），不是「豎屏或 dock 二選一」那個布林能表達的。
     *
     * @param spec 新規格
     */
    fun setSideSpec(spec: MetricGridSpec) {
        uiState = uiState.copy(sideSpec = spec)
    }

    /**
     * 切換「有內容的格子往前收攏」。
     *
     * 只改顯示順序，不動 [WorkoutUiState.layout]：使用者自己填在後面幾格的指標，
     * 關掉開關就回到他當初放的那一格。
     *
     * @param alignStart 是否收攏
     */
    fun setAlignStart(alignStart: Boolean) {
        uiState = uiState.copy(alignStart = alignStart)
    }

    /**
     * 把版面配置編碼成一個字串。
     *
     * 只含**使用者配置**：三組網格規格 + 被改過的格子。執行狀態、正在調節的格子、
     * 編輯模式都不進去——那些是這一次運動的過程，重建之後從頭開始才對。
     *
     * 格式 `豎屏規格|dock規格|k=v;k=v|側邊規格|左對齊`。新欄位一律接在最後而不是插進中間——
     * 舊版存檔只有三段，`split` 出來的 `entries` 索引不能因為新加一段而挪位，
     * 不然舊存檔會被讀錯地方。指標 ID 與資料位 key 都不含分隔符，所以拆得回來。
     */
    fun exportLayout(): String {
        val entries = uiState.layout.entries.joinToString(ENTRY_SEPARATOR) { "${it.key}$KV_SEPARATOR${it.value}" }
        return listOf(
            MetricGridSpec.encode(uiState.portraitSpec),
            MetricGridSpec.encode(uiState.dockSpec),
            entries,
            MetricGridSpec.encode(uiState.sideSpec),
            uiState.alignStart.toString(),
        ).joinToString(FIELD_SEPARATOR)
    }

    /**
     * 還原 [exportLayout] 存下來的版面。
     *
     * 認不出來的部分各自退回預設，不整個丟掉——存檔可能是上一版寫的。
     *
     * @param encoded [exportLayout] 的輸出
     */
    fun importLayout(encoded: String) {
        val fields = encoded.split(FIELD_SEPARATOR)
        val portrait = MetricGridSpec.decode(
            fields.getOrNull(0),
            MetricGridSpec.PortraitOptions,
            MetricGridSpec.PortraitDefault,
        )
        val dock = MetricGridSpec.decode(
            fields.getOrNull(1),
            MetricGridSpec.DockOptions,
            MetricGridSpec.DockDefault,
        )
        val layout = fields.getOrNull(2)
            ?.split(ENTRY_SEPARATOR)
            ?.mapNotNull { entry ->
                val parts = entry.split(KV_SEPARATOR)
                if (parts.size == 2 && parts[0].isNotEmpty()) parts[0] to parts[1] else null
            }
            ?.toMap()
            .orEmpty()
        // 舊存檔沒有第 4 段，getOrNull 回傳 null，decode 退回預設——不是崩潰。
        val side = MetricGridSpec.decode(
            fields.getOrNull(3),
            MetricGridSpec.SideOptions,
            MetricGridSpec.SideDefault,
        )
        // 同理第 5 段：舊存檔沒有，toBoolean 對 null 走不到，直接給預設 false
        val alignStart = fields.getOrNull(4)?.toBooleanStrictOrNull() ?: false
        uiState = uiState.copy(
            portraitSpec = portrait,
            dockSpec = dock,
            layout = layout,
            sideSpec = side,
            alignStart = alignStart,
        )
    }

    /**
     * 移除一格，後面的**依次往前補位**。
     *
     * 不是把那一格單獨清空——中間留一個洞，後面的讀數還停在原位，看起來像壞了。
     * 刪掉之後整塊網格塌陷一格，空位只會出現在末尾，而末尾空著的整列本來就不畫
     * （見 `visibleRowCount`）。
     *
     * 補位範圍是**同一塊網格**（見 [WorkoutUiState.blockSlotsOf]），不跨側邊與 dock。
     *
     * 補完之後每一格都落成顯式覆蓋：只寫被動過的那幾格不夠——沒寫的格子會繼續走
     * 預設值，補位的結果就被預設值蓋回去了。空出來的末尾格記成 [EMPTY_SLOT]，
     * 同理不能靠刪 key 表達。
     *
     * @param slotKey 要移除的資料位
     */
    fun clearSlot(slotKey: String) {
        val slots = uiState.blockSlotsOf(slotKey)
        val index = slots.indexOfFirst { it.key == slotKey }
        if (index < 0) return

        val shifted = slots
            .map { uiState.metricAt(it.key)?.id }
            .toMutableList()
            .apply { removeAt(index) }
        val layout = uiState.layout.toMutableMap()
        slots.forEachIndexed { position, slot ->
            layout[uiState.layoutKeyOf(slot.key)] = shifted.getOrNull(position) ?: EMPTY_SLOT
        }
        uiState = uiState.copy(layout = layout)
    }

    /** 進出版面編輯模式。離開時把開著的浮層一併收掉。 */
    fun setLayoutEditing(editing: Boolean) {
        uiState = uiState.copy(
            editingLayout = editing,
            adjustingSlotKey = null,
            editingSlotKey = null,
        )
    }

    /**
     * 兩個資料位互換指標。
     *
     * 換的是**當前顯示的內容**，所以兩邊都要落成顯式覆蓋——只寫一邊的話，
     * 另一邊會繼續走預設值，看起來像沒換。
     *
     * 跨族不換：豎屏和橫屏是兩套版面，把豎屏的格子拖到橫屏去沒有意義。
     *
     * @param fromKey 拖起來的那一格
     * @param toKey 放下的那一格
     */
    fun swapSlots(fromKey: String, toKey: String) {
        if (fromKey == toKey) return
        if (MetricSlots.isPortrait(fromKey) != MetricSlots.isPortrait(toKey)) return
        val from = uiState.metricAt(fromKey)?.id
        val to = uiState.metricAt(toKey)?.id
        val next = uiState.layout.toMutableMap()
        // 空格子也參與交換：把對方的內容搬過來，自己這邊記成 [EMPTY_SLOT]。
        // 不能用「刪掉覆蓋」表達空——那等於「沒改過」，這一格會回落到預設值，
        // 換完原地又長回原本的指標。
        next[uiState.layoutKeyOf(fromKey)] = to ?: EMPTY_SLOT
        next[uiState.layoutKeyOf(toKey)] = from ?: EMPTY_SLOT
        uiState = uiState.copy(layout = next)
    }

    fun resetLayout() {
        uiState = uiState.copy(layout = emptyMap(), editingSlotKey = null)
    }

    /** 在主組與第 2 組之間循環。 */
    fun switchGroup() {
        uiState = uiState.copy(groupIndex = (uiState.groupIndex + 1) % GROUP_COUNT)
    }

    private companion object {
        const val GROUP_COUNT = 2

        /** [exportLayout] 的分隔符。指標 ID 與資料位 key 都不含這幾個字元。 */
        const val FIELD_SEPARATOR = "|"
        const val ENTRY_SEPARATOR = ";"
        const val KV_SEPARATOR = "="
    }
}

/** 模擬的寫入結果，對應參考稿的三種 Toast。 */
enum class WriteOutcome { SUCCESS, FAILED, REJECTED }

/**
 * 記住一個 [WorkoutController]。
 *
 * @param device 初始裝置能力
 */
@Composable
fun rememberWorkoutController(
    device: WorkoutDeviceCapability = WorkoutDeviceCapability.IndoorBike,
): WorkoutController {
    val scope = rememberCoroutineScope()
    val controller = remember(scope, device) { WorkoutController(scope, device) }

    // 版面是**使用者配置**，不是執行狀態：換過的網格規格與挪過位置的格子，
    // 轉屏（Activity 重建）之後要還在。運動狀態不存——那是這一次運動的過程，
    // 重建之後從頭開始才對。
    //
    // 存的是一個字串。真實產品該落 DataStore（跨進程、跨安裝都在），
    // 這一版先讓它扛住重建就夠驗收，換儲存後端只要改這一處。
    var saved by rememberSaveable { mutableStateOf(controller.exportLayout()) }
    LaunchedEffect(controller) { controller.importLayout(saved) }
    LaunchedEffect(controller) {
        snapshotFlow { controller.exportLayout() }.collect { saved = it }
    }
    return controller
}

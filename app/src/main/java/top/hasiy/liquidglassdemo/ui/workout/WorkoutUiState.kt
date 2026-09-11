package top.hasiyliquidglassdemo.ui.workout

import androidx.compose.runtime.Immutable

/**
 * 主畫面的完整狀態。
 *
 * 本階段只做 UI 複刻：所有數值來自 [MetricCatalog] 的固定示例值，
 * 由 [WorkoutController] 驅動狀態切換，不接 FTMS 真機。
 *
 * @param runtime 執行狀態，決定計時是否凍結、調節能不能用
 * @param device 裝置能力，決定哪些指標可調、哪些候選指標會出現在抽屜
 * @param controlWrite 進行中或剛結束的一次控制寫入
 * @param layout `組別:資料位 key` → 指標 ID。只存被改過的位置，其餘走 [MetricSlots.defaultMetricId]
 * @param overrides 被調節過的指標值，覆蓋 [MetricCatalog] 的示例值
 * @param adjustingSlotKey 正在調節的資料位；null 表示沒有開啟調節浮層
 * @param editingSlotKey 正在編輯（長按觸發）的資料位；null 表示抽屜關閉
 * @param groupIndex 當前指標組，0 為主組、1 為第 2 組
 * @param slideEndProgress 滑動結束的進度（0..1），達到閾值才真的結束
 * @param portraitSpec 豎屏那一組網格的規格
 * @param dockSpec 橫屏 dock 網格的規格
 * @param sideSpec 橫屏側邊的規格，只選欄數（行數固定）
 * @param alignStart 把有內容的格子往前收攏、空槽推到末尾。只影響顯示順序，
 *   不動 [layout]——關回去每一格還在原位
 * @param editingLayout 是否處於**版面編輯模式**。這個模式下格子可以拖著換位，
 *   單擊改成換指標——不開這個模式的話拖動就得和「長按換指標」搶同一個手勢
 */
@Immutable
data class WorkoutUiState(
    val runtime: WorkoutRuntimeState = WorkoutRuntimeState.ACTIVE,
    val device: WorkoutDeviceCapability = WorkoutDeviceCapability.IndoorBike,
    val controlWrite: ControlWriteState = ControlWriteState.Idle,
    val layout: Map<String, String> = emptyMap(),
    val overrides: Map<String, String> = emptyMap(),
    val adjustingSlotKey: String? = null,
    val editingSlotKey: String? = null,
    val groupIndex: Int = 0,
    val slideEndProgress: Float = 0f,
    val portraitSpec: MetricGridSpec = MetricGridSpec.PortraitDefault,
    val dockSpec: MetricGridSpec = MetricGridSpec.DockDefault,
    val sideSpec: MetricGridSpec = MetricGridSpec.SideDefault,
    val alignStart: Boolean = false,
    val editingLayout: Boolean = false,
) {
    /**
     * 取某個資料位當前該顯示的指標。
     *
     * @param slotKey 資料位 key
     * @return 指標；資料位或指標 ID 無效時回傳 null
     */
    fun metricAt(slotKey: String): WorkoutMetric? {
        // 覆蓋值優先，而且**空字串是一個有效的覆蓋**：它表示「這一格被清空了」。
        //
        // 需要這個哨兵是因為「清空」和「沒改過」是兩件事。把覆蓋直接刪掉的話，
        // 這一格會回落到預設值——拖著一格去和空格子換位，換完原地又長回原本的指標。
        val override = layout[layoutKeyOf(slotKey)]
        if (override != null) {
            return if (override == EMPTY_SLOT) null else MetricCatalog.find(override)
        }
        val id = MetricSlots.defaultMetricId(slotKey, groupIndex) ?: return null
        return MetricCatalog.find(id)
    }

    /**
     * 版面覆蓋的儲存 key。
     *
     * 帶上組別：兩組是兩套讀數，在主組把某一格改成踏頻，不該讓第 2 組的那一格
     * 也跟著變——否則切組看起來像沒切。
     */
    fun layoutKeyOf(slotKey: String): String = "$groupIndex:$slotKey"

    /**
     * 指標當前的顯示值：調節過的用覆蓋值，否則用示例值。
     *
     * 斷線與暫停時一律讀這裡——參考稿要求「保留最後數值」，不清零也不顯示佔位。
     */
    fun valueOf(metric: WorkoutMetric): String = overrides[metric.id] ?: metric.value

    /**
     * 指標在卡片上該顯示什麼。
     *
     * 三種不是「一個數」的情況：
     * - 要外接感測器但沒配對：顯示 [DISPLAY_NO_DATA]，由卡片畫成一條橫線。
     *   不能顯示 0——0 是「量到了，就是 0」，跟「沒量到」是兩回事。
     * - 阻力不由 App 控制（機械旋鈕檔）：顯示 [DISPLAY_MANUAL]，數值位置寫「手動」。
     *   這台機器的阻力是使用者自己轉的，App 報一個數字反而是假的。
     * - 其餘：走 [valueOf]。
     *
     * @param metric 指標
     * @return 顯示用的字串，或兩個哨兵之一
     */
    fun displayValueOf(metric: WorkoutMetric): String = when {
        metric.requiresSensor && !device.sensorConnected -> DISPLAY_NO_DATA
        metric.id == RESISTANCE_ID && !device.isControllable(metric.id) -> DISPLAY_MANUAL
        else -> valueOf(metric)
    }

    /** 這個資料位上的指標現在能不能調。 */
    fun canAdjustAt(slotKey: String): Boolean {
        val metric = metricAt(slotKey) ?: return false
        return device.canAdjust(metric.id, runtime)
    }

    /**
     * 這個資料位該顯示哪種只讀提示。
     *
     * 參考稿把「裝置只讀」與「斷線暫時不能調」分成兩種文案，不能混用。
     */
    fun readOnlyReasonAt(slotKey: String): ReadOnlyReason? {
        val metric = metricAt(slotKey) ?: return null
        if (!device.isControllable(metric.id)) {
            // 只有「本來就是控制項」的指標才需要解釋為什麼不能調。距離、熱量、步數
            // 在任何裝置上都只是讀數，掛上「設備只讀」會讓整面板看起來壞掉。
            val downgraded = device.readOnly && metric.id in device.controllableMetricIds
            return if (downgraded) ReadOnlyReason.DEVICE_READ_ONLY else null
        }
        return if (runtime.allowsControl) null else ReadOnlyReason.TEMPORARILY_LOCKED
    }

    /**
     * 抽屜要列出的候選指標。
     *
     * 參考稿的規則：候選項由裝置能力動態提供——未支援的指標不出現，
     * 也不用 0 佔位。
     *
     * @param category 分類頁籤
     */
    fun candidatesIn(category: MetricCategory): List<WorkoutMetric> =
        MetricCatalog.inCategory(category).filter { it.id in device.supportedMetricIds }

    /** 豎屏那一組當前的資料位。 */
    val portraitSlots: List<MetricSlot> get() = MetricSlots.portrait(portraitSpec)

    /** 橫屏 dock 當前的資料位。 */
    val dockSlots: List<MetricSlot> get() = MetricSlots.dock(dockSpec)

    /** 橫屏側邊當前的資料位，兩側合起來一份。 */
    val sideSlots: List<MetricSlot> get() = MetricSlots.side(sideSpec)

    /** 平板會話讀數區的資料位。格數固定，不隨規格變。 */
    val baySlots: List<MetricSlot> get() = MetricSlots.bay(MetricSlots.BAY_MAX)

    /**
     * 這個資料位所屬的版面族，**只含當前規格內的格子**。
     *
     * 豎屏是一套，橫屏的側卡與 dock 合起來是另一套。
     *
     * 必須跟著規格算：規格從 3×3 收回 2×3 之後，第 7～9 格的版面覆蓋還留在
     * [layout] 裡（切回去要能恢復），但它們已經不在畫面上——拿全集去比的話，
     * 一個只存在於「更大規格」裡的指標會被誤報成「已在其他位置顯示」。
     *
     * @param slotKey 資料位 key
     */
    fun familySlotsOf(slotKey: String): List<MetricSlot> = when {
        MetricSlots.isPortrait(slotKey) -> portraitSlots
        // 平板那兩頁的會話讀數區自成一族：它是一塊獨立的網格，
        // 旁邊沒有別的網格會跟它爭同一個指標。
        MetricSlots.isBay(slotKey) -> baySlots
        MetricSlots.isCircuitBay(slotKey) -> MetricSlots.CircuitBay
        else -> sideSlots + dockSlots
    }

    /**
     * 這個資料位所在的**那一塊網格**，順序即顯示順序。
     *
     * 跟 [familySlotsOf] 不是同一件事：那個是「判斷指標有沒有被佔用」的範圍，
     * 橫屏的側邊與 dock 算同一族；這個是「刪掉一格之後誰往前補位」的範圍，
     * 只能是同一塊網格——把 dock 的一格刪掉卻讓側卡的內容跑進 dock，
     * 那不是補位，是把版面攪了。
     *
     * @param slotKey 資料位 key
     */
    fun blockSlotsOf(slotKey: String): List<MetricSlot> = when {
        MetricSlots.isPortrait(slotKey) -> portraitSlots
        MetricSlots.isSide(slotKey) -> sideSlots
        MetricSlots.isBay(slotKey) -> baySlots
        MetricSlots.isCircuitBay(slotKey) -> MetricSlots.CircuitBay
        else -> dockSlots
    }

    /**
     * 這個指標是不是已經擺在同一版面的別的格子上。
     *
     * @param slotKey 當前正在編輯的資料位，比較時要排除自己
     * @param metricId 候選指標
     */
    fun isMetricUsedElsewhere(slotKey: String, metricId: String): Boolean =
        familySlotsOf(slotKey)
            .filter { it.key != slotKey }
            .any { metricAt(it.key)?.id == metricId }

    /** 正在編輯的資料位。 */
    val editingSlot: MetricSlot?
        get() = editingSlotKey?.let { key -> MetricSlots.All.firstOrNull { it.key == key } }

    /** 正在調節的指標。 */
    val adjustingMetric: WorkoutMetric?
        get() = adjustingSlotKey?.let { metricAt(it) }

    /** 是否有浮層蓋在儀表上（調節浮層或編輯抽屜）。 */
    val hasOverlay: Boolean get() = adjustingSlotKey != null || editingSlotKey != null
}

/**
 * 「沒有數據」的哨兵。
 *
 * 卡片看到它就畫一條橫線、不畫單位。用一個不可能出現在真實數值裡的字串，
 * 而不是空字串——空字串在版面覆蓋裡另有含義（[EMPTY_SLOT]，「這一格是空的」），
 * 兩件事撞在一起會讓沒配對心率帶的那一格整格消失。
 */
const val DISPLAY_NO_DATA: String = "\u0000no-data"

/** 「阻力由使用者自己轉」的哨兵。卡片在數值位置寫「手動」。 */
const val DISPLAY_MANUAL: String = "\u0000manual"

/** 阻力的指標 ID。只有它有「手動」這一態。 */
internal const val RESISTANCE_ID = "resistance"

/**
 * 版面覆蓋裡表示「這一格是空的」的哨兵。
 *
 * 用空字串而不是把 key 刪掉：刪掉等於「沒改過」，會回落到預設值。
 * 它跟著 [WorkoutController.exportLayout] 一起存檔，`k=` 這種形式原樣能還原。
 */
const val EMPTY_SLOT: String = ""

/** 指標不能調節的原因，兩種提示文案不同。 */
enum class ReadOnlyReason {
    /** 裝置本身不支援調節。 */
    DEVICE_READ_ONLY,

    /** 裝置支援，但當前狀態（斷線、暫停）暫時不能調。 */
    TEMPORARILY_LOCKED,
}

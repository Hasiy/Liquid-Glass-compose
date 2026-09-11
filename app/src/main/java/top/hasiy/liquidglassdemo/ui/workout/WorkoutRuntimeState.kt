package top.hasiyliquidglassdemo.ui.workout

import androidx.compose.runtime.Immutable

/**
 * 運動階段的執行狀態。
 *
 * 決定計時與實時指標是否更新，以及調節入口是否可用——參考稿的規則是
 * 「只有 ACTIVE 能調」，其餘狀態一律禁用，且保留最後數值不清零。
 */
enum class WorkoutRuntimeState {
    /** 計時走、實時指標更新、可調節。 */
    ACTIVE,

    /** 計時與實時指標凍結，顯示暫停浮層。 */
    PAUSED,

    /** 連線中斷，保留最後數值。 */
    DISCONNECTED,

    /** 斷線後正在重連，表現同 [DISCONNECTED]，只有文案不同。 */
    RECONNECTING,

    /** 會話已結束。 */
    ENDED;

    /** 計時與實時指標是否還在跑。 */
    val isLive: Boolean get() = this == ACTIVE

    /**
     * 調節入口是否可用。
     *
     * 只有 [ACTIVE] 可調。這是硬性規則：斷線時寫入必然失敗，
     * 暫停時調了也沒有意義，結束後更不該再寫。
     */
    val allowsControl: Boolean get() = this == ACTIVE

    /** 是否處於連線異常（斷線或重連中）。 */
    val isOffline: Boolean get() = this == DISCONNECTED || this == RECONNECTING
}

/**
 * 一次控制寫入的結果。
 *
 * 參考稿把三種結果都用 Toast 呈現，差別在文案、圖示與數值處理：
 * 寫入中樂觀顯示目標值，失敗與拒絕都要回彈到原值。
 */
@Immutable
sealed interface ControlWriteState {

    /** 沒有進行中的寫入。 */
    data object Idle : ControlWriteState

    /**
     * 寫入中，樂觀顯示目標值。
     *
     * @param metricId 正在調節的指標
     * @param target 目標值的顯示字串
     */
    data class Writing(val metricId: String, val target: String) : ControlWriteState

    /**
     * 寫入失敗（多半是連線中斷），數值已回彈。
     *
     * @param metricId 調節失敗的指標
     * @param restored 回彈後的值
     */
    data class Failed(val metricId: String, val restored: String) : ControlWriteState

    /**
     * 裝置拒絕（當前狀態不允許調節），保留原值。
     *
     * @param metricId 被拒絕的指標
     * @param restored 保留的原值
     */
    data class Rejected(val metricId: String, val restored: String) : ControlWriteState

    /** 是否需要顯示 Toast。 */
    val isVisible: Boolean get() = this !is Idle
}

/**
 * 裝置能力。
 *
 * 參考稿的規則：候選指標由裝置能力動態提供，未支援的指標不出現、
 * 也不用 0 佔位；只讀裝置把可調節指標一併降級為只讀。
 *
 * @param name 裝置顯示名稱
 * @param protocol 連線協議，顯示在狀態行尾（參考稿的「已连接 · FTMS Bike」）
 * @param readOnly 裝置是否完全不支援調節
 * @param controllableMetricIds 支援調節的指標 ID；[readOnly] 為 true 時視為空
 * @param supportedMetricIds 裝置能提供的指標 ID；抽屜只列這些
 * @param sensorConnected 外接感測器（心率帶）是否已配對。沒配對時
 *   [WorkoutMetric.requiresSensor] 的指標沒有數據，顯示成一條橫線
 */
@Immutable
data class WorkoutDeviceCapability(
    val name: String,
    val protocol: String = "FTMS",
    val readOnly: Boolean = false,
    val controllableMetricIds: Set<String> = setOf("resistance", "target-speed"),
    val supportedMetricIds: Set<String> = MetricCatalog.All.map { it.id }.toSet(),
    val sensorConnected: Boolean = true,
) {
    /**
     * 這個指標在當前狀態下能不能調。
     *
     * @param metricId 指標 ID
     * @param runtime 當前執行狀態
     */
    fun canAdjust(metricId: String, runtime: WorkoutRuntimeState): Boolean =
        !readOnly && runtime.allowsControl && metricId in controllableMetricIds

    /**
     * 這個指標本身是否可調（不看執行狀態）。
     *
     * 用來區分「裝置只讀」與「當前不能調」——參考稿這兩種情況的提示文案不同。
     */
    fun isControllable(metricId: String): Boolean = !readOnly && metricId in controllableMetricIds

    companion object {
        /** 參考稿豎屏場景的室內單車：阻力可調。 */
        val IndoorBike = WorkoutDeviceCapability(
            name = "室内单车",
            protocol = "FTMS Bike",
            controllableMetricIds = setOf("resistance"),
        )

        /** 參考稿橫屏場景的跑步機：目標速度與阻力都可調。 */
        val Treadmill = WorkoutDeviceCapability(
            name = "跑步机",
            protocol = "FTMS Treadmill",
            controllableMetricIds = setOf("target-speed", "resistance"),
        )

        /**
         * 速度與阻力都能調的單車。
         *
         * 雙表儀表舱與路線地圖那兩頁各有一塊速度表和一塊阻力表，兩邊都要能調——
         * 預設的 [IndoorBike] 只支援阻力，速度表會整塊退成只讀。
         */
        val DualTargetBike = WorkoutDeviceCapability(
            name = "室内单车",
            protocol = "FTMS Bike",
            controllableMetricIds = setOf("resistance", "target-speed"),
        )

        /**
         * 機械阻力的單車：阻力由使用者自己轉旋鈕，App 讀不到也調不了。
         *
         * 這台機器的阻力格子在數值位置寫「手動」——報一個數字反而是假的。
         * 它跟「只讀裝置」不是一回事：只讀是整台機器都不接受寫入，
         * 這台只是阻力這一項沒有電控。
         */
        val ManualResistanceBike = WorkoutDeviceCapability(
            name = "室内单车",
            protocol = "FTMS Bike",
            controllableMetricIds = emptySet(),
        )

        /** 只讀裝置：能報數但不接受任何寫入。 */
        val ReadOnlyBike = WorkoutDeviceCapability(
            name = "室内单车",
            protocol = "FTMS Bike",
            readOnly = true,
        )
    }
}

/**
 * 可調節指標的量程。
 *
 * 數值取自參考稿：阻力 1–16 步進 1，目標速度 0.5–20.0 步進 0.1。
 *
 * @param min 下限
 * @param max 上限
 * @param step 精調的每次增減量
 * @param decimals 顯示小數位數
 */
@Immutable
data class ControlRange(
    val min: Float,
    val max: Float,
    val step: Float,
    val decimals: Int,
) {
    /** 把數值夾到量程內並對齊步進。 */
    fun snap(value: Float): Float {
        val clamped = value.coerceIn(min, max)
        val steps = Math.round((clamped - min) / step)
        return (min + steps * step).coerceIn(min, max)
    }

    /** 數值在量程中的位置（0..1），供滑桿使用。 */
    fun fractionOf(value: Float): Float =
        if (max <= min) 0f else ((value - min) / (max - min)).coerceIn(0f, 1f)

    /** 由滑桿位置反算數值。 */
    fun valueAt(fraction: Float): Float = snap(min + fraction.coerceIn(0f, 1f) * (max - min))

    /**
     * 滑桿的離散分隔點數。
     *
     * `GlassSlider.steps` 數的是**區間內部**的分隔點，不含頭尾，所以要減 1：
     * 阻力 1–16 步進 1 共 15 段、14 個內部分隔點。
     *
     * 用 `roundToInt` 而不是 `toInt`：目標速度是 (20 − 0.5) / 0.1，浮點算出來是
     * 194.99998，截斷會少一格。
     */
    val discreteSteps: Int
        get() = (Math.round((max - min) / step) - 1).coerceAtLeast(0)

    /** 依 [decimals] 格式化。 */
    fun format(value: Float): String =
        if (decimals == 0) value.toInt().toString() else "%.${decimals}f".format(value)

    companion object {
        val Resistance = ControlRange(min = 1f, max = 16f, step = 1f, decimals = 0)
        val TargetSpeed = ControlRange(min = 0.5f, max = 20f, step = 0.1f, decimals = 1)

        /** 依指標 ID 取量程；不可調的指標回傳 null。 */
        fun forMetric(metricId: String): ControlRange? = when (metricId) {
            "resistance" -> Resistance
            "target-speed" -> TargetSpeed
            else -> null
        }
    }
}

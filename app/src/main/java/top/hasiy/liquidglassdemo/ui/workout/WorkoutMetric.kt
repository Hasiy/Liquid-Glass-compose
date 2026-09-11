package top.hasiyliquidglassdemo.ui.workout

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import top.hasiyliquidglassdemo.R

/** 指標分類，對應參考稿抽屜裡的三個分頁。 */
enum class MetricCategory {
    /** 實時：心率、功率、踏頻、坡度、阻力、目標速度、當前速度 */
    REALTIME,

    /** 平均：平均速度、平均心率、平均功率 */
    AVERAGE,

    /** 最大 / 總數值：時長、距離、熱量、步數、峰值心率、最大功率 */
    AGGREGATE,
}

/**
 * 指標卡的附加圖形。
 *
 * 只有橫屏側邊的大卡片會畫，底部 dock 的小卡不畫。
 */
enum class MetricArt {
    NONE,

    /** 時長卡：附「自動記錄中」狀態 */
    DURATION,

    /** 心率卡：脈搏波 */
    HEART,

    /** 功率卡：四段條形 */
    POWER,

    /** 坡度卡：坡形 */
    SLOPE,

    /** 拉力卡：一段拉力曲線，配網格底 */
    PULL,
}

/**
 * 「本桨 vs 上一桨」的對比資料。
 *
 * 划船機的桨次指標單看一個數讀不出好壞——42 N 是進步還是退步，要跟上一桨比。
 * 所以這一類指標的卡片畫兩條橫進度：上面一條是本桨、下面一條是上一桨，
 * 再加一根豎線標出歷史平均當基準。
 *
 * 三個值都是**同一個量程**（[max]）下的絕對值，比例由卡片自己算——存比例的話
 * 換量程就得把三個數全改一遍。
 *
 * @param max 量程上限，兩條進度都按它算比例
 * @param previous 上一桨的值
 * @param reference 歷史平均，畫成那根豎線
 */
@Immutable
data class MetricComparison(
    val max: Float,
    val previous: Float,
    val reference: Float,
)

/**
 * 一個可放進資料位的指標。
 *
 * [value] 是參考稿的固定示例值——本階段只做 UI 複刻，不接 FTMS 真機，
 * 所以它是資料而不是文案，直接留在程式碼裡；[labelRes] 與 [unitRes] 才是要翻譯的。
 *
 * @param id 穩定標識，與參考稿的 `metricCatalog` 一致，供版面持久化使用
 * @param category 所屬分類，決定它出現在抽屜的哪個分頁
 * @param labelRes 指標名稱
 * @param value 顯示值
 * @param unitRes 單位；無單位（例如步數、時長）為 null
 * @param art 附加圖形，只在橫屏側邊大卡片生效
 * @param controllable 裝置是否支援調節這個指標（阻力與目標速度）
 * @param requiresSensor 要不要外接感測器才有數據。心率這類沒配對上就沒有值，
 *   顯示成一條橫線而不是 0——0 是「量到了，就是 0」，那是另一回事
 * @param comparison 有值的話這一格畫成「本桨 vs 上一桨」的雙條對比卡，
 *   見 [MetricComparison]
 */
@Immutable
data class WorkoutMetric(
    val id: String,
    val category: MetricCategory,
    @StringRes val labelRes: Int,
    val value: String,
    @StringRes val unitRes: Int?,
    val art: MetricArt = MetricArt.NONE,
    val controllable: Boolean = false,
    val requiresSensor: Boolean = false,
    val comparison: MetricComparison? = null,
)

/**
 * 候選指標。
 *
 * 參考稿給了 16 個；拉力與做功是後補的——dock 開到 3×4 有 12 格，
 * 原本那 8 個預設值填不滿，第 3 列就空著。
 *
 * 順序與 `metricCatalog` 一致；抽屜按 [MetricCategory] 分頁後仍維持這個相對順序。
 */
object MetricCatalog {

    val All: List<WorkoutMetric> = listOf(
        WorkoutMetric("duration", MetricCategory.AGGREGATE, R.string.metric_duration, "18:42", null, MetricArt.DURATION),
        WorkoutMetric("distance", MetricCategory.AGGREGATE, R.string.metric_distance, "3.82", R.string.unit_km),
        WorkoutMetric("calories", MetricCategory.AGGREGATE, R.string.metric_calories, "228", R.string.unit_kcal),
        WorkoutMetric("heart", MetricCategory.REALTIME, R.string.metric_heart, "146", R.string.unit_bpm, MetricArt.HEART, requiresSensor = true),
        WorkoutMetric("power", MetricCategory.REALTIME, R.string.metric_power, "168", R.string.unit_watt, MetricArt.POWER),
        WorkoutMetric("cadence", MetricCategory.REALTIME, R.string.metric_cadence, "82", R.string.unit_rpm),
        WorkoutMetric("slope", MetricCategory.REALTIME, R.string.metric_slope, "3.0", R.string.unit_percent, MetricArt.SLOPE),
        WorkoutMetric("resistance", MetricCategory.REALTIME, R.string.metric_resistance, "8", R.string.unit_level, controllable = true),
        WorkoutMetric("target-speed", MetricCategory.REALTIME, R.string.metric_target_speed, "12.5", R.string.unit_kmh, controllable = true),
        WorkoutMetric("speed", MetricCategory.REALTIME, R.string.metric_speed, "10.5", R.string.unit_kmh),
        WorkoutMetric("avg-speed", MetricCategory.AVERAGE, R.string.metric_avg_speed, "9.8", R.string.unit_kmh),
        WorkoutMetric("avg-heart", MetricCategory.AVERAGE, R.string.metric_avg_heart, "138", R.string.unit_bpm, MetricArt.HEART, requiresSensor = true),
        WorkoutMetric("steps", MetricCategory.AGGREGATE, R.string.metric_steps, "4,286", null),
        WorkoutMetric("peak-heart", MetricCategory.AGGREGATE, R.string.metric_peak_heart, "154", R.string.unit_bpm, MetricArt.HEART, requiresSensor = true),
        WorkoutMetric("avg-power", MetricCategory.AVERAGE, R.string.metric_avg_power, "154", R.string.unit_watt, MetricArt.POWER),
        WorkoutMetric("max-power", MetricCategory.AGGREGATE, R.string.metric_max_power, "212", R.string.unit_watt, MetricArt.POWER),
        WorkoutMetric("pull", MetricCategory.REALTIME, R.string.metric_pull, "42", R.string.unit_newton, MetricArt.PULL),
        WorkoutMetric("work", MetricCategory.AGGREGATE, R.string.metric_work, "1", R.string.unit_kj),
        // 賽道模式專屬的兩個圈速讀數。它們只在閉環賽道上有意義——開放路線沒有「圈」，
        // 所以不進側邊與 dock 的預設值，只由賽道那一頁的骨架擺出來。
        WorkoutMetric("lap-time", MetricCategory.AGGREGATE, R.string.metric_lap_time, "3:18", null),
        WorkoutMetric("best-lap", MetricCategory.AGGREGATE, R.string.metric_best_lap, "7:42", null),

        // 划船機的桨次指標。量程按器材的常見上限給：拉力 0–200 N、單桨時間 0–3 s、
        // 每桨功率 0–150 W、拉桨長度 0–140 cm、每桨里程 0–12 m、每桨做功 0–170 J。
        // 接真機時量程應該由器材上報的範圍決定，示例值先寫在這裡。
        WorkoutMetric(
            "peak-pull", MetricCategory.AGGREGATE, R.string.metric_peak_pull, "126.3", R.string.unit_newton,
            comparison = MetricComparison(max = 200f, previous = 92f, reference = 145f),
        ),
        WorkoutMetric(
            "avg-pull", MetricCategory.AVERAGE, R.string.metric_avg_pull, "42.1", R.string.unit_newton,
            comparison = MetricComparison(max = 200f, previous = 10f, reference = 56f),
        ),
        WorkoutMetric(
            "drive-time", MetricCategory.REALTIME, R.string.metric_drive_time, "0.1", R.string.unit_second,
            comparison = MetricComparison(max = 3f, previous = 2.16f, reference = 0.15f),
        ),
        WorkoutMetric(
            "recovery-time", MetricCategory.REALTIME, R.string.metric_recovery_time, "0.4", R.string.unit_second,
            comparison = MetricComparison(max = 3f, previous = 1f, reference = 0.85f),
        ),
        WorkoutMetric(
            "stroke-power", MetricCategory.REALTIME, R.string.metric_stroke_power, "20.1", R.string.unit_watt,
            comparison = MetricComparison(max = 150f, previous = 30f, reference = 26f),
        ),
        WorkoutMetric(
            "drive-length", MetricCategory.REALTIME, R.string.metric_drive_length, "18.0", R.string.unit_cm,
            comparison = MetricComparison(max = 140f, previous = 110f, reference = 24f),
        ),
        WorkoutMetric(
            "stroke-distance", MetricCategory.REALTIME, R.string.metric_stroke_distance, "1.0", R.string.unit_meter,
            comparison = MetricComparison(max = 12f, previous = 4.6f, reference = 2f),
        ),
        WorkoutMetric(
            "stroke-work", MetricCategory.REALTIME, R.string.metric_stroke_work, "10.5", R.string.unit_joule,
            comparison = MetricComparison(max = 170f, previous = 51f, reference = 29f),
        ),
    )

    private val byId: Map<String, WorkoutMetric> = All.associateBy { it.id }

    /**
     * 按 ID 取指標。
     *
     * @param id 指標 ID；未知時回傳 null（例如版面持久化後指標被移除）
     */
    fun find(id: String): WorkoutMetric? = byId[id]

    /** 某個分類下的候選指標，維持 [All] 的相對順序。 */
    fun inCategory(category: MetricCategory): List<WorkoutMetric> =
        All.filter { it.category == category }
}

/**
 * 資料位。
 *
 * key 沿用參考稿的命名（`portrait-0`、`land-side-2`、`land-dock-7`），
 * 版面持久化與「已在其他位置顯示」的判斷都依賴它。
 *
 * @param key 穩定標識
 * @param nameRes 位置名稱，顯示在抽屜標題（例如「豎屏第 1 個資料位」）
 * @param nameIndex 名稱模板要填的序號（1 起算）。網格規格可調之後格數不固定，
 *   豎屏格與 dock 格的名稱走帶序號的模板；側邊四格是固定方位、名稱具名，這裡為 null
 * @param art 這個位置是否會畫附加圖形。只有橫屏側邊的四個大卡片會
 */
@Immutable
data class MetricSlot(
    val key: String,
    @StringRes val nameRes: Int,
    val nameIndex: Int? = null,
    val drawsArt: Boolean = false,
)

/**
 * 資料位版面。
 *
 * 豎屏那一組與橫屏 dock 的格數由 [MetricGridSpec] 決定，所以資料位是**按規格生成**的，
 * 不是寫死的固定清單。key 只跟序號綁（`portrait-3`、`land-dock-11`），規格縮小時
 * 多出來的 key 不再渲染但版面覆蓋仍留著——切回大規格能原樣恢復。
 *
 * 橫屏側邊不受 dock 規格控制，但有自己獨立的規格（只選欄數）：它們是固定方位的
 * 主讀數，見 [side]。
 */
object MetricSlots {

    /**
     * 豎屏那一組資料位。
     *
     * @param spec 網格規格，決定生成幾格
     */
    fun portrait(spec: MetricGridSpec): List<MetricSlot> = List(spec.slotCount) { index ->
        MetricSlot(
            key = PORTRAIT_PREFIX + index,
            nameRes = R.string.slot_portrait_at,
            nameIndex = index + 1,
        )
    }

    /**
     * 橫屏 dock 的資料位。
     *
     * @param spec 網格規格，決定生成幾格
     */
    fun dock(spec: MetricGridSpec): List<MetricSlot> = List(spec.slotCount) { index ->
        MetricSlot(
            key = DOCK_PREFIX + index,
            nameRes = R.string.slot_land_dock_at,
            nameIndex = index + 1,
        )
    }

    /**
     * 橫屏側邊的資料位，左右各按 [spec] 生成。
     *
     * 行數固定（[SIDE_ROWS]），只有欄數隨 [spec] 變——它們是儀表兩側的主讀數，
     * 位置本身是版面骨架，不像 dock 那樣行列都能開。
     *
     * 回傳的是**兩側合起來**的一份清單，跟 [portrait] / [dock] 不同：呼叫端要
     * `take(spec.slotCount)` 取左側、`drop(spec.slotCount)` 取右側——沿用既有的
     * key 排布（左側 0..slotCount-1、右側 slotCount..2*slotCount-1），欄數變動時
     * 舊版面覆蓋的意義會跟著重新對應，這跟豎屏／dock 換欄數時是同一套取捨
     * （見 [MetricGridSpec] 的 KDoc）。
     *
     * @param spec 側邊規格，決定每側幾欄
     */
    fun side(spec: MetricGridSpec): List<MetricSlot> = List(spec.slotCount * 2) { index ->
        MetricSlot(
            key = SIDE_PREFIX + index,
            nameRes = R.string.slot_land_side_at,
            nameIndex = index + 1,
            // 附加圖形只在最寬鬆那一檔畫得下，而那正是 MetricCard 自己按密度檔
            // 篩的（`density == REGULAR` 才畫）。這裡不用跟著欄數再擋一次。
            drawsArt = true,
        )
    }

    /** 側邊每一欄幾列，固定值，與 [MetricGridSpec] 的 `SideOptions` 保持一致。 */
    const val SIDE_ROWS: Int = 4

    /**
     * 平板專屬顯示模式的會話讀數位。
     *
     * 雙表儀表舱（08）中間那塊 2×4、路線模式（09）底部那排 2×4 都用這一族。
     * 兩頁各自持有自己的 [WorkoutController]，版面覆蓋互不影響，所以可以共用同一組 key。
     *
     * 這一族**不參與規格配置**：那兩頁的版面是照參考稿定死的骨架，能改的只有
     * 「哪一格放什麼」——所以只給拖動換位與移除，不給 2×4 / 3×5 那種格數選項。
     *
     * @param count 這一頁排幾格，不超過 [BAY_MAX]
     */
    fun bay(count: Int): List<MetricSlot> = List(count.coerceAtMost(BAY_MAX)) { index ->
        MetricSlot(
            key = BAY_PREFIX + index,
            nameRes = R.string.slot_bay_at,
            nameIndex = index + 1,
        )
    }

    /**
     * 賽道模式底部那 4 格。
     *
     * 跟 [bay] 分成兩族而不是「同一族取前 4 格」：兩種模式報的是不同的事——
     * 開放路線報剩餘距離與八項會話讀數，閉環賽道報圈速。共用一族的話，
     * 使用者在路線模式排好的第 5～8 格會在切到賽道模式時被截掉，切回來還在不在
     * 就得看實作細節；分開之後兩套版面各自記得自己的樣子。
     */
    val CircuitBay: List<MetricSlot> = List(CIRCUIT_BAY_COUNT) { index ->
        MetricSlot(
            key = CIRCUIT_PREFIX + index,
            nameRes = R.string.slot_circuit_at,
            nameIndex = index + 1,
        )
    }

    /** 會話讀數位的上限，也是反查表的長度。 */
    const val BAY_MAX: Int = 8

    /** 賽道模式底部固定 4 格。 */
    const val CIRCUIT_BAY_COUNT: Int = 4

    /**
     * 參考稿的預設版面，按序號排。
     *
     * 規格開大之後會有格子取不到預設值——那些格子就是**空白**的，等使用者自己填。
     * 這是刻意的：憑空塞一個指標進去，使用者分不出哪些是他選的、哪些是系統補的。
     */
    val PortraitDefaults: List<String> = listOf(
        // 參考稿的 2×3
        "power", "heart", "cadence", "distance", "resistance", "calories",
        // 3×5 有 15 格，補到填得滿。豎屏與橫屏是**不同族**，所以這裡可以跟
        // dock 的預設重複——「已在其他位置顯示」只在同一族內比較。
        "speed", "target-speed", "duration", "pull",
        "avg-speed", "avg-power", "steps", "max-power", "work",
    )

    val SideDefaults: List<String> = listOf(
        // 單欄（現狀）：左欄時長、心率、踏頻、拉力；右欄功率、坡度、平均心率、做功。
        //
        // 側邊與 dock 是**同一族**，兩邊的預設值不能撞——撞了的話兩張卡會互相
        // 標成「已在其他位置顯示」。所以拉力與做功從 dock 的預設裡拿掉了，
        // 它們挪到這裡：側邊是會畫附加圖形的大卡，拉力正好有一條拉力曲線。
        "duration", "heart", "cadence", "pull",
        "power", "slope", "avg-heart", "work",
        // 兩欄那一版多出來的 8 格。划船機的桨次對比指標正好剩下這 8 個沒被
        // 側邊／dock 用過——兩欄擠成最密檔之後附加圖形畫不下，但雙條對比卡
        // 不靠附加圖形，密度再高也認得出兩條進度在比什麼。
        "peak-pull", "avg-pull", "drive-time", "recovery-time",
        "stroke-power", "drive-length", "stroke-distance", "stroke-work",
    )

    val DockDefaults: List<String> = listOf(
        "distance", "target-speed", "calories", "resistance",
        "avg-speed", "avg-power", "steps", "max-power",
        // 再兩個給 3×4 用。避開側邊那 8 個——同族撞了會互相標成
        // 「已在其他位置顯示」。填不滿的格子就留白，等使用者自己選：
        // 憑空塞一個指標進去，使用者分不出哪些是他選的、哪些是系統補的。
        "speed", "peak-heart",
    )

    /**
     * 平板會話讀數區的預設值，照參考稿 08 的中間八格 / 09 路線模式底部八格。
     *
     * 兩份稿的順序一致（08 是 2 欄 4 列、09 是 4 欄 2 列，但按閱讀順序展開是同一串），
     * 所以共用一份。
     */
    val BayDefaults: List<String> = listOf(
        "duration", "distance", "heart", "power",
        "cadence", "calories", "avg-speed", "max-power",
    )

    /** 賽道模式底部四格：時長、距離、本圈用時、最快單圈。 */
    val CircuitDefaults: List<String> = listOf("duration", "distance", "lap-time", "best-lap")

    /** 第 2 組指標，循環切換時顯示。同樣按序號取，取不到就空白。 */
    val SecondGroupPortrait: List<String> =
        listOf("avg-speed", "avg-heart", "avg-power", "peak-heart", "max-power", "steps")

    val SecondGroupDock: List<String> = listOf(
        "avg-speed", "avg-heart", "avg-power", "max-power",
        "peak-heart", "cadence", "resistance", "duration",
    )

    /**
     * 所有可能出現的資料位，供依 key 反查。
     *
     * 按**最大**規格生成：使用者可能先在 3×3 上改過某一格再切回 2×3，
     * 反查表少了那一格的話，切回去就找不到它。
     */
    val All: List<MetricSlot> =
        portrait(largest(MetricGridSpec.PortraitOptions)) +
            side(largest(MetricGridSpec.SideOptions)) +
            dock(largest(MetricGridSpec.DockOptions)) +
            bay(BAY_MAX) +
            CircuitBay

    /**
     * 某個資料位在指定組別下預設顯示哪個指標。
     *
     * 橫屏側邊的大卡不參與切組——它們是這台裝置的主要讀數，
     * 切到第 2 組時仍留在原位，只有豎屏格與底部 dock 會換一批。
     *
     * @param slotKey 資料位 key
     * @param groupIndex 0 為主組，1 為第 2 組
     * @return 指標 ID；沒有預設值（規格開大後多出來的格子）時為 null
     */
    fun defaultMetricId(slotKey: String, groupIndex: Int): String? {
        val index = indexOf(slotKey) ?: return null
        return when {
            isPortrait(slotKey) ->
                if (groupIndex == 0) PortraitDefaults.getOrNull(index)
                else SecondGroupPortrait.getOrNull(index)

            isDock(slotKey) ->
                if (groupIndex == 0) DockDefaults.getOrNull(index)
                else SecondGroupDock.getOrNull(index)

            isBay(slotKey) -> BayDefaults.getOrNull(index)

            isCircuitBay(slotKey) -> CircuitDefaults.getOrNull(index)

            else -> SideDefaults.getOrNull(index)
        }
    }

    /** 從 key 取序號。 */
    fun indexOf(slotKey: String): Int? = slotKey.substringAfterLast(KEY_SEPARATOR).toIntOrNull()

    /** 這個 key 是不是豎屏那一族。 */
    fun isPortrait(slotKey: String): Boolean = slotKey.startsWith(PORTRAIT_PREFIX)

    /** 這個 key 是不是 dock 那一族。 */
    fun isDock(slotKey: String): Boolean = slotKey.startsWith(DOCK_PREFIX)

    /** 這個 key 是不是橫屏側邊那一組。 */
    fun isSide(slotKey: String): Boolean = slotKey.startsWith(SIDE_PREFIX)

    /** 這個 key 是不是平板會話讀數區那一族。 */
    fun isBay(slotKey: String): Boolean = slotKey.startsWith(BAY_PREFIX)

    /** 這個 key 是不是賽道模式底部那一族。 */
    fun isCircuitBay(slotKey: String): Boolean = slotKey.startsWith(CIRCUIT_PREFIX)

    /** 候選規格裡格數最多的那個。 */
    private fun largest(options: List<MetricGridSpec>): MetricGridSpec =
        options.maxBy { it.slotCount }

    private const val SIDE_PREFIX = "land-side-"
    private const val PORTRAIT_PREFIX = "portrait-"
    private const val DOCK_PREFIX = "land-dock-"
    private const val BAY_PREFIX = "bay-"
    private const val CIRCUIT_PREFIX = "circuit-"
    private const val KEY_SEPARATOR = "-"
}

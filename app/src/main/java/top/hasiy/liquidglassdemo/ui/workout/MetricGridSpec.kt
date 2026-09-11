package top.hasiyliquidglassdemo.ui.workout

import androidx.compose.runtime.Immutable

/**
 * 指標網格的規格：幾行幾列。
 *
 * 版面不再寫死。豎屏那一組與橫屏 dock 各自可選一種規格，格數 = [rows] × [columns]。
 *
 * 為什麼要同時存行與列：2×3 和 3×2 都是 6 格，但一個是 3 欄 2 列、一個是 2 欄 3 列，
 * 只存格數分不出來。[MetricGrid] 吃的是 [columns]，行數由 `chunked` 自然得出——
 * 只要生成的資料位剛好是 [slotCount] 個就對得上。
 *
 * @param rows 行數
 * @param columns 每行幾格
 */
@Immutable
data class MetricGridSpec(val rows: Int, val columns: Int) {

    /** 這個規格一共幾格。 */
    val slotCount: Int get() = rows * columns

    /** 顯示用的名字，例如「2 × 3」。 */
    val label: String get() = "$rows × $columns"

    /**
     * 這個規格該用哪一檔卡片密度。
     *
     * 格子越窄，卡片裡的字要跟著收——3×5 的每格只有 ~140dp 寬還很扁，
     * 用豎屏那一檔字號會直接撐破。
     */
    val density: MetricCardDensity
        get() = when {
            columns >= DENSE_COLUMNS -> MetricCardDensity.DENSE
            columns >= COMPACT_COLUMNS -> MetricCardDensity.COMPACT
            else -> MetricCardDensity.REGULAR
        }

    companion object {
        /**
         * 豎屏那一組可選的規格。
         *
         * 4 欄與 5 欄在手機豎屏上每格只有 60～80dp 寬，讀數會收到最密那一檔
         * （見 [density]）——這是使用者自己選的取捨：一屏看更多，還是看得更清楚。
         */
        val PortraitOptions: List<MetricGridSpec> = listOf(
            MetricGridSpec(2, 2),
            MetricGridSpec(2, 3),
            MetricGridSpec(2, 4),
            MetricGridSpec(2, 5),
            MetricGridSpec(3, 2),
            MetricGridSpec(3, 3),
            MetricGridSpec(3, 4),
            MetricGridSpec(3, 5),
        )

        /** 橫屏 dock 可選的規格。 */
        val DockOptions: List<MetricGridSpec> = listOf(
            MetricGridSpec(2, 4),
            MetricGridSpec(3, 4),
            MetricGridSpec(2, 5),
            MetricGridSpec(3, 5),
        )

        /**
         * 橫屏側邊可選的規格：行數固定 [SIDE_FIXED_ROWS]，只有欄數能選。
         *
         * 側邊跟 dock／豎屏不一樣——它兩側要各自跟儀表那一行等高，開放行數只會讓
         * 「同一個高度分幾格」這件事變得無法預期。使用者真正想要的取捨是欄數：
         * 一欄時每格更高、能塞附加圖形（拉力曲線、心率波形）；兩欄時每側讀數翻倍，
         * 但格子變窄，字號要收到最密那一檔，附加圖形也畫不下了。
         *
         * 兩欄不是把側邊的**總寬度**加倍——那會把儀表擠到只剩一條縫。而是在同一塊
         * 側邊寬度裡切成兩條窄欄，見螢幕層 `LandscapeLayout` 的說明。
         */
        val SideOptions: List<MetricGridSpec> = listOf(
            MetricGridSpec(SIDE_FIXED_ROWS, 1),
            MetricGridSpec(SIDE_FIXED_ROWS, 2),
        )

        /** 參考稿的豎屏版面：2 列 3 欄。候選排序改過，不能再靠 `first()`。 */
        val PortraitDefault: MetricGridSpec = MetricGridSpec(2, 3)

        /** 參考稿的 dock 版面：2 列 4 欄。 */
        val DockDefault: MetricGridSpec = MetricGridSpec(2, 4)

        /** 側邊預設：單欄，即現狀。 */
        val SideDefault: MetricGridSpec = MetricGridSpec(SIDE_FIXED_ROWS, 1)

        /** 側邊固定的行數，須與 [MetricSlots.SIDE_ROWS] 保持一致。 */
        private const val SIDE_FIXED_ROWS = 4

        /**
         * 從 `"行x列"` 還原。
         *
         * 規格要跟著 `rememberSaveable` 進 Bundle，用字串是最省事的表示。
         * 認不出來或不在候選裡就退回預設——存檔是上一版寫的、候選改過了，
         * 這時給一個能用的版面比崩掉好。
         *
         * @param encoded [encode] 的輸出
         * @param options 允許的候選
         * @param fallback 認不出來時用哪一個
         */
        fun decode(
            encoded: String?,
            options: List<MetricGridSpec>,
            fallback: MetricGridSpec,
        ): MetricGridSpec {
            val parts = encoded?.split(SEPARATOR) ?: return fallback
            if (parts.size != 2) return fallback
            val rows = parts[0].toIntOrNull() ?: return fallback
            val columns = parts[1].toIntOrNull() ?: return fallback
            val spec = MetricGridSpec(rows, columns)
            return if (spec in options) spec else fallback
        }

        /** 存檔用的表示。 */
        fun encode(spec: MetricGridSpec): String = "${spec.rows}$SEPARATOR${spec.columns}"

        private const val SEPARATOR = "x"
        private const val COMPACT_COLUMNS = 3
        private const val DENSE_COLUMNS = 5
    }
}

/**
 * 指標卡的密度檔。
 *
 * 原本只有「常規／緊湊」兩檔，那是為參考稿的兩種版面（豎屏 3 欄、dock 4 欄）配的。
 * 網格可調到 5 欄之後需要第三檔：格子只有 ~140dp 寬，緊湊檔的字號還是會撐破。
 */
enum class MetricCardDensity {
    /** 豎屏 2 欄。 */
    REGULAR,

    /** 豎屏 3 欄、dock 4 欄。 */
    COMPACT,

    /** dock 5 欄。 */
    DENSE,
    ;

    /** dock 那種「一整塊面板 + 分隔線」的排法都算緊湊，不再各自帶底。 */
    val isCompact: Boolean get() = this != REGULAR
}

/**
 * 按密度檔挑一個值。
 *
 * 三檔各給一個字號／內距，比在每個用點寫 `when` 短得多，也不會漏掉一檔。
 *
 * @param regular 常規檔的值
 * @param compact 緊湊檔的值
 * @param dense 最密檔的值
 */
fun <T> MetricCardDensity.pick(regular: T, compact: T, dense: T): T = when (this) {
    MetricCardDensity.REGULAR -> regular
    MetricCardDensity.COMPACT -> compact
    MetricCardDensity.DENSE -> dense
}

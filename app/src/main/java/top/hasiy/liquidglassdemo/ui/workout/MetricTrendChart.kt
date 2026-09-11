package top.hasiyliquidglassdemo.ui.workout

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import top.hasiy.designsystem.GlassConfig
import top.hasiy.designsystem.accentToneColor
import top.hasiy.designsystem.dangerColor
import top.hasiy.designsystem.isLightSurface
import top.hasiy.designsystem.lineColor
import top.hasiy.designsystem.mutedContentColor
import kotlin.math.abs

/**
 * 指標卡上的趨勢圖。
 *
 * 對應參考稿的 `.metric-trend`：兩條底格線、一塊淡填充、一條線，末端一個點。
 * 只在**橫屏的第 2 組**出現——那一組全是平均值與極值，看趨勢才有意義；主組是實時
 * 讀數，每張卡再掛一條線會把版面吵死。豎屏那一組參考稿也沒畫：三欄下每格只有
 * ~110dp 寬，文字與圖擠在一起兩樣都讀不清。
 *
 * 每個指標的形狀是**照參考稿抄的**（見 [TREND_SAMPLES]），不是算出來的：
 * 阻力是一級一級的階梯、心率是鋸齒、踏頻是正弦、時長是單調上升——形狀本身就在
 * 說明這個量怎麼變化，用同一條隨機曲線套上去等於把這個資訊丟了。
 *
 * 參考稿沒給的指標（使用者可以把任何指標換進第 2 組）回落到 [trendSeries]：
 * 由 ID 產生一條確定的線，同一個指標每次畫出來都一樣。
 *
 * @param metricId 指標 ID，決定線的形狀與配色
 * @param config 玻璃主題參數
 * @param modifier 外部修飾符
 */
@Composable
fun MetricTrendChart(metricId: String, config: GlassConfig, modifier: Modifier = Modifier) {
    // 心率系走危險色，與參考稿的 .metric-trend.heart 一致。
    //
    // 其餘指標在**淺色屏上是中性灰**而不是強調色：參考稿 nordic 把 trend-line 覆寫成
    // #6f6f6f。淺色配色裡強調色只留給「可調節」與「達標」這類語義，一排八張卡
    // 全掛上綠線的話，那個語義就被稀釋掉了。
    val heart = metricId.contains("heart")
    val light = config.isLightSurface
    val lineColor = when {
        heart -> config.dangerColor
        light -> config.mutedContentColor
        else -> config.accentToneColor
    }
    val areaAlpha = if (heart) HEART_AREA_ALPHA else AREA_ALPHA
    val sample = TREND_SAMPLES[metricId]

    Canvas(modifier) {
        // 兩條底格線，位置照參考稿的 y=12 / y=25（viewBox 高 36）
        GRID_LINES.forEach { y ->
            drawLine(
                color = config.lineColor,
                start = Offset(0f, size.height * (y / VIEW_H)),
                end = Offset(size.width, size.height * (y / VIEW_H)),
                strokeWidth = 1f,
            )
        }

        // 座標統一在參考稿的 viewBox 空間裡算，最後按實際大小縮放。
        // SVG 與 Compose 的 y 都是往下為正，所以不必翻轉。
        val points: List<Offset>
        val smooth: Boolean
        if (sample != null) {
            points = sample.points.map { (x, y) ->
                Offset(x / VIEW_W * size.width, y / VIEW_H * size.height)
            }
            smooth = sample.smooth
        } else {
            val series = trendSeries(metricId)
            val stepX = if (series.size > 1) size.width / (series.size - 1) else size.width
            points = series.mapIndexed { index, value ->
                Offset(index * stepX, size.height * (1f - value))
            }
            smooth = true
        }
        if (points.isEmpty()) return@Canvas

        val line = if (smooth) {
            smoothPath(points)
        } else {
            Path().apply {
                points.forEachIndexed { index, point ->
                    if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                }
            }
        }
        val area = Path().apply {
            addPath(line)
            lineTo(points.last().x, size.height)
            lineTo(points.first().x, size.height)
            close()
        }

        drawPath(area, lineColor.copy(alpha = areaAlpha))
        drawPath(
            path = line,
            color = lineColor,
            style = Stroke(width = LINE_WIDTH.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        // 末端那個點就是「現在」。參考稿還給它加了發光，淺色系覆寫掉了；
        // 這裡兩邊都不加——發光在小尺寸上只會讓點看起來髒。
        drawCircle(color = lineColor, radius = DOT_RADIUS.toPx(), center = points.last())
    }
}

/**
 * 一條趨勢線的樣例。
 *
 * @param smooth 走平滑曲線還是折線。平均值那幾條是連續量、參考稿用的是貝茲；
 *   心率、階梯這些必須是折線，抹圓了就不是那個形狀了
 * @param points 參考稿 viewBox（120 × 36）裡的座標
 */
private data class TrendSample(
    val smooth: Boolean,
    val points: List<Pair<Float, Float>>,
)

/**
 * 參考稿第 2 組那八條趨勢線。
 *
 * 座標照抄 `.metric-trend` 的 `d`。原稿用 `C` / `S` 畫的三條在這裡只留關鍵點、
 * 交給 Catmull-Rom 補控制點——兩者的差別在 47% 卡寬的尺寸下看不出來，
 * 但這樣改點就不用同時改六個控制點。
 *
 * 踏頻那條額外補了波峰谷：原稿的 `S` 命令把起伏藏在控制點裡（`C10 15 20 15 30 23`），
 * 只留穿過 y=23 的那幾個點會畫成一條直線。
 */
private val TREND_SAMPLES: Map<String, TrendSample> = mapOf(
    // 平均速度：一路緩升
    "avg-speed" to TrendSample(
        smooth = true,
        points = listOf(0f to 28f, 34f to 23f, 64f to 20f, 96f to 16f, 120f to 11f),
    ),
    // 平均心率：鋸齒。心率本來就是一跳一跳的，抹成曲線就不像心率了
    "avg-heart" to TrendSample(
        smooth = false,
        points = listOf(
            0f to 24f, 18f to 24f, 25f to 19f, 31f to 27f, 38f to 8f, 46f to 30f,
            54f to 20f, 61f to 24f, 78f to 24f, 86f to 16f, 94f to 28f, 103f to 10f,
            112f to 25f, 120f to 20f,
        ),
    ),
    // 平均功率：緩升，中段有一次回落
    "avg-power" to TrendSample(
        smooth = true,
        points = listOf(0f to 29f, 34f to 24f, 66f to 18f, 88f to 20f, 120f to 8f),
    ),
    // 最大功率：平緩往上，中間一記尖峰——那一下就是「最大」的來處
    "max-power" to TrendSample(
        smooth = false,
        points = listOf(
            0f to 29f, 19f to 27f, 34f to 25f, 47f to 23f, 58f to 7f, 67f to 22f,
            83f to 20f, 96f to 17f, 108f to 13f, 120f to 12f,
        ),
    ),
    "peak-heart" to TrendSample(
        smooth = false,
        points = listOf(
            0f to 26f, 20f to 25f, 31f to 23f, 40f to 26f, 49f to 9f, 58f to 28f,
            68f to 22f, 78f to 24f, 91f to 18f, 100f to 23f, 109f to 6f, 120f to 20f,
        ),
    ),
    // 踏頻：正弦。踩踏是週期動作，穩定的踏頻畫出來就是這個樣子
    "cadence" to TrendSample(
        smooth = true,
        points = listOf(
            0f to 23f, 15f to 16f, 30f to 23f, 45f to 30f, 60f to 23f,
            75f to 16f, 90f to 23f, 105f to 29f, 120f to 21f,
        ),
    ),
    // 阻力：階梯。它是**一級一級**調上去的，畫成斜線會讀成連續變化
    "resistance" to TrendSample(
        smooth = false,
        points = listOf(
            0f to 29f, 25f to 29f, 25f to 25f, 51f to 25f, 51f to 20f, 78f to 20f,
            78f to 16f, 101f to 16f, 101f to 10f, 120f to 10f,
        ),
    ),
    // 運動時長：單調上升。它只會往前走，任何回落都是錯的
    "duration" to TrendSample(
        smooth = false,
        points = listOf(
            0f to 31f, 20f to 28f, 40f to 25f, 60f to 21f, 80f to 17f,
            100f to 12f, 120f to 7f,
        ),
    ),
)

/**
 * 由指標 ID 產生一組 0..1 的序列。
 *
 * 參考稿沒給樣例的指標走這條：用 ID 的雜湊當種子，同一個指標每次都是同一條線——
 * 趨勢圖每次重繪都換形狀，看起來像數據在亂跳。
 *
 * @param metricId 指標 ID
 * @return [POINT_COUNT] 個 0..1 的值
 */
internal fun trendSeries(metricId: String): List<Float> {
    var seed = metricId.hashCode()
    return List(POINT_COUNT) { index ->
        // 線性同餘，只要能穩定重現就夠了，不需要統計品質
        seed = seed * 1103515245 + 12345
        val noise = (abs(seed / 65536) % 1000) / 1000f
        // 疊一個緩慢上升的底：參考稿每條趨勢線都是往右上走的
        val rise = index.toFloat() / (POINT_COUNT - 1)
        (BASE + rise * RISE_WEIGHT + noise * NOISE_WEIGHT).coerceIn(0.05f, 0.95f)
    }
}

/** 參考稿 `.metric-trend` 的 viewBox。 */
private const val VIEW_W = 120f
private const val VIEW_H = 36f

/** 兩條底格線的 y，照參考稿的 `M0 12H120M0 25H120`。 */
private val GRID_LINES = listOf(12f, 25f)

/**
 * 兜底序列的取樣點數與抖動幅度。
 *
 * 8 個點、小噪聲才看得出「趨勢」而不是「雜訊」。
 */
private const val POINT_COUNT = 8
private const val BASE = 0.2f
private const val RISE_WEIGHT = 0.5f
private const val NOISE_WEIGHT = 0.14f

/** 填充濃度：參考稿非心率 10%、心率 8%。 */
private const val AREA_ALPHA = 0.1f
private const val HEART_AREA_ALPHA = 0.08f
private val LINE_WIDTH = 1.5.dp
private val DOT_RADIUS = 2.dp

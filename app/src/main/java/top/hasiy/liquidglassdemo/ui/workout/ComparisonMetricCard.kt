package top.hasiyliquidglassdemo.ui.workout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.hasiy.designsystem.GlassConfig
import top.hasiy.designsystem.LocalGlassConfig
import top.hasiy.designsystem.accentLightColor
import top.hasiy.designsystem.accentToneColor
import top.hasiy.designsystem.lineColor
import top.hasiy.designsystem.mutedContentColor
import top.hasiy.designsystem.panelColor
import top.hasiy.designsystem.screenContentColor
import top.hasiy.designsystem.tactileKeycap
import top.hasiy.designsystem.tokens.GlassVisualStyle
import top.hasiy.designsystem.trackColor
import top.hasiyliquidglassdemo.R

/**
 * 「本桨 vs 上一桨」的對比卡。
 *
 * 划船機的桨次指標單看一個數讀不出好壞——42 N 是進步還是退步，得跟上一桨比。
 * 所以左邊照常報數，右邊疊兩條橫進度：
 * ```
 * 上條 = 本桨（強調色）     下條 = 上一桨（強調色的淺階）
 * 貫穿兩條的豎線 = 歷史平均，當基準
 * ```
 *
 * 兩條同色系而不是換一個色相：它們是**同一個量的兩次取樣**，用兩個不相干的顏色
 * 會讀成兩件事。深淺分先後，亮的是現在。
 *
 * 豎線畫在兩條**之上**、貫穿整組高度：分別畫在每條裡的話就是兩個刻度，
 * 讀者得自己確認它們對齊；一根線跨過去才是「同一個基準」。
 *
 * 這一類卡在豎屏佔兩格寬（見 [MetricGrid] 的跨欄），橫屏按單格算——
 * 橫屏的格子本來就寬，一格放得下。
 *
 * @param metric 指標，[WorkoutMetric.comparison] 必須有值
 * @param value 本桨的顯示值
 * @param config 玻璃主題參數
 * @param modifier 外部修飾符
 * @param density 卡片密度檔
 * @param flat dock 裡的格子沒有自己的底，整塊面板由外層畫
 */
@Composable
fun ComparisonMetricCard(
    metric: WorkoutMetric,
    value: String,
    config: GlassConfig = LocalGlassConfig.current,
    modifier: Modifier = Modifier,
    density: MetricCardDensity = MetricCardDensity.REGULAR,
    flat: Boolean = false,
) {
    val comparison = metric.comparison ?: return
    val corner = metricCardCorner(config, density)
    val shape = RoundedCornerShape(corner)
    val surface = when {
        flat -> Modifier
        config.visualStyle == GlassVisualStyle.TACTILE -> Modifier.tactileKeycap(corner)
        else -> Modifier.clip(shape).background(config.panelColor).border(1.dp, config.lineColor, shape)
    }

    Row(
        modifier = modifier
            .then(surface)
            .padding(density.pick(CARD_PADDING, COMPACT_PADDING, DENSE_PADDING)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(density.pick(GAP, COMPACT_GAP, COMPACT_GAP)),
    ) {
        Column(modifier = Modifier.weight(TEXT_WEIGHT)) {
            // 標籤與數值都鎖單行、超出截斷：這一欄只佔卡寬的 42%，兩欄側邊
            // 再對半分之後往往不到卡寬的四分之一。四個字的標籤（「峰值拉力」
            // 這一類）在這個寬度下換行是常態，換行會把數值那一行擠出卡片高度、
            // 被 clip 切掉——這一格看起來就是「標籤斷成兩截、數字整個消失」。
            // 截斷雖然看不全標籤，至少數值留得住，而數值才是這張卡真正要給的。
            Text(
                text = stringResource(metric.labelRes),
                color = config.mutedContentColor,
                fontSize = density.pick(LABEL_SIZE, COMPACT_LABEL_SIZE, DENSE_LABEL_SIZE),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    color = config.screenContentColor,
                    fontSize = density.pick(VALUE_SIZE, COMPACT_VALUE_SIZE, DENSE_VALUE_SIZE),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.alignByBaseline(),
                )
                if (metric.unitRes != null) {
                    Text(
                        text = stringResource(metric.unitRes),
                        color = config.mutedContentColor,
                        fontSize = density.pick(UNIT_SIZE, COMPACT_UNIT_SIZE, DENSE_UNIT_SIZE),
                        maxLines = 1,
                        // 同指標卡：單位對數值的**基線**，不是盒底
                        modifier = Modifier.padding(start = 3.dp).alignByBaseline(),
                    )
                }
            }
        }

        ComparisonBars(
            current = value.toFloatOrNull() ?: 0f,
            comparison = comparison,
            config = config,
            density = density,
            modifier = Modifier.weight(BARS_WEIGHT),
        )
    }
}

/**
 * 兩條橫進度加一根基準線。
 *
 * @param current 本桨的值
 * @param comparison 量程、上一桨、歷史平均
 * @param config 玻璃主題參數
 * @param density 卡片密度檔
 * @param modifier 外部修飾符
 */
@Composable
private fun ComparisonBars(
    current: Float,
    comparison: MetricComparison,
    config: GlassConfig,
    density: MetricCardDensity,
    modifier: Modifier = Modifier,
) {
    val max = comparison.max.takeIf { it > 0f } ?: 1f
    val barHeight = density.pick(BAR_HEIGHT, COMPACT_BAR_HEIGHT, DENSE_BAR_HEIGHT)
    val description = stringResource(
        R.string.metric_compare_desc,
        formatBarValue(current),
        formatBarValue(comparison.previous),
    )

    Box(modifier = modifier.semantics { contentDescription = description }) {
        Column(verticalArrangement = Arrangement.spacedBy(BAR_GAP)) {
            Bar(
                fraction = current / max,
                color = config.accentToneColor,
                height = barHeight,
                config = config,
            )
            Bar(
                fraction = comparison.previous / max,
                color = config.accentLightColor,
                height = barHeight,
                config = config,
            )
        }
        // 基準線畫在兩條之上，貫穿整組高度。
        //
        // 用內容色而不是強調色：它不是「又一個成績」，是刻度。跟兩條同色系的話
        // 三者會讀成三次取樣。
        Canvas(modifier = Modifier.matchParentSize()) {
            val x = (comparison.reference / max).coerceIn(0f, 1f) * size.width
            drawLine(
                color = config.screenContentColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = REFERENCE_WIDTH.toPx(),
            )
        }
    }
}

/** 一條橫進度：軌道加填充。 */
@Composable
private fun Bar(
    fraction: Float,
    color: androidx.compose.ui.graphics.Color,
    height: androidx.compose.ui.unit.Dp,
    config: GlassConfig,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(BAR_CORNER))
            .background(config.trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(BAR_CORNER))
                .background(color),
        )
    }
}

/** 無障礙描述裡的數值：一位小數就夠，整數不拖一個 `.0`。 */
private fun formatBarValue(value: Float): String =
    if (value % 1f == 0f) value.toInt().toString() else "%.1f".format(value)

/**
 * 左邊文字與右邊兩條的寬度分配。
 *
 * 參考圖裡文字約占四成、條占六成，這裡是 46:54——比參考圖再給文字欄多留一點。
 * 兩欄側邊把卡寬再砍半之後，「126.3」這種三位整數的示例值在 42:58 下會被 DENSE
 * 那一檔的字級擠到裁切；46:54 加上下面 [DENSE_VALUE_SIZE] 收窄，剛好放得下，
 * 代價是進度條窄了一點，兩條的粗細差還是分得出來——這張卡存在的理由正是那個差。
 */
private const val TEXT_WEIGHT = 0.46f
private const val BARS_WEIGHT = 0.54f

private val CARD_PADDING = 9.dp
private val COMPACT_PADDING = 7.dp
private val DENSE_PADDING = 5.dp
private val GAP = 10.dp
private val COMPACT_GAP = 7.dp

private val LABEL_SIZE = 11.sp
private val COMPACT_LABEL_SIZE = 9.sp
private val DENSE_LABEL_SIZE = 8.sp
private val VALUE_SIZE = 20.sp
private val COMPACT_VALUE_SIZE = 17.sp
// 兩欄側邊那一檔比原本再收一號：那裡的卡片寬度又比一般 DENSE 場景窄了一半。
private val DENSE_VALUE_SIZE = 12.sp
private val UNIT_SIZE = 10.sp
private val COMPACT_UNIT_SIZE = 8.sp
private val DENSE_UNIT_SIZE = 7.sp

private val BAR_HEIGHT = 9.dp
private val COMPACT_BAR_HEIGHT = 7.dp
private val DENSE_BAR_HEIGHT = 5.dp
private val BAR_GAP = 4.dp
private val BAR_CORNER = 2.dp
private val REFERENCE_WIDTH = 1.5.dp

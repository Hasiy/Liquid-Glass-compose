package top.hasiyliquidglassdemo.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.hasiy.designsystem.GlassConfig
import top.hasiy.designsystem.LocalGlassConfig
import top.hasiy.designsystem.SevenSegmentText
import top.hasiy.designsystem.TACTILE_LAMP
import top.hasiy.designsystem.TACTILE_LAMP_HEIGHT
import top.hasiy.designsystem.TACTILE_LAMP_WIDTH
import top.hasiy.designsystem.accentToneColor
import top.hasiy.designsystem.lineColor
import top.hasiy.designsystem.mutedContentColor
import top.hasiy.designsystem.panelColor
import top.hasiy.designsystem.quietAccentColor
import top.hasiy.designsystem.screenContentColor
import top.hasiy.designsystem.tactileKeycap
import top.hasiy.designsystem.tokens.GlassVisualStyle
import top.hasiyliquidglassdemo.R

/**
 * 指標卡：標籤 + 數值 + 單位。
 *
 * 三種樣態對應參考稿的三個 class：
 * - 一般：`.metric`，中性底加一圈 line
 * - 可調節：`.metric.adjust`，強調色描邊與淡底，右上角一個「− ＋」提示
 * - 只能讀：`.metric.control-disabled`，整卡壓淡、不顯示 ± 提示、附一行原因
 *
 * 卡片只負責呈現，長按與點擊由外層的資料位處理。
 *
 * @param metric 要顯示的指標
 * @param value 顯示值，由呼叫端從 [WorkoutUiState.valueOf] 取；斷線時是最後值
 * @param config 玻璃主題參數
 * @param modifier 外部修飾符
 * @param adjustable 是否顯示可調節標記
 * @param readOnlyReason 只讀提示的原因；null 表示不顯示提示
 * @param density 密度檔。格子越窄字級與內距越小，見 [MetricCardDensity]
 * @param art 附加圖形；[MetricArt.NONE] 或非常規密度時不畫
 * @param trend 是否在右上角畫趨勢圖（第 2 組才有）
 * @param live 是否還在跑，交給附加圖形決定要不要轉中性色
 * @param flat 去掉自己的底與描邊。橫屏 dock 是**一整塊**面板，格子只靠分隔線分開，
 *   每格再畫一個卡片底就變成八張小卡貼在一起了
 */
@Composable
fun MetricCard(
    metric: WorkoutMetric,
    value: String,
    config: GlassConfig = LocalGlassConfig.current,
    modifier: Modifier = Modifier,
    adjustable: Boolean = false,
    readOnlyReason: ReadOnlyReason? = null,
    density: MetricCardDensity = MetricCardDensity.REGULAR,
    art: MetricArt = MetricArt.NONE,
    trend: Boolean = false,
    live: Boolean = true,
    flat: Boolean = false,
    stepRange: ControlRange? = null,
    onStep: (Int) -> Unit = {},
) {
    val locked = readOnlyReason != null
    val digital = config.visualStyle == GlassVisualStyle.DIGITAL
    val tactile = config.visualStyle == GlassVisualStyle.TACTILE
    // Digital 是低圓角的實色面板，Tactile 是凸起膠帽，兩者都不是「玻璃卡換個顏色」
    val corner = metricCardCorner(config, density)
    val shape = RoundedCornerShape(corner)
    // 可調節的格子**不套綠框**。
    //
    // 參考稿原本是 `inset 0 0 0 1.5px rgba(lime,.45)`，但一排讀數裡只有一格被綠色
    // 圈起來，那一格會從版面上跳出來——使用者要的是「和別的格子一樣，只是多了
    // 一顆燈和一組按鍵」。「這一格能調」由右上角那顆指示燈負責說，框不必再說一次。
    //
    // 只讀時的虛線框仍然要（見下面的 dashedBorder）：那是「本來能調、現在不能」，
    // 跟普通讀數格不是同一件事，得有個記號。
    val borderColor = config.lineColor
    // 卡片底就是 panel 那一階，可調節的格子也一樣——**不再疊淡綠底**。
    //
    // 參考稿給可調節的格子鋪了一層極淡的強調色（Nordic 的 `#e7efe9`），但一排
    // 讀數裡只有一格顏色不同，那一格就從版面上跳出來。使用者要的是「和別的格子
    // 一樣，只是多了一顆燈和一組按鍵」：能不能調由右上角那顆指示燈負責說，
    // 底色不必再說一次。
    //
    // 原本照抄參考稿基礎主題的 `rgba(255,255,255,.055)` 疊在內容色上，
    // 那組數字只在**深色屏**成立：白色低透明度疊在深底上是提亮。換到淺色配色，
    // screenContent 變成近黑，同一組 alpha 就成了「往下壓一階」，
    // 卡片比屏底還暗，跟參考稿 `--panel` 高於 `--screen` 的方向正好相反。
    //
    // panel token 本身就是那層漸層的等效實色（深色配色 #171B1C 對應
    // 5.5% 白疊在 #070909 上），讀它 8 組配色都對。
    val fill = SolidColor(config.panelColor)

    val surface: Modifier = when {
        // dock 裡的格子沒有自己的底，整塊面板由外層畫——可調節的那一格**也一樣**。
        // 它跟旁邊的讀數格看起來就該是同一種格子，差別只在右上角那顆燈和那組按鍵。
        flat -> Modifier
        // 膠帽自帶漸層、描邊與內外光影，不再疊玻璃卡的那層底
        tactile -> Modifier.tactileKeycap(corner)
        else -> Modifier.clip(shape).background(fill)
    }

    Column(
        // alpha 必須排在繪製修飾符之前：它開的是一個圖層，只蓋得住右邊的內容，
        // 放在最後的話底色與描邊都不會跟著變淡
        modifier = modifier
            .alpha(if (locked) LOCKED_ALPHA else 1f)
            .then(surface)
            // 參考稿的 .control-disabled 是 border-style: dashed。
            // Modifier.border 畫不出虛線，只能自己描一圈。
            .then(
                when {
                    flat -> Modifier
                    locked -> Modifier.dashedBorder(borderColor, corner)
                    // 膠帽自帶描邊，再描一圈會把立體感壓平。可調節的也一樣——
                    // Tactile 用指示燈表達可調，套一圈綠框就變成兩套語彙了。
                    tactile -> Modifier
                    else -> Modifier.border(width = 1.dp, color = borderColor, shape = shape)
                }
            )
            .padding(density.pick(CARD_PADDING, COMPACT_PADDING, DENSE_PADDING)),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = stringResource(metric.labelRes),
                color = config.mutedContentColor,
                fontSize = density.pick(LABEL_SIZE, COMPACT_LABEL_SIZE, DENSE_LABEL_SIZE),
            )
            // 標題行的行尾：不能調時放原因，Tactile 下放指示燈。
            // 指示燈和「− ＋」是**兩個**標記，參考稿兩者並存——燈說「這顆是可調的」，
            // ± 說「怎麼調」，不是二選一。
            when {
                readOnlyReason != null -> Text(
                    text = stringResource(readOnlyReason.labelRes),
                    // 參考稿的 .read-only 用的是中性灰，不是警示色：不能調是一個
                    // 事實陳述，不是需要處理的錯誤
                    color = config.mutedContentColor,
                    fontSize = density.pick(HINT_SIZE, COMPACT_HINT_SIZE, DENSE_HINT_SIZE),
                )

                // 實體面板上「這顆能按」靠的是指示燈，參考稿的
                // `.theme-tactile .metric.adjust::before` 就是這個
                adjustable && tactile -> TactileLamp(
                    // 參考稿的 `top: 8px; right: 10px` 是相對卡片邊緣量的，
                    // 這裡是排在標題行尾，再往左下各推一點才對得上
                    Modifier.padding(top = LAMP_NUDGE, end = LAMP_NUDGE)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            val valueSize = density.pick(VALUE_SIZE, COMPACT_VALUE_SIZE, DENSE_VALUE_SIZE)
            // 兩種「不是一個數」的顯示：沒有數據畫一條橫線，手動阻力寫「手動」。
            // 兩者都不帶單位——「– bpm」和「手動 級」都是病句。
            val noData = value == DISPLAY_NO_DATA
            val manual = value == DISPLAY_MANUAL
            val text = when {
                noData -> stringResource(R.string.metric_no_data)
                manual -> stringResource(R.string.metric_resistance_manual)
                else -> value
            }
            // 單位跟數值是**基線**對齊，不是盒底對齊：參考稿的 `.metric strong`
            // 與 `.metric em` 是同一行的行內元素。Compose 的 Alignment.Bottom 對的是
            // 盒底，大小差一倍的兩個盒子底邊齊平時，小字看起來是浮在中間的。
            // 七段數碼管是自繪的、沒有基線，那條分支只能退回盒底對齊。
            if (digital && !noData && !manual) {
                SevenSegmentText(text = text, color = config.screenContentColor, fontSize = valueSize)
            } else {
                Text(
                    text = text,
                    // 「手動」是兩個漢字，照數字的字號排會撐破格子；橫線同理，
                    // 一條 30sp 的破折號在卡片裡大得不像佔位符
                    fontSize = if (noData || manual) valueSize * SPECIAL_VALUE_SCALE else valueSize,
                    color = if (noData) config.mutedContentColor else config.screenContentColor,
                    modifier = Modifier.alignByBaseline(),
                )
            }
            if (metric.unitRes != null && !noData && !manual) {
                Text(
                    text = stringResource(metric.unitRes),
                    color = config.mutedContentColor,
                    fontSize = density.pick(UNIT_SIZE, COMPACT_UNIT_SIZE, DENSE_UNIT_SIZE),
                    modifier = Modifier
                        .padding(start = 3.dp)
                        .then(
                            if (digital) Modifier.padding(bottom = 2.dp)
                            else Modifier.alignByBaseline()
                        ),
                )
            }
            // 參考稿的 .mini-step 是 `float: right`，落在**數值這一行**的行尾，
            // 不是標題行。只讀時整個拿掉——留著會讓人以為還能調。
            //
            // 給了量程就換成真按鍵：橫屏 dock 的格子併兩欄之後放得下一組鍵，
            // 就地調比「開浮層、調、關浮層」少兩次分心。卡片的底、框與右上角
            // 那顆燈都不變——樣式照舊，只是那對小小的「− ＋」從提示變成能按的。
            if (adjustable && !locked) {
                if (stepRange != null) {
                    // 刻度擺在數值與按鍵**之間**，佔掉中間剩下的寬度，不另起一行。
                    //
                    // 一開始放在 Column 的下一行，結果整條被裁掉：dock 的格子高度是
                    // 固定的（72/64/56dp），標籤行加上按鍵那 44dp 的命中區就已經吃滿，
                    // 再多一行根本畫不出來。擺進同一行是唯一不動格高的做法。
                    AdjustScale(
                        range = stepRange,
                        value = value.toFloatOrNull() ?: stepRange.min,
                        config = config,
                        density = density,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = SCALE_GAP)
                            // 這一行是按基線對齊的（數值與單位要對基線），刻度沒有
                            // 基線可對，得自己居中——不然它會貼在行底。
                            .align(Alignment.CenterVertically),
                    )
                    AdjustKeys(
                        range = stepRange,
                        onStep = onStep,
                        metricLabel = stringResource(metric.labelRes),
                        unit = metric.unitRes?.let { stringResource(it) }.orEmpty(),
                        config = config,
                        density = density,
                        locked = false,
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.metric_step_hint),
                        // 說明性小字，淺色配色下整批降回中性——見 quietAccentColor
                        color = config.quietAccentColor,
                        fontSize = density.pick(STEP_HINT_SIZE, COMPACT_LABEL_SIZE, DENSE_LABEL_SIZE),
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            if (trend) {
                Spacer(Modifier.weight(1f))
                MetricTrendChart(
                    metricId = metric.id,
                    config = config,
                    // 參考稿的 .metric-trend 佔卡片寬度的 47%、高 39px。
                    // 高度跟著密度收：dock 開到 3×4 時每格只有 ~56dp 高，
                    // 39dp 的圖會把數值頂出格子。
                    modifier = Modifier
                        .fillMaxWidth(TREND_WIDTH_FRACTION)
                        .height(density.pick(TREND_HEIGHT, TREND_HEIGHT_COMPACT, TREND_HEIGHT_DENSE)),
                )
            }
        }

        // 附加圖形只在橫屏側邊的大卡上畫，小卡塞不下
        if (art != MetricArt.NONE && density == MetricCardDensity.REGULAR) {
            MetricArtwork(
                art = art,
                config = config,
                live = live,
                modifier = Modifier.padding(top = 5.dp),
            )
        }
    }
}

/**
 * 虛線圓角描邊。
 *
 * @param color 線色
 * @param corner 圓角半徑，要與卡片本身一致
 */
private fun Modifier.dashedBorder(color: Color, corner: Dp): Modifier = drawBehind {
    val width = 1.dp.toPx()
    val radius = CornerRadius(corner.toPx())
    // 描邊是沿著路徑置中畫的，往內縮半個線寬才不會被 clip 切掉一半
    val inset = width / 2f
    drawRoundRect(
        color = color,
        topLeft = Offset(inset, inset),
        size = Size(size.width - width, size.height - width),
        cornerRadius = radius,
        style = Stroke(
            width = width,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(DASH_ON.toPx(), DASH_OFF.toPx())),
        ),
    )
}

/** 只讀提示的文案。兩種原因分開講，混用會讓人搞不清是裝置不行還是現在不行。 */
private val ReadOnlyReason.labelRes: Int
    get() = when (this) {
        ReadOnlyReason.DEVICE_READ_ONLY -> R.string.metric_read_only_device
        ReadOnlyReason.TEMPORARILY_LOCKED -> R.string.metric_read_only_locked
    }

/**
 * 指標卡的圓角。
 *
 * 卡片本體、長按進度環、編輯描邊、空佔位都得取**同一個**值——各自寫死的話，
 * Digital 底下卡片是 7dp 而環是 16dp，長按時會冒出一圈對不上的大圓角。
 *
 * @param config 玻璃主題參數
 * @param density 密度檔
 */
@Composable
fun metricCardCorner(
    config: GlassConfig = LocalGlassConfig.current,
    density: MetricCardDensity = MetricCardDensity.REGULAR,
): Dp =
    when {
        // Digital 是低圓角的實色面板
        config.visualStyle == GlassVisualStyle.DIGITAL -> DIGITAL_CORNER
        density == MetricCardDensity.DENSE -> DENSE_CORNER
        density.isCompact -> COMPACT_CORNER
        else -> CARD_CORNER
    }

/** 空的資料位：抽屜還沒指派指標時的佔位。 */
@Composable
fun EmptyMetricCard(
    config: GlassConfig = LocalGlassConfig.current,
    modifier: Modifier = Modifier,
    editing: Boolean = false,
) {
    // 不在編輯版面時**什麼都不畫**。
    //
    // 空位不是一個「有邊界的東西」——它就是這一格沒東西。畫一圈描邊會讓人以為
    // 那是一張壞掉的卡片，而且刪掉一格之後那圈框還留在原地，看起來像沒刪成功。
    if (!editing) return

    // 編輯版面時才給一個很淡的虛線占位：這時使用者需要知道「這裡還能填」，
    // 不畫出來就沒有可點的目標。
    // 圓角要在 drawBehind **外面**取：metricCardCorner 是個 @Composable，
    // 繪製 lambda 裡調不了。
    val corner = metricCardCorner(config)
    val shape = RoundedCornerShape(corner)
    Box(
        modifier = modifier.clip(shape).drawBehind {
            drawRoundRect(
                color = config.lineColor,
                cornerRadius = CornerRadius(corner.toPx()),
                style = Stroke(
                    width = EMPTY_STROKE.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(EMPTY_DASH.toPx(), EMPTY_DASH.toPx())
                    ),
                ),
            )
        }
    )
}

/** 編輯版面時空位那圈虛線。 */
private val EMPTY_STROKE = 1.dp
private val EMPTY_DASH = 3.dp

/** 可調節卡片的描邊與底色濃度，取自參考稿的 `.metric.adjust` */

/**
 * flat 的可調節格子，底與框往內縮多少。
 *
 * dock 的格子之間只隔一條分隔線，不縮的話綠框會貼在分隔線上。
 */

/** 一般卡片的底：在屏底上鋪一層極淡的內容色，做出「比屏底高一階」 */

/** 只讀卡片的整體透明度，與參考稿的 `opacity: .5` 一致 */
private const val LOCKED_ALPHA = 0.5f

/** Digital 是低圓角的實色面板，參考稿 `border-radius: 7px`。 */
private val DIGITAL_CORNER = 7.dp

private val CARD_CORNER = 16.dp
private val COMPACT_CORNER = 12.dp
private val CARD_PADDING = 9.dp
private val COMPACT_PADDING = 7.dp

/** 參考稿 .metric-trend：寬 47%、高 39px。 */
private const val TREND_WIDTH_FRACTION = 0.47f
private val TREND_HEIGHT = 39.dp
private val TREND_HEIGHT_COMPACT = 32.dp
private val TREND_HEIGHT_DENSE = 24.dp

/** 指示燈相對標題行再往左下推的量。 */
private val LAMP_NUDGE = 3.dp

private val DASH_ON = 3.dp
private val DASH_OFF = 3.dp

private val LABEL_SIZE = 11.sp
private val VALUE_SIZE = 22.sp
private val UNIT_SIZE = 10.sp
/** 刻度條與兩側（數值、按鍵）之間的間距。 */
private val SCALE_GAP = 8.dp

private val STEP_HINT_SIZE = 14.sp

/** 只讀提示比標籤再小一號，參考稿是 6px 對 7px */
private val HINT_SIZE = 9.sp
private val COMPACT_HINT_SIZE = 8.sp
private val COMPACT_LABEL_SIZE = 9.sp
private val COMPACT_VALUE_SIZE = 17.sp
private val COMPACT_UNIT_SIZE = 8.sp

/**
 * 最密那一檔：dock 開到 5 欄時每格只有 ~140dp 寬還很扁。
 *
 * 緊湊檔的 17sp 讀數在這個寬度下，配上單位就會換行；內距也要再收，
 * 否則三行內容撐不進格子的高度。
 */
private val DENSE_LABEL_SIZE = 8.sp
private val DENSE_VALUE_SIZE = 14.sp
private val DENSE_UNIT_SIZE = 7.sp
private val DENSE_HINT_SIZE = 7.sp
private val DENSE_PADDING = 5.dp
private val DENSE_CORNER = 10.dp

/**
 * 「－」與「手動」相對數字字號的比例。
 *
 * 這兩個都不是數字：漢字照 22sp 排會撐破格子，一條破折號照 22sp 畫也大得
 * 不像佔位符。
 */
private const val SPECIAL_VALUE_SCALE = 0.72f

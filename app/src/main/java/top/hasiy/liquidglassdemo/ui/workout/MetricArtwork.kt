package top.hasiyliquidglassdemo.ui.workout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.hasiy.designsystem.GlassConfig
import top.hasiy.designsystem.LocalGlassConfig
import top.hasiy.designsystem.accentToneColor
import top.hasiy.designsystem.dangerColor
import top.hasiy.designsystem.lineColor
import top.hasiy.designsystem.mutedContentColor
import top.hasiy.designsystem.quietAccentColor
import top.hasiyliquidglassdemo.R

/**
 * 指標卡的附加圖形。
 *
 * 對應參考稿橫屏側邊四張大卡上的裝飾：時長的「自動記錄中」、心率的脈搏波、
 * 功率的四段條、坡度的坡形。只有橫屏側卡會畫，底部 dock 的小卡不畫——
 * 見 [MetricSlot.drawsArt]。
 *
 * @param art 要畫哪一種
 * @param config 玻璃主題參數
 * @param live 是否還在跑；凍結時圖形轉為中性色
 * @param modifier 外部修飾符
 */
@Composable
fun MetricArtwork(
    art: MetricArt,
    config: GlassConfig = LocalGlassConfig.current,
    live: Boolean,
    modifier: Modifier = Modifier,
) {
    when (art) {
        MetricArt.NONE -> Unit
        MetricArt.DURATION -> StateLine(
            textRes = R.string.card_state_recording,
            tint = if (live) config.quietAccentColor else config.mutedContentColor,
            modifier = modifier,
        )

        MetricArt.HEART -> Box(modifier) {
            StateLine(R.string.card_state_heart_zone, config.dangerColor)
            PulseWave(
                color = config.dangerColor,
                gridColor = config.lineColor,
                modifier = Modifier.fillMaxWidth().height(ART_HEIGHT),
            )
        }

        MetricArt.POWER -> Box(modifier) {
            PowerBars(config.quietAccentColor)
        }

        MetricArt.PULL -> Box(modifier) {
            PullCurve(
                color = config.accentToneColor,
                gridColor = config.lineColor,
                axisColor = config.mutedContentColor,
                modifier = Modifier.fillMaxWidth().height(PULL_HEIGHT),
            )
        }

        MetricArt.SLOPE -> Box(modifier) {
            StateLine(R.string.card_state_slope_up, config.quietAccentColor)
            SlopeGraphic(
                color = config.quietAccentColor,
                gridColor = config.lineColor,
                modifier = Modifier.fillMaxWidth().height(SLOPE_HEIGHT),
            )
        }
    }
}

/** 卡片上的一行狀態，前面一顆發光的小點。 */
@Composable
private fun StateLine(textRes: Int, tint: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(STATE_DOT).clip(CircleShape).background(tint))
        Text(text = stringResource(textRes), color = tint, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * 拉力卡的拉力曲線。
 *
 * 畫的是**連續幾槳**：每一槳一個峰，入水陡升、出水回落到底。單槳一個峰讀不出
 * 節奏，連著幾槳才看得出「這幾槳一致不一致」——峰高差一截就是有一槳偷懶了。
 *
 * 三個元素各有分工：
 * - 等距的豎向網格是**時間軸**。沒有它，這條線只是個裝飾波紋
 * - 底下那條橫軸是**零位**。峰谷落不落到零，決定了它讀起來是幾槳還是一團起伏
 * - 線下的填充給曲線一個「量」的體感；只有一根線的話，峰高要靠眼睛量
 *
 * 曲線走平滑而不是折線：拉力是連續量，折角會讀成採樣點。用 Catmull-Rom
 * 轉三次貝茲，控制點由相鄰兩點的斜率算，不必手寫。
 *
 * @param color 曲線顏色
 * @param gridColor 網格線顏色
 * @param axisColor 底部零位線的顏色
 * @param modifier 外部修飾符
 */
@Composable
private fun PullCurve(
    color: Color,
    gridColor: Color,
    axisColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        // 豎向時間軸
        val step = size.width / PULL_GRID_LINES
        for (index in 1 until PULL_GRID_LINES) {
            val x = step * index
            drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
        }

        // 曲線佔的高度留出頂部一點餘量，峰頂才不會貼著卡片的上緣
        val top = size.height * PULL_TOP_INSET
        val baseline = size.height - PULL_AXIS_WIDTH.toPx()
        val points = PULL_POINTS.map { (x, y) ->
            // y 是 0..1 的拉力比例，畫布往下為正，所以要翻過來
            Offset(x * size.width, baseline - (baseline - top) * y)
        }

        val line = smoothPath(points)
        // 線下填充：把同一條路徑封到零位線上圍成一塊
        val area = Path().apply {
            addPath(line)
            lineTo(points.last().x, baseline)
            lineTo(points.first().x, baseline)
            close()
        }
        drawPath(path = area, color = color.copy(alpha = PULL_FILL_ALPHA))
        drawPath(
            path = line,
            color = color,
            style = Stroke(width = PULL_STROKE.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawLine(
            color = axisColor.copy(alpha = PULL_AXIS_ALPHA),
            start = Offset(0f, baseline),
            end = Offset(size.width, baseline),
            strokeWidth = PULL_AXIS_WIDTH.toPx(),
        )
    }
}

/**
 * 把一串點連成平滑曲線。
 *
 * Catmull-Rom 轉三次貝茲：每一段的兩個控制點由**相鄰**兩點的連線斜率給出，
 * 所以曲線一定穿過每一個原始點（這正是它比手寫貝茲省事的地方——調點就行，
 * 不用同時調六個控制點）。端點各自複製一份當虛擬鄰居。
 *
 * @param points 至少兩個點
 */
internal fun smoothPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points.first().x, points.first().y)
    if (points.size == 1) return path

    for (index in 0 until points.size - 1) {
        val p0 = points[(index - 1).coerceAtLeast(0)]
        val p1 = points[index]
        val p2 = points[index + 1]
        val p3 = points[(index + 2).coerceAtMost(points.size - 1)]
        path.cubicTo(
            p1.x + (p2.x - p0.x) / SMOOTH_TENSION,
            p1.y + (p2.y - p0.y) / SMOOTH_TENSION,
            p2.x - (p3.x - p1.x) / SMOOTH_TENSION,
            p2.y - (p3.y - p1.y) / SMOOTH_TENSION,
            p2.x,
            p2.y,
        )
    }
    return path
}

/** Catmull-Rom 的張力：6 是標準值，越大越貼近折線。 */
private const val SMOOTH_TENSION = 6f

/**
 * 脈搏波。
 *
 * 座標照抄參考稿 `.pulse-path` 的 viewBox 0 0 140 36，按實際寬高等比縮放。
 */
@Composable
private fun PulseWave(color: Color, gridColor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val sx = size.width / PULSE_VIEW_W
        val sy = size.height / PULSE_VIEW_H

        // 兩條底格線
        listOf(11f, 24f).forEach { y ->
            drawLine(
                color = gridColor,
                start = Offset(0f, y * sy),
                end = Offset(size.width, y * sy),
                strokeWidth = 1f,
            )
        }

        val path = Path()
        PULSE_POINTS.forEachIndexed { index, (x, y) ->
            if (index == 0) path.moveTo(x * sx, y * sy) else path.lineTo(x * sx, y * sy)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = 2.2f * sy,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

/** 功率四段條，最後一段淡一級表示還沒到。 */
@Composable
private fun PowerBars(color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.height(POWER_BARS_HEIGHT),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        POWER_BAR_HEIGHTS.forEachIndexed { index, height ->
            Box(
                Modifier
                    .width(POWER_BAR_WIDTH)
                    .height(height)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        color.copy(
                            alpha = if (index == POWER_BAR_HEIGHTS.lastIndex) POWER_BAR_DIM else 1f
                        )
                    )
            )
        }
    }
}

/** 坡形：底紋 + 一塊斜切的填充 + 一條坡線。 */
@Composable
private fun SlopeGraphic(color: Color, gridColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.verticalGradient(
                    listOf(color.copy(alpha = 0.02f), color.copy(alpha = 0.08f))
                )
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawSlopeRidges(gridColor)
            drawSlopeFill(color)
        }
    }
}

/** 底紋：每 14dp 一條橫線，從底部往上排。 */
private fun DrawScope.drawSlopeRidges(gridColor: Color) {
    val gap = 14.dp.toPx()
    var y = size.height
    while (y > 0f) {
        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        y -= gap
    }
}

/**
 * 斜切的填充加坡線。
 *
 * 參考稿用 `clip-path: polygon(0 100%, 100% 18%, 100% 100%)` 加一條 `rotate(-18deg)`
 * 的線。這裡直接畫三角形，再沿著它的斜邊描線——旋轉一條直線在 Compose 裡還要自己
 * 算兩端出界的位置，不如直接用斜邊的座標。
 */
private fun DrawScope.drawSlopeFill(color: Color) {
    val top = size.height * SLOPE_TOP_FRACTION
    val triangle = Path().apply {
        moveTo(0f, size.height)
        lineTo(size.width, top)
        lineTo(size.width, size.height)
        close()
    }
    drawPath(
        path = triangle,
        brush = Brush.linearGradient(
            listOf(color.copy(alpha = 0.02f), color.copy(alpha = 0.22f))
        ),
    )
    drawLine(
        color = color,
        start = Offset(0f, size.height),
        end = Offset(size.width, top),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round,
    )
}

private val ART_HEIGHT = 30.dp
private val SLOPE_HEIGHT = 34.dp
private val STATE_DOT = 5.dp

private const val PULSE_VIEW_W = 140f
private const val PULSE_VIEW_H = 36f

/** 參考稿 `.pulse-path` 的折點。 */
private val PULSE_POINTS = listOf(
    0f to 23f, 24f to 23f, 32f to 20f, 40f to 25f, 49f to 8f, 58f to 29f, 67f to 19f,
    75f to 23f, 96f to 23f, 104f to 20f, 112f to 25f, 121f to 11f, 130f to 27f, 140f to 22f,
)

private val POWER_BARS_HEIGHT = 23.dp
private val POWER_BAR_WIDTH = 13.dp
private val POWER_BAR_HEIGHTS = listOf(10.dp, 16.dp, 22.dp, 14.dp)
private const val POWER_BAR_DIM = 0.58f

/** 坡頂落在高度的 18%，與參考稿的 clip-path 一致。 */
private const val SLOPE_TOP_FRACTION = 0.18f


/**
 * 連續幾槳的拉力曲線，(時間 0..1, 拉力 0..1)。
 *
 * 三槳：一記大的、一記很輕的、一記中等的，每槳都回落到零位。峰高不齊是刻意的——
 * 這條線的用處正是把「這幾槳不一致」畫出來。
 *
 * 接真機時換成拉力特徵的滑動窗口，點的間距即採樣間隔。
 */
private val PULL_POINTS = listOf(
    0f to 0f,
    0.08f to 0.10f,
    0.15f to 0.78f,
    0.21f to 0.42f,
    0.26f to 0.06f,
    0.32f to 0.26f,
    0.40f to 0.08f,
    0.50f to 0.02f,
    0.60f to 0.03f,
    0.68f to 0.30f,
    0.73f to 0.48f,
    0.80f to 0.20f,
    0.88f to 0.03f,
    1f to 0.02f,
)

/** 時間軸的網格線數與曲線粗細。 */
private const val PULL_GRID_LINES = 8
private val PULL_STROKE = 1.5.dp

/** 拉力卡那塊圖比別的高一點：三個峰要分得開。 */
private val PULL_HEIGHT = 38.dp

/** 峰頂與卡片上緣之間留的餘量，佔整塊高度的比例。 */
private const val PULL_TOP_INSET = 0.08f

/** 線下填充的濃度，與零位線的粗細、濃度。 */
private const val PULL_FILL_ALPHA = 0.18f
private val PULL_AXIS_WIDTH = 1.dp
private const val PULL_AXIS_ALPHA = 0.45f

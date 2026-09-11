package top.hasiyliquidglassdemo.ui.workout

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import top.hasiy.designsystem.GlassConfig
import top.hasiy.designsystem.LocalGlassConfig
import top.hasiy.designsystem.accentToneColor
import top.hasiy.designsystem.lineColor
import top.hasiy.designsystem.mutedContentColor
import top.hasiy.designsystem.screenColor
import top.hasiy.designsystem.sunkColor
import top.hasiy.designsystem.trackColor
import top.hasiyliquidglassdemo.R

/**
 * F1 賽道地圖。
 *
 * 對應參考稿 09 的賽道模式。座標照抄那份 `viewBox="0 0 260 150"` 的 `d`。
 *
 * 跟虛擬路線（[RouteMap]）是兩件事，所以分開寫：
 * - 路線是**開放折線**，畫起點、終點、剩餘距離；賽道是**閉環**，畫起跑線、彎角、圈數
 * - 賽道不鋪地形：F1 官方賽道圖就是純底加賽道線，閉環幾乎占滿畫布，也沒有安全區可放
 * - [progress] 的含義也不同：路線是全程走了多少，賽道是**本圈**走了多少，每圈歸零
 * - 縮放方式也不同：路線可以跟著畫布拉伸（它只是條示意的折線），賽道必須**等比**——
 *   賽道的形狀本身就是它的身分，橫向拉開之後就不是那條賽道了
 *
 * 賽道本身用「外沿 + 路面」兩層描邊，這是賽道圖的慣用畫法：粗的一層當路肩、
 * 細的一層當路面，比單線更像一條真的賽道。
 *
 * @param progress 本圈已走的比例（0..1）
 * @param config 玻璃主題參數
 * @param modifier 外部修飾符
 */
@Composable
fun CircuitMap(
    progress: Float,
    config: GlassConfig = LocalGlassConfig.current,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    // 這張圖傳達的是「賽道走向 + 現在在哪 + 本圈走了多少」。前兩樣沒法用一句話講清，
    // 進度是這裡面唯一對讀屏使用者有用又講得出來的，所以描述只報它。
    val description = stringResource(
        R.string.circuit_map_desc,
        (progress.coerceIn(0f, 1f) * 100).toInt(),
    )

    // 賽道的幾何與那些固定文字都放進 drawWithCache，只有跟進度有關的部分留在每帧。
    //
    // 這一整塊裡真正隨時間變的只有 progress，而它只決定「已走的那段取多長」與
    // 「當前位置的點畫在哪」。賽道本體是十二段貝茲加兩段直線，PathMeasure.setPath
    // 還得走完整條算長度；S1/S2/S3 與七個彎角編號則是固定文字，每次 measure 都是
    // 重新排版一次。這些全都只依賴畫布尺寸（透過 scale/dx/dy）與主題色，
    // 留在 draw lambda 裡就是每帧重算一次本來可以留著的東西。
    Spacer(
        modifier
            .fillMaxSize()
            .semantics { contentDescription = description }
            .drawWithCache {
            // 底圖路網鋪滿整塊畫布（參考稿的格線就是鋪滿的），所以它用畫布自己的比例；
            // 賽道本體則等比縮放後居中。
            val gridSx = size.width / CIRCUIT_VIEW_W
            val gridSy = size.height / CIRCUIT_VIEW_H

            // 等比縮放（SVG 的 `preserveAspectRatio="xMidYMid meet"`）：取兩軸的較小比例，
            // 剩下的那一軸居中留白。
            val scale = minOf(gridSx, gridSy)
            val dx = (size.width - CIRCUIT_VIEW_W * scale) / 2f
            val dy = (size.height - CIRCUIT_VIEW_H * scale) / 2f

            val track = buildCircuitPath(scale, dx, dy)
            val measure = PathMeasure().apply { setPath(track, false) }
            val total = measure.length
            // 已走那一段的 Path 複用：每帧 reset 比每帧 new 便宜，getSegment 自己會 moveTo
            val lapPath = Path()

            // 線寬按比例算，不用固定 dp：參考稿的 9 與 5 是 viewBox 單位（佔 260 寬的
            // 3.5% 與 1.9%），固定 dp 會讓小畫布上粗成一團、大畫布上細成一根頭髮。
            val edgeStroke = Stroke(
                width = TRACK_EDGE_UNITS * scale,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            )
            val coreStroke = Stroke(
                width = TRACK_CORE_UNITS * scale,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            )

            val sectorLabels = SECTOR_TAGS.map { (label, _) ->
                measurer.measure(
                    label,
                    TextStyle(
                        color = config.mutedContentColor,
                        fontSize = (SECTOR_TAG_SIZE * scale).toSp(),
                        fontWeight = FontWeight.ExtraBold,
                    ),
                )
            }
            val cornerNumbers = List(CORNERS.size) { index ->
                measurer.measure(
                    (index + 1).toString(),
                    TextStyle(
                        color = config.mutedContentColor,
                        fontSize = (CORNER_NUM_SIZE * scale).toSp(),
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }

            onDrawBehind {
                val done = progress.coerceIn(0f, 1f)

                drawCircuitGrid(gridSx, gridSy, config.lineColor)

                // 路肩、路面、本圈已走，三層疊上去。順序不能換：路面要蓋在路肩中間，
                // 已走的那條再蓋在路面上。
                drawPath(path = track, color = config.trackColor, style = edgeStroke)
                drawPath(path = track, color = config.sunkColor, style = coreStroke)

                lapPath.reset()
                measure.getSegment(0f, total * done, lapPath, true)
                drawPath(path = lapPath, color = config.accentToneColor, style = coreStroke)

                drawStartLine(scale, dx, dy, config, lapDone = done >= LAP_DONE_THRESHOLD)
                drawSectorTags(scale, dx, dy, sectorLabels)
                drawCorners(scale, dx, dy, config, cornerNumbers)

                // 當前位置：光暈加實心點，長在已走那一段的末端。跟描線走同一條路徑，
                // 兩者天然對齊——各算一次的話點會浮在線外面。
                if (total > 0f) {
                    val now = measure.getPosition(total * done)
                    drawCircle(config.accentToneColor.copy(alpha = PIN_HALO_ALPHA), PIN_HALO.toPx(), now)
                    drawCircle(config.accentToneColor, PIN_DOT.toPx(), now)
                    drawCircle(
                        color = config.screenColor,
                        radius = PIN_DOT.toPx(),
                        center = now,
                        style = Stroke(width = PIN_RING_WIDTH.toPx()),
                    )
                }
            }
        }
    )
}

/** 底圖路網：只做暗示，不能搶賽道本身。 */
private fun DrawScope.drawCircuitGrid(sx: Float, sy: Float, color: Color) {
    val width = CIRCUIT_GRID_WIDTH.toPx()
    CIRCUIT_GRID_ROWS.forEach { y ->
        drawLine(color, Offset(0f, y * sy), Offset(size.width, y * sy), width)
    }
    CIRCUIT_GRID_COLUMNS.forEach { x ->
        drawLine(color, Offset(x * sx, 0f), Offset(x * sx, size.height), width)
    }
}

/**
 * 起跑線。
 *
 * 參考稿用 SVG `pattern` 鋪 3.2 單位的棋盤格，這裡直接按格子畫——
 * 一條 3.4×14 的細長條上只放得下 2 欄，為此建一個 pattern 不划算。
 *
 * 本圈跑滿時線後面亮一下（`.is-done .start-line-glow`）：閉環上跑滿一圈沒有別的
 * 視覺變化，位置點又回到原地，不亮一下使用者不知道自己完成了一圈。
 */
private fun DrawScope.drawStartLine(
    scale: Float,
    dx: Float,
    dy: Float,
    config: GlassConfig,
    lapDone: Boolean,
) {
    if (lapDone) {
        drawRect(
            color = config.accentToneColor,
            topLeft = Offset(START_GLOW_X * scale + dx, START_GLOW_Y * scale + dy),
            size = Size(START_GLOW_W * scale, START_GLOW_H * scale),
        )
    }

    val left = START_LINE_X * scale + dx
    val top = START_LINE_Y * scale + dy
    val cellW = START_LINE_W * scale / START_LINE_COLUMNS
    val cellH = START_LINE_H * scale / START_LINE_ROWS
    clipRect(left, top, left + START_LINE_W * scale, top + START_LINE_H * scale) {
        repeat(START_LINE_ROWS) { row ->
            repeat(START_LINE_COLUMNS) { column ->
                // 棋盤：行列同奇偶的格子塗深色，其餘留亮色
                val dark = (row + column) % 2 == 0
                drawRect(
                    color = if (dark) START_LINE_DARK else START_LINE_LIGHT,
                    topLeft = Offset(left + column * cellW, top + row * cellH),
                    size = Size(cellW, cellH),
                )
            }
        }
    }
}

/** 分段標籤 S1 / S2 / S3，擺在各段的外側。 */
private fun DrawScope.drawSectorTags(
    scale: Float,
    dx: Float,
    dy: Float,
    labels: List<TextLayoutResult>,
) {
    SECTOR_TAGS.forEachIndexed { index, (_, position) ->
        val (x, y) = position
        val layout = labels[index]
        // SVG 的 y 是基線，Compose 的 drawText 從頂端量起
        drawText(
            textLayoutResult = layout,
            topLeft = Offset(x * scale + dx, y * scale + dy - layout.size.height),
        )
    }
}

/** 彎角編號：一圈中性色小圓加號碼。 */
private fun DrawScope.drawCorners(
    scale: Float,
    dx: Float,
    dy: Float,
    config: GlassConfig,
    numbers: List<TextLayoutResult>,
) {
    CORNERS.forEachIndexed { index, (x, y) ->
        val center = Offset(x * scale + dx, y * scale + dy)
        drawCircle(config.sunkColor, CORNER_RADIUS * scale, center)
        drawCircle(
            color = config.mutedContentColor,
            radius = CORNER_RADIUS * scale,
            center = center,
            style = Stroke(width = CORNER_STROKE * scale),
        )
        val layout = numbers[index]
        drawText(
            textLayoutResult = layout,
            topLeft = Offset(
                center.x - layout.size.width / 2f,
                center.y - layout.size.height / 2f,
            ),
        )
    }
}

/**
 * 賽道本體。
 *
 * 照抄參考稿的 `d`：兩段直線加十二段三次貝茲，閉合。
 * 起點在 (44,122)——起跑線那一段直道上，所以本圈進度從那裡開始長。
 */
private fun buildCircuitPath(scale: Float, dx: Float, dy: Float): Path = Path().apply {
    fun x(value: Float) = value * scale + dx
    fun y(value: Float) = value * scale + dy

    moveTo(x(44f), y(122f))
    lineTo(x(176f), y(122f))
    CIRCUIT_CURVE_A.forEach { segment ->
        val (c1, c2, end) = segment
        cubicTo(x(c1.first), y(c1.second), x(c2.first), y(c2.second), x(end.first), y(end.second))
    }
    lineTo(x(120f), y(30f))
    CIRCUIT_CURVE_B.forEach { segment ->
        val (c1, c2, end) = segment
        cubicTo(x(c1.first), y(c1.second), x(c2.first), y(c2.second), x(end.first), y(end.second))
    }
    close()
}

/** 參考稿的 viewBox。 */
private const val CIRCUIT_VIEW_W = 260f
private const val CIRCUIT_VIEW_H = 150f

private val CIRCUIT_GRID_ROWS = listOf(34f, 78f, 122f)
private val CIRCUIT_GRID_COLUMNS = listOf(52f, 116f, 178f, 228f)

/** 起跑線直道之後那一串彎，到上方直線的起點 (198,18) 為止。 */
private val CIRCUIT_CURVE_A = listOf(
    listOf(200f to 122f, 214f to 118f, 224f to 108f),
    listOf(236f to 96f, 238f to 80f, 226f to 72f),
    listOf(214f to 64f, 196f to 68f, 188f to 60f),
    listOf(180f to 52f, 190f to 40f, 204f to 38f),
    listOf(218f to 36f, 226f to 44f, 228f to 32f),
    listOf(230f to 20f, 214f to 14f, 198f to 18f),
)

/** 上方直線之後那一串彎，繞回起點 (44,122)。 */
private val CIRCUIT_CURVE_B = listOf(
    listOf(104f to 33f, 96f to 26f, 84f to 28f),
    listOf(68f to 30f, 62f to 44f, 72f to 52f),
    listOf(82f to 60f, 96f to 56f, 100f to 66f),
    listOf(104f to 77f, 86f to 82f, 74f to 78f),
    listOf(58f to 73f, 42f to 82f, 38f to 96f),
    listOf(35f to 108f, 36f to 118f, 44f to 122f),
)

/** 分段標籤與它們的基線位置。 */
private val SECTOR_TAGS = listOf(
    "S1" to (58f to 136f),
    "S2" to (212f to 26f),
    "S3" to (24f to 88f),
)

/** 七個彎角的圓心，順序即編號。 */
private val CORNERS = listOf(
    192f to 132f,
    246f to 88f,
    178f to 50f,
    238f to 22f,
    80f to 18f,
    110f to 70f,
    62f to 90f,
)

private const val CORNER_RADIUS = 4.6f
private const val CORNER_STROKE = 0.9f
private const val CORNER_NUM_SIZE = 5.4f
private const val SECTOR_TAG_SIZE = 6.4f

/** 路肩與路面的線寬，viewBox 單位。 */
private const val TRACK_EDGE_UNITS = 9f
private const val TRACK_CORE_UNITS = 5f

/** 起跑線與它跑滿一圈時的光暈。 */
private const val START_LINE_X = 106.8f
private const val START_LINE_Y = 115f
private const val START_LINE_W = 3.4f
private const val START_LINE_H = 14f
private const val START_LINE_COLUMNS = 2
private const val START_LINE_ROWS = 9
private const val START_GLOW_X = 106f
private const val START_GLOW_Y = 114f
private const val START_GLOW_W = 5f
private const val START_GLOW_H = 16f

/**
 * 起跑線的棋盤格用**固定的黑白**，不走主題色。
 *
 * 它不是介面元素，是賽道上畫著的那塊格紋——真實賽道上就是黑白的，
 * 跟著主題換色反而讓人認不出那是起跑線。
 */
private val START_LINE_DARK = Color(0xFF15171A)
private val START_LINE_LIGHT = Color(0xFFF2F2EE)

/** 本圈跑到這個比例就算跑滿，起跑線亮一下。 */
private const val LAP_DONE_THRESHOLD = 0.995f

private val CIRCUIT_GRID_WIDTH = 1.dp
private val PIN_DOT = 5.dp
private val PIN_HALO = 10.dp
private val PIN_RING_WIDTH = 2.dp
private const val PIN_HALO_ALPHA = 0.28f

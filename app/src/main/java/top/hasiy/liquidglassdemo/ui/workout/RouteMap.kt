package top.hasiyliquidglassdemo.ui.workout

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import top.hasiy.designsystem.GlassConfig
import top.hasiy.designsystem.LocalGlassConfig
import top.hasiy.designsystem.accentToneColor
import top.hasiy.designsystem.isLightSurface
import top.hasiy.designsystem.lineColor
import top.hasiy.designsystem.quietAccentColor
import top.hasiy.designsystem.screenContentColor
import top.hasiy.designsystem.sunkColor
import top.hasiy.designsystem.trackColor

/**
 * 虛擬路線地圖。
 *
 * 對應參考稿 09 的 `.map-svg`。座標照抄那份 `viewBox="0 0 260 150"`，按實際畫布等比縮放。
 *
 * 地形（山脊、湖、林地）的擺位是**寫死的**，只為做稿；接真實路線時要按路線種子生成，
 * 而且生成時必須跟路線做碰撞檢測——地形壓在路線上會把進度線讀糊。參考稿留了兩塊安全區：
 * ```
 * 左上  路線在 x<140 段的 y 始終 ≥ 68，所以 y<55 可用
 * 右下  路線在 x>120 段的 y 始終 ≤ 68，所以 y>90 可用
 * ```
 *
 * 進度用 [PathMeasure] 截取路徑的前 [progress] 段，而不是換一條短路徑——
 * 兩條路徑的貝茲控制點必須完全一致，否則已走與未走那兩段會在接縫處錯開。
 *
 * @param progress 已完成的比例（0..1）
 * @param config 玻璃主題參數
 * @param modifier 外部修飾符
 */
@Composable
fun RouteMap(
    progress: Float,
    config: GlassConfig = LocalGlassConfig.current,
    modifier: Modifier = Modifier,
) {
    // 地形與路線的幾何都放進 drawWithCache，只有跟進度有關的部分留在每帧。
    //
    // 這張圖裡隨時間變的只有 progress，它只決定已走那段取多長、當前位置的點畫在哪。
    // 而地形是四組多邊形與水域林地的 blob、路線是一條多段貝茲，PathMeasure.setPath
    // 還得走完整條算長度——這些只依賴畫布尺寸。KDoc 也提到接真實路線之後 progress
    // 會是持續更新的即時值，屆時重繪頻率會上去，這些重建的代價會被放大。
    Spacer(
        modifier.fillMaxSize().drawWithCache {
            val sx = size.width / VIEW_W
            val sy = size.height / VIEW_H

            val hills = HILLS.map { polygonOf(it, sx, sy) }
            val hillTops = HILL_TOPS.map { polygonOf(it, sx, sy) }
            val lake = blobOf(LAKE, sx, sy)
            val woods = WOODS.map { blobOf(it, sx, sy) }

            val route = buildRoutePath(ROUTE_CURVE, sx, sy)
            val measure = PathMeasure().apply { setPath(route, false) }
            val total = measure.length
            // 已走那一段的 Path 複用：每帧 reset 比每帧 new 便宜
            val done = Path()

            val routeStroke = Stroke(width = ROUTE_WIDTH.toPx(), cap = StrokeCap.Round)
            val start = Offset(ROUTE_START.first * sx, ROUTE_START.second * sy)
            val finish = Offset(ROUTE_FINISH.first * sx, ROUTE_FINISH.second * sy)

            onDrawBehind {
                val fraction = progress.coerceIn(0f, 1f)

                drawMapGrid(sx, sy, config.lineColor)
                drawTerrain(hills, hillTops, lake, woods, config)

                // 未走的整條先鋪底，已走的再蓋上去
                drawPath(path = route, color = config.trackColor, style = routeStroke)
                done.reset()
                measure.getSegment(0f, total * fraction, done, true)
                drawPath(path = done, color = config.accentToneColor, style = routeStroke)

                // 起點與終點
                drawCircle(config.quietAccentColor, PIN_RADIUS.toPx(), start)
                drawCircle(
                    color = config.screenContentColor,
                    radius = FINISH_RING.toPx(),
                    center = finish,
                    style = Stroke(width = FINISH_RING_WIDTH.toPx()),
                )
                drawCircle(config.screenContentColor, PIN_RADIUS.toPx(), finish)

                // 當前位置：光暈加實心點，長在已走那一段的末端。
                //
                // 整段包在 total > 0 裡面：量不到長度時原本會落到 Offset(0, 0)，
                // 在左上角畫出一個跟路線無關的光點。畫不出來比畫錯位置好。
                if (total > 0f) {
                    val now = measure.getPosition(total * fraction)
                    drawCircle(config.accentToneColor.copy(alpha = HALO_ALPHA), HALO_RADIUS.toPx(), now)
                    drawCircle(config.accentToneColor, NOW_RADIUS.toPx(), now)
                }
            }
        }
    )
}

/** 底圖路網：只做暗示，不能抢路線本身。 */
private fun DrawScope.drawMapGrid(sx: Float, sy: Float, color: Color) {
    val width = GRID_WIDTH.toPx()
    GRID_ROWS.forEach { y ->
        drawLine(color, Offset(0f, y * sy), Offset(size.width, y * sy), width)
    }
    GRID_COLUMNS.forEach { x ->
        drawLine(color, Offset(x * sx, 0f), Offset(x * sx, size.height), width)
    }
}

/**
 * 地形：兩座山脊在左上，湖與林地在右下與空檔處。
 *
 * 山脊是**屏內內容色的極低透明度**（參考稿 `--map-hill: rgba(20,20,20,.07)`）——
 * 它只是地勢的暗示。不能用 sunk 那個表面色：畫布底本身就是 sunk，同色疊上去
 * 等於什麼都沒畫。
 *
 * 湖與林地用固定的藍與綠，跟主題色無關：水是藍的、林地是綠的，這是地圖的約定，
 * 跟著強調色變就不像地圖了。明暗各一組——深色屏上要提亮，否則在 #0D1011 的畫布上
 * 兩塊都看不出來。
 */
private fun DrawScope.drawTerrain(
    hills: List<Path>,
    hillTops: List<Path>,
    lake: Path,
    woods: List<Path>,
    config: GlassConfig,
) {
    val light = config.isLightSurface
    val rock = config.screenContentColor

    hills.forEach { hill -> drawPath(hill, rock.copy(alpha = TERRAIN_ALPHA)) }
    hillTops.forEach { top -> drawPath(top, rock.copy(alpha = TERRAIN_TOP_ALPHA)) }
    drawPath(lake, if (light) WATER_LIGHT else WATER_DARK)
    val green = if (light) GREEN_LIGHT else GREEN_DARK
    woods.forEach { wood -> drawPath(wood, green) }
}

/** 把 (x, y) 序列連成一個閉合多邊形。 */
private fun polygonOf(points: List<Pair<Float, Float>>, sx: Float, sy: Float): Path = Path().apply {
    points.forEachIndexed { index, (x, y) ->
        if (index == 0) moveTo(x * sx, y * sy) else lineTo(x * sx, y * sy)
    }
    close()
}

/**
 * 把三次貝茲的控制點序列連成一個閉合塊（湖、林地）。
 *
 * 每四個點一組：起點在上一段的終點，後三個是兩個控制點加終點。
 */
private fun blobOf(segments: List<List<Pair<Float, Float>>>, sx: Float, sy: Float): Path = Path().apply {
    val first = segments.first().first()
    moveTo(first.first * sx, first.second * sy)
    segments.forEach { segment ->
        val (c1, c2, end) = segment.drop(1)
        cubicTo(c1.first * sx, c1.second * sy, c2.first * sx, c2.second * sy, end.first * sx, end.second * sy)
    }
    close()
}

/** 路線：一串三次貝茲段。 */
private fun buildRoutePath(segments: List<List<Pair<Float, Float>>>, sx: Float, sy: Float): Path =
    Path().apply {
        moveTo(ROUTE_START.first * sx, ROUTE_START.second * sy)
        segments.forEach { segment ->
            val (c1, c2, end) = segment
            cubicTo(c1.first * sx, c1.second * sy, c2.first * sx, c2.second * sy, end.first * sx, end.second * sy)
        }
    }

/** 參考稿的 viewBox。 */
private const val VIEW_W = 260f
private const val VIEW_H = 150f

private val GRID_ROWS = listOf(34f, 78f, 122f)
private val GRID_COLUMNS = listOf(52f, 116f, 178f, 228f)

/** 路線起點與終點。 */
private val ROUTE_START = 20f to 118f
private val ROUTE_FINISH = 240f to 26f

/**
 * 路線本體：四段三次貝茲，每段是（控制點 1、控制點 2、終點）。
 *
 * 座標照抄參考稿的 `d="M20 118 C 46 122, 50 84, 76 80 …"`。
 */
private val ROUTE_CURVE = listOf(
    listOf(46f to 122f, 50f to 84f, 76f to 80f),
    listOf(96f to 77f, 104f to 92f, 120f to 68f),
    listOf(134f to 47f, 152f to 24f, 184f to 32f),
    listOf(214f to 39f, 226f to 50f, 240f to 26f),
)

/** 兩座山脊，底邊壓在 y=52，離路線最近處還有 16px。 */
private val HILLS = listOf(
    listOf(26f to 53f, 50f to 16f, 74f to 53f),
    listOf(70f to 53f, 92f to 23f, 114f to 53f),
)
private val HILL_TOPS = listOf(
    listOf(50f to 16f, 59f to 30f, 41f to 30f),
    listOf(92f to 23f, 99f to 34f, 85f to 34f),
)

/** 湖：右下，頂邊 y=94。 */
private val LAKE = listOf(
    listOf(152f to 104f, 166f to 94f, 196f to 94f, 210f to 106f),
    listOf(210f to 106f, 222f to 116f, 218f to 136f, 200f to 142f),
    listOf(200f to 142f, 180f to 148f, 156f to 142f, 150f to 126f),
    listOf(150f to 126f, 146f to 115f, 147f to 108f, 152f to 104f),
)

/** 林地：一塊貼湖、一塊塞右上角的空檔。 */
private val WOODS = listOf(
    listOf(
        listOf(94f to 118f, 110f to 110f, 132f to 114f, 140f to 128f),
        listOf(140f to 128f, 146f to 140f, 132f to 149f, 114f to 147f),
        listOf(114f to 147f, 98f to 145f, 86f to 132f, 94f to 118f),
    ),
    listOf(
        listOf(234f to 64f, 246f to 60f, 257f to 68f, 257f to 80f),
        listOf(257f to 80f, 257f to 92f, 244f to 95f, 237f to 89f),
        listOf(237f to 89f, 229f to 81f, 226f to 68f, 234f to 64f),
    ),
)

private val ROUTE_WIDTH = 4.dp
private val GRID_WIDTH = 1.dp
private val PIN_RADIUS = 4.dp
private val FINISH_RING = 8.dp
private val FINISH_RING_WIDTH = 1.5.dp
private val NOW_RADIUS = 5.dp
private val HALO_RADIUS = 11.dp
private const val HALO_ALPHA = 0.28f

/** 地形只做暗示，濃度壓到很低——它們不是讀數。照抄參考稿的 7% 與 12%。 */
private const val TERRAIN_ALPHA = 0.07f
private const val TERRAIN_TOP_ALPHA = 0.12f

/** 湖與林地，照抄參考稿的 `--map-water` / `--map-green` 兩套。 */
private val WATER_LIGHT = Color(0x33467AA5)
private val WATER_DARK = Color(0x2B6094BE)
private val GREEN_LIGHT = Color(0x26508246)
private val GREEN_DARK = Color(0x1C92C47E)

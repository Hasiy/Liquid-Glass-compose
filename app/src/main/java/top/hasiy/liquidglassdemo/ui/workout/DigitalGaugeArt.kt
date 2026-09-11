package top.hasiyliquidglassdemo.ui.workout

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Digital 風格的儀表繪製。
 *
 * 這套風格不畫弧和刻度——參考稿把 `.gauge-scale` 與 `.phase-mini` 整個藏掉，
 * 換成一圈點陣進度加一組能量刻度，像儀器面板而不是玻璃錶盤。所以它不是
 * 「同一個環換個顏色」，得單獨一條繪製路徑。
 *
 * 座標全部照參考稿的 `viewBox 0 0 240 240` 等比縮放。
 *
 * @param fraction 進度 0..1
 */
internal fun DrawScope.drawDigitalGauge(fraction: Float) {
    val unit = size.minDimension / VIEW_BOX
    val center = Offset(size.width / 2f, size.height / 2f)

    drawDotRing(center, unit, fraction)
    drawEnergyBars(center, unit)
}

/**
 * 點環：52 顆點沿環排一圈，已到達的用紅、當前那顆再大一檔。
 *
 * 參考稿把 `reachedIndex` 寫死成 `52 × 238 / 286`（也就是那張圖當下的讀數）。
 * 這裡改成跟著實際進度走，否則環永遠停在同一個位置。
 */
private fun DrawScope.drawDotRing(center: Offset, unit: Float, fraction: Float) {
    val reached = (TOTAL_DOTS * fraction.coerceIn(0f, 1f)).roundToInt()
    for (index in 0..TOTAL_DOTS) {
        val radians = Math.toRadians((RING_START_DEG + index * RING_STEP_DEG).toDouble())
        val position = Offset(
            x = center.x + (RING_RADIUS * unit) * cos(radians).toFloat(),
            y = center.y + (RING_RADIUS * unit) * sin(radians).toFloat(),
        )
        val (color, radius) = when {
            index == reached -> DOT_CURRENT to DOT_CURRENT_RADIUS
            index < reached -> DOT_REACHED to DOT_REACHED_RADIUS
            else -> DOT_IDLE to DOT_IDLE_RADIUS
        }
        drawCircle(color = color, radius = radius * unit, center = position)
    }
}

/**
 * 能量刻度：環的下緣一組 7 根短線，中間最長、兩側漸短，後 3 根逐級變淡。
 *
 * 這一組是固定的裝飾，參考稿沒有讓它跟著讀數變。
 */
private fun DrawScope.drawEnergyBars(center: Offset, unit: Float) {
    ENERGY_ANGLES.forEachIndexed { index, angle ->
        val radians = Math.toRadians(angle.toDouble())
        val inner = ENERGY_INNER_BASE + abs(ENERGY_PEAK - index) * ENERGY_INNER_STEP
        drawLine(
            color = ENERGY_COLORS[index],
            start = Offset(
                x = center.x + (inner * unit) * cos(radians).toFloat(),
                y = center.y + (inner * unit) * sin(radians).toFloat(),
            ),
            end = Offset(
                x = center.x + (RING_RADIUS * unit) * cos(radians).toFloat(),
                y = center.y + (RING_RADIUS * unit) * sin(radians).toFloat(),
            ),
            strokeWidth = (if (index < ENERGY_STRONG_COUNT) 4.5f else 4f) * unit,
            cap = StrokeCap.Round,
        )
    }
}

/** 參考稿的 SVG viewBox 邊長。 */
private const val VIEW_BOX = 240f

private const val TOTAL_DOTS = 52
private const val RING_START_DEG = 145f
private const val RING_STEP_DEG = 5f
private const val RING_RADIUS = 101f

private val DOT_IDLE = Color(0xFFD7DAD7)
private val DOT_REACHED = Color(0xFFFF3B3F)
private val DOT_CURRENT = Color(0xFFFF262B)
private const val DOT_IDLE_RADIUS = 2.15f
private const val DOT_REACHED_RADIUS = 2.45f
private const val DOT_CURRENT_RADIUS = 3f

private val ENERGY_ANGLES = listOf(68f, 75f, 82f, 89f, 96f, 103f, 110f)
private const val ENERGY_INNER_BASE = 83f
private const val ENERGY_INNER_STEP = 1.5f
private const val ENERGY_PEAK = 3
private const val ENERGY_STRONG_COUNT = 4
private val ENERGY_COLORS = listOf(
    Color(0xFF0ABF38),
    Color(0xFF0ABF38),
    Color(0xFF0ABF38),
    Color(0xFF0ABF38),
    Color(0xFF69C96D),
    Color(0xFFA8D9A9),
    Color(0xFFD2E3D0),
)

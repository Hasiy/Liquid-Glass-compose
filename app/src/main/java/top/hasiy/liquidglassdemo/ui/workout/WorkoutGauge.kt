package top.hasiyliquidglassdemo.ui.workout

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import top.hasiy.designsystem.GlassConfig
import top.hasiy.designsystem.LocalGlassConfig
import top.hasiy.designsystem.SevenSegmentText
import top.hasiy.designsystem.accentLightColor
import top.hasiy.designsystem.accentToneColor
import androidx.compose.ui.graphics.lerp
import top.hasiy.designsystem.liftColor
import top.hasiy.designsystem.sheenColor
import top.hasiy.designsystem.sunkColor
import top.hasiy.designsystem.trackColor
import top.hasiy.designsystem.isLightSurface
import top.hasiy.designsystem.lineColor
import top.hasiy.designsystem.mutedContentColor
import top.hasiy.designsystem.quietAccentColor
import top.hasiy.designsystem.panelColor
import top.hasiy.designsystem.screenContentColor
import top.hasiy.designsystem.tokens.GlassVisualStyle
import top.hasiyliquidglassdemo.R

/**
 * 中央儀表環。
 *
 * 對應參考稿的 `.p-gauge` / `.p-ticks` / `.gauge-scale` / `.gauge-copy`，
 * 橫屏的 `.l-hero` 是同一個環放大。角度與刻度取自 spec 3.4 的實測值。
 *
 * @param speed 當前速度（km/h），超出量程時夾到端點
 * @param config 玻璃主題參數
 * @param modifier 外部修飾符
 * @param live 是否還在跑；凍結時整環轉為中性色
 * @param target 目標速度（km/h）；null 表示不畫目標標記
 * @param delta 與 [deltaWindow] 之前相比的變化量（km/h）；null 表示不顯示那一行
 * @param deltaWindow 上面那個比較的時間窗文案，例如「1 分鐘」。窗口長度是**使用端**
 *   決定的（要比 1 分鐘還是 5 分鐘，儀表管不著），所以連同文案一起傳進來；
 *   不傳就不顯示對比行——[delta] 與它任一為 null 都不畫
 * @param phaseProgress 階段條上的位置（0..1）；null 表示不畫階段條
 */
@Composable
fun WorkoutGauge(
    speed: Float,
    config: GlassConfig = LocalGlassConfig.current,
    modifier: Modifier = Modifier,
    live: Boolean = true,
    target: Float? = null,
    delta: Float? = null,
    deltaWindow: String? = null,
    phaseProgress: Float? = null,
) {
    val measurer = rememberTextMeasurer()
    // 讀數跳變時讓弧走過去，而不是瞬移——實時速度本來就是連續量
    val animatedSpeed by animateFloatAsState(
        targetValue = speed.coerceIn(SPEED_MIN, SPEED_MAX),
        label = "gaugeSpeed",
    )
    val arcColor = if (live) config.accentToneColor else config.mutedContentColor
    // 軌道是**實色襯底**（參考稿 `--track`：深色主題 #303536、Nordic #B0B0B0），
    // 不是分隔線。之前拿 lineColor，Nordic 下那是 8% 的黑，壓在屏底上幾乎看不見，
    // 錶盤右半圈就成了一段沒畫完的環。
    val trackColor = config.trackColor
    val tickColor = config.mutedContentColor
    val targetColor = config.accentLightColor
    // 盤面分明暗兩種畫法，這是**表面性質**的分野，不是某一組配色的偏好：
    // 淺色屏的盤面和面板明度太近，不畫成一塊受光的凸圓就糊成一片；深色屏的盤面
    // 是純色，凸起感由外環給（參考稿也只為淺色配色覆寫了那道 radial-gradient）。
    //
    // 兩條路徑用的顏色全部來自表面 token，一個色都不寫死——同一個色系裡的六組
    // 主題走的是同一條路徑，差異只在色值。
    val litFace = config.liftColor
    val sheenFace = config.sheenColor
    val edgeFace = lerp(config.panelColor, config.sunkColor, FACE_EDGE_BLEND)

    BoxWithConstraints(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        // 中央讀數與階段條都要跟著環一起縮。刻度數字已經是按環的比例算的，
        // 中間那幾行如果固定 sp，環一小就會壓到刻度上（橫屏手機上「+1.6」正好蓋住「30」）。
        val ring = minOf(maxWidth, maxHeight)

        // Digital 不畫弧與刻度，改成點環加能量刻度——參考稿把 .gauge-scale 與
        // .phase-mini 整個藏掉，是另一條繪製路徑，不是換個顏色
        val digital = config.visualStyle == GlassVisualStyle.DIGITAL
        if (digital) {
            Canvas(Modifier.fillMaxSize()) {
                drawDialFace(
                    style = config.visualStyle,
                    fallbackFace = config.panelColor,
                    lit = config.isLightSurface,
                    litFace = litFace,
                    sheenFace = sheenFace,
                    edgeFace = edgeFace,
                )
                drawDigitalGauge(fractionOf(animatedSpeed))
            }
            GaugeReadout(
                speed = animatedSpeed,
                config = config,
                live = live,
                delta = delta,
                deltaWindow = deltaWindow,
                ring = ring,
            )
            return@BoxWithConstraints
        }

        Canvas(Modifier.fillMaxSize()) {
            val ringPx = size.minDimension
            // 盤面畫在最底層：刻度與讀數都壓在它上面。參考稿的 .p-gauge::after 是
            // 一塊 inset 18/238 的圓，Tactile 覆寫成帶內外光影的金屬面。
            drawDialFace(
                    style = config.visualStyle,
                    fallbackFace = config.panelColor,
                    lit = config.isLightSurface,
                    litFace = litFace,
                    sheenFace = sheenFace,
                    edgeFace = edgeFace,
                )
        }

        Canvas(Modifier.fillMaxSize()) {
            val ring = size.minDimension
            val stroke = ring * RING_STROKE_RATIO
            val inset = stroke / 2f
            val arcSize = Size(ring - stroke, ring - stroke)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = trackColor,
                startAngle = START_ANGLE_DEG,
                sweepAngle = SWEEP_DEG,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            drawArc(
                color = arcColor,
                startAngle = START_ANGLE_DEG,
                sweepAngle = SWEEP_DEG * fractionOf(animatedSpeed),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            drawTicks(ring = ring, stroke = stroke, tickColor = tickColor)
            drawScaleLabels(ring = ring, stroke = stroke, measurer = measurer, color = tickColor)

            if (target != null) {
                drawTargetMark(
                    ring = ring,
                    stroke = stroke,
                    fraction = fractionOf(target.coerceIn(SPEED_MIN, SPEED_MAX)),
                    color = targetColor,
                )
            }
        }

        GaugeReadout(
            speed = animatedSpeed,
            config = config,
            live = live,
            delta = delta,
            deltaWindow = deltaWindow,
            ring = ring,
        )

        if (phaseProgress != null) {
            PhaseMiniBar(
                progress = phaseProgress,
                config = config,
                ring = ring,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/**
 * 儀表下緣的階段迷你條。
 *
 * 對應參考稿的 `.phase-mini`：一條中間亮兩端淡的細線，線上一顆發光的點標出當前
 * 位置，兩端各一個階段名。
 *
 * @param progress 當前位置 0..1
 * @param config 玻璃主題參數
 * @param modifier 外部修飾符
 */
@Composable
private fun PhaseMiniBar(
    progress: Float,
    config: GlassConfig = LocalGlassConfig.current,
    ring: Dp,
    modifier: Modifier = Modifier,
) {
    val clamped = progress.coerceIn(0f, 1f)
    Column(
        modifier = modifier.width(ring * PHASE_WIDTH_RATIO),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(PHASE_DOT_SIZE),
            contentAlignment = Alignment.CenterStart,
        ) {
            // 兩端淡出的細線，讓它看起來是一段軌跡而不是一根量尺
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .align(Alignment.Center)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                config.accentToneColor.copy(alpha = PHASE_LINE_ALPHA),
                                Color.Transparent,
                            )
                        )
                    )
            )
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .offset(x = (maxWidth - PHASE_DOT_SIZE) * clamped)
                        .size(PHASE_DOT_SIZE)
                        .clip(CircleShape)
                        .background(config.accentToneColor)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val labelSize = (ring.value * PHASE_LABEL_RATIO).sp
            Text(stringResource(R.string.phase_relax), color = config.mutedContentColor, fontSize = labelSize)
            Text(stringResource(R.string.phase_sprint), color = config.mutedContentColor, fontSize = labelSize)
        }
    }
}

/** 儀表中央的讀數。 */
@Composable
private fun GaugeReadout(
    speed: Float,
    config: GlassConfig = LocalGlassConfig.current,
    live: Boolean,
    delta: Float?,
    deltaWindow: String?,
    ring: Dp,
) {
    val scale = ring.value
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            text = stringResource(R.string.metric_speed),
            color = config.mutedContentColor,
            fontSize = (scale * CAPTION_RATIO).sp,
        )
        val valueColor = if (live) config.screenContentColor else config.mutedContentColor
        val valueSize = (scale * VALUE_RATIO).sp
        if (config.visualStyle == GlassVisualStyle.DIGITAL) {
            SevenSegmentText(text = SPEED_FORMAT.format(speed), color = valueColor, fontSize = valueSize)
        } else {
            Text(
                text = SPEED_FORMAT.format(speed),
                color = valueColor,
                fontSize = valueSize,
                fontWeight = FontWeight.Light,
                // 參考稿是 `letter-spacing: -.07em`：這麼大的細體數字不收字距的話
                // 會散成一排，佔滿盤面還顯得鬆
                letterSpacing = valueSize * VALUE_TRACKING,
                // `line-height: 1`，否則上下各留一截把讀數推離盤心
                lineHeight = valueSize,
            )
        }
        // 單位獨立一行、置中。參考稿的 `.gauge-copy strong` 是 display: block，
        // 後面的 em 自然落到下一行——貼在數字右邊會把讀數的重心拉偏。
        Text(
            text = stringResource(R.string.unit_kmh),
            color = config.mutedContentColor,
            fontSize = (scale * UNIT_RATIO).sp,
        )
        // 凍結時不報「比多久前」——計時停了，這個比較沒有意義
        if (delta != null && deltaWindow != null && live) {
            Text(
                text = stringResource(R.string.gauge_delta, deltaWindow, formatDelta(delta)),
                color = config.quietAccentColor,
                fontSize = (scale * DELTA_RATIO).sp,
                // `.gauge-copy span` 是 `font-weight: 750`，三行小字裡只有它加粗
                fontWeight = FontWeight.Bold,
                // `.gauge-copy span` 是 `margin-top: 9px` 配 238 的錶盤
                modifier = Modifier.padding(top = ring * DELTA_GAP_RATIO),
            )
        }
    }
}

/** 增量帶正負號，`%+.1f` 在負值時會自己給出 `-`。 */
private fun formatDelta(delta: Float): String = DELTA_FORMAT.format(delta)

/**
 * 31 根刻度：每 1 km/h 一根，每 5 km/h 一根長的。
 *
 * 用 [rotate] 把畫布轉到刻度角度再畫一條垂直線段，比逐根算 sin/cos 少一半算式，
 * 端點也不會對不齊。
 */
private fun DrawScope.drawTicks(ring: Float, stroke: Float, tickColor: Color) {
    val outer = ring / 2f - stroke - TICK_GAP_RATIO * ring
    repeat(MINOR_TICK_COUNT + 1) { index ->
        val major = index % MAJOR_EVERY == 0
        val length = ring * if (major) MAJOR_TICK_LEN_RATIO else MINOR_TICK_LEN_RATIO
        val width = ring * if (major) MAJOR_TICK_WIDTH_RATIO else MINOR_TICK_WIDTH_RATIO
        val alpha = if (major) MAJOR_TICK_ALPHA else MINOR_TICK_ALPHA
        // 刻度 0 在弧的起點，而下面畫的是一條 12 點鐘方向的線段，
        // 所以旋轉角要在起點角度上再補 90°
        rotate(degrees = START_ANGLE_DEG + index * MINOR_STEP_DEG + 90f) {
            drawLine(
                color = tickColor.copy(alpha = alpha),
                start = Offset(center.x, center.y - outer),
                end = Offset(center.x, center.y - outer + length),
                strokeWidth = width,
            )
        }
    }
}

/** 主刻度旁的數字（0、5、…、30）。 */
private fun DrawScope.drawScaleLabels(
    ring: Float,
    stroke: Float,
    measurer: TextMeasurer,
    color: Color,
) {
    val radius = ring / 2f - stroke - ring * LABEL_INSET_RATIO
    val style = TextStyle(color = color, fontSize = (ring * LABEL_SIZE_RATIO).toSp())
    repeat(MAJOR_TICK_COUNT) { index ->
        val angleRad = Math.toRadians((START_ANGLE_DEG + index * MAJOR_STEP_DEG).toDouble())
        val layout = measurer.measure((index * MAJOR_EVERY).toString(), style)
        // drawText 收的是文字左上角，減掉一半尺寸才會以刻度為中心
        drawText(
            textLayoutResult = layout,
            topLeft = Offset(
                center.x + (cos(angleRad) * radius).toFloat() - layout.size.width / 2f,
                center.y + (sin(angleRad) * radius).toFloat() - layout.size.height / 2f,
            ),
        )
    }
}

/** 目標速度的標記：環上的一小段亮弧。 */
private fun DrawScope.drawTargetMark(ring: Float, stroke: Float, fraction: Float, color: Color) {
    val inset = stroke / 2f
    drawArc(
        color = color,
        startAngle = START_ANGLE_DEG + SWEEP_DEG * fraction - TARGET_MARK_DEG / 2f,
        sweepAngle = TARGET_MARK_DEG,
        useCenter = false,
        topLeft = Offset(inset, inset),
        size = Size(ring - stroke, ring - stroke),
        style = Stroke(width = stroke),
    )
}

/** 速度在量程中的位置（0..1）。 */
internal fun fractionOf(speed: Float): Float =
    ((speed - SPEED_MIN) / (SPEED_MAX - SPEED_MIN)).coerceIn(0f, 1f)

/** 某個速度對應的畫布角度，測試用。 */
internal fun gaugeAngleOf(speed: Float): Float = START_ANGLE_DEG + SWEEP_DEG * fractionOf(speed)

// ---------- spec 3.4 的實測值 ----------

/** 量程 0–30 km/h。 */
internal const val SPEED_MIN = 0f
internal const val SPEED_MAX = 30f

/**
 * 弧的起點。
 *
 * spec 寫的 218° 是從 12 點鐘量的，Compose 的 0° 在 3 點鐘，所以減 90。
 * 換算後弧從左下出發、順時針掃到右下，缺口正對正下方。
 */
internal const val START_ANGLE_DEG = 218f - 90f

/** 弧的總跨度。 */
internal const val SWEEP_DEG = 286f

/** 每 5 km/h 一根主刻度，共 7 根、6 段。 */
private const val MAJOR_EVERY = 5
internal const val MAJOR_TICK_COUNT = 7
internal const val MINOR_TICK_COUNT = 30

/** 47.667°，與 spec 一致。 */
internal const val MAJOR_STEP_DEG = SWEEP_DEG / (MAJOR_TICK_COUNT - 1)

/** 9.533°，與 spec 一致。 */
internal const val MINOR_STEP_DEG = SWEEP_DEG / MINOR_TICK_COUNT

// ---------- 以下是視覺比例，隨環的大小縮放 ----------

/**
 * 環寬佔直徑的比例。
 *
 * 必須等於 [FACE_INSET_RATIO]：參考稿的環是 `conic-gradient` 鋪滿整圓、再用
 * `::after { inset: 18px }` 把中心蓋掉，所以環內緣和盤面外緣是**同一條線**。
 * 這裡兩個比例對不上的話，環和盤面之間會露出一圈屏底色的縫。
 */
private const val RING_STROKE_RATIO = 18f / 238f
private const val TICK_GAP_RATIO = 0.018f
private const val MAJOR_TICK_LEN_RATIO = 0.052f
private const val MINOR_TICK_LEN_RATIO = 0.028f
private const val MAJOR_TICK_WIDTH_RATIO = 0.008f
private const val MINOR_TICK_WIDTH_RATIO = 0.005f
private const val MAJOR_TICK_ALPHA = 0.9f
private const val MINOR_TICK_ALPHA = 0.45f

/**
 * 數字的內縮量。
 *
 * 主刻度的內端落在半徑 0.375R（= 0.5R − 環寬 − 間隙 − 刻度長）。數字要壓在
 * 0.31R 附近：再往內就會撞到中央讀數底下那行增量（0 與 30 正好在同一個高度帶），
 * 再往外就疊上刻度。
 */
private const val LABEL_INSET_RATIO = 0.128f
private const val LABEL_SIZE_RATIO = 0.042f

/** 目標速度標記的弧長。 */
private const val TARGET_MARK_DEG = 2.5f

private const val SPEED_FORMAT = "%.1f"
private const val DELTA_FORMAT = "%+.1f"

/**
 * 中央讀數各行相對環直徑的比例。
 *
 * 以豎屏 280dp 環為基準：讀數 48sp、標題 11sp、單位 12sp，換算成比例後小環也不會爆版。
 * 讀數原本是 40sp，中央那塊留白太多，放大兩號才撐得起錶盤。
 */
/**
 * 中央讀數相對錶盤直徑的比例。
 *
 * 參考稿 `.gauge-copy strong` 是 47px 配 238px 的錶盤（橫屏 `.l-hero` 是 56px，
 * 錶盤也跟著大）。這個比例要照抄，字號單獨調會讓讀數在盤面上的重心跑掉。
 */
private const val VALUE_RATIO = 47f / 238f

/** `.gauge-copy strong` 的 `letter-spacing: -.07em`。 */
private const val VALUE_TRACKING = -0.07f
/**
 * 讀數區三行小字的字號比例。
 *
 * 參考稿的 `.gauge-copy` 裡 small（當前速度）、em（km/h）、span（比 X 前）
 * **都是 8px**，只有 span 加粗到 750。之前三行各給了一個數（.039 / .043 / .034），
 * 於是單位比標題大、對比行又比單位小，三行大小全不一樣。
 */
private const val CAPTION_RATIO = 8f / 238f

/** 增量那一行比標題再小一號：它和左右兩側的 0 / 30 在同一個高度帶，太寬會撞上。 */
/**
 * 對比行的字號比例。
 *
 * 參考稿的 `.gauge-copy` 三行小字都是 8px，但那一版是英數與網頁字體。落到真機上
 * 這一行是中文，同樣字號寬出一截，跟盤面底部的 `0` / `30` 刻度擠在同一條線上——
 * 錶盤放大之後直接疊成「+1.630」。單獨收一號，讓它待在兩個刻度之間。
 */
private const val DELTA_RATIO = 7f / 238f
private const val DELTA_GAP_RATIO = 9f / 238f
private const val UNIT_RATIO = CAPTION_RATIO

/** 參考稿的 .phase-mini 是 132×18、點 9px，同樣換成相對環的比例。 */
private const val PHASE_WIDTH_RATIO = 0.47f
private const val PHASE_LABEL_RATIO = 0.029f
private val PHASE_DOT_SIZE = 9.dp
private const val PHASE_LINE_ALPHA = 0.45f

/**
 * 錶盤內圈的盤面。
 *
 * 對應參考稿的 `.p-gauge::after`：一塊比外環內縮 [FACE_INSET_RATIO] 的圓，刻度、
 * 讀數都畫在它上面。少了它，刻度就是直接浮在頁面底色上，錶盤看起來「沒有面」。
 *
 * 每套風格的盤面材質不同，所以按 [style] 分派；要做新的錶盤版本時，
 * 加一個分支即可，不必動外環與刻度的繪製。
 *
 * @param style 當前視覺風格
 * @param fallbackFace 盤面主色（panel 那一階）。**必須跟著調色板走**——寫死一個深色的話，
 *   Nordic 這種淺色主題上就是一塊黑盤。
 * @param lit 屏內是否為淺色表面。淺色才把盤面畫成受光的凸圓，深色維持純色
 * @param litFace 受光側的顏色（lift 那一階）
 * @param sheenFace 光心的顏色（sheen 那一階）
 * @param edgeFace 背光外緣的顏色（panel 往 sunk 靠一點）
 */
private fun DrawScope.drawDialFace(
    style: GlassVisualStyle,
    fallbackFace: Color,
    lit: Boolean = false,
    litFace: Color = fallbackFace,
    sheenFace: Color = fallbackFace,
    edgeFace: Color = fallbackFace,
) {
    val ring = size.minDimension
    val radius = ring / 2f - ring * FACE_INSET_RATIO
    val center = Offset(size.width / 2f, size.height / 2f)

    when (style) {
        // 實體錶盤：左上受光、右下背光的金屬面，外緣再壓一圈暗收邊
        GlassVisualStyle.TACTILE -> {
            drawCircle(
                brush = Brush.linearGradient(
                    0f to TACTILE_FACE_TOP,
                    0.66f to TACTILE_FACE_BOTTOM,
                    start = Offset(center.x - radius, center.y - radius),
                    end = Offset(center.x + radius, center.y + radius),
                ),
                radius = radius,
                center = center,
            )
            // 沿對角再壓一道暗，讓盤面是「凸起的圓盤」。
            // 早先用的是整圈徑向暗邊——四周一樣暗，看起來就變成一個凹坑了。
            drawCircle(
                brush = Brush.linearGradient(
                    0.55f to Color.Transparent,
                    1f to FACE_RIM_SHADOW,
                    start = Offset(center.x - radius, center.y - radius),
                    end = Offset(center.x + radius, center.y + radius),
                ),
                radius = radius,
                center = center,
            )
        }

        // 數碼儀表是淺色面板，盤面跟著面板走，不另外壓深
        GlassVisualStyle.DIGITAL -> drawCircle(DIGITAL_FACE, radius, center)

        // 其餘風格：淺色屏畫成一塊受光的凸圓，對應參考稿為淺色配色覆寫的
        // `radial-gradient(circle at 38% 28%, #fff 0%, #f8f8f8 40%, #ececec 74%, #dedede 100%)`；
        // 深色屏維持純色（參考稿基礎主題的 `#15191A`）。
        //
        // 四段顏色全部由呼叫端從表面 token 取，這裡一個色都不寫死。
        else -> if (!lit) {
            drawCircle(fallbackFace, radius, center)
        } else {
            drawCircle(
                brush = Brush.radialGradient(
                    0f to sheenFace,
                    // 參考稿 40% 那一站是 #F8F8F8，正好落在光心與 lift 的中間
                    0.4f to lerp(sheenFace, litFace, FACE_LIT_MIX),
                    0.74f to fallbackFace,
                    1f to edgeFace,
                    // 參考稿的光心在 38% / 28%，換算成相對圓心的偏移
                    center = Offset(
                        center.x - radius * FACE_LIGHT_DX,
                        center.y - radius * FACE_LIGHT_DY,
                    ),
                    radius = radius * FACE_LIGHT_SPREAD,
                ),
                radius = radius,
                center = center,
            )
        }
    }
}

/** `circle at 38% 28%` 相對圓心的偏移，以及讓漸層鋪滿整個盤面所需的擴散。 */
private const val FACE_LIGHT_DX = 0.24f
private const val FACE_LIGHT_DY = 0.44f
private const val FACE_LIGHT_SPREAD = 1.35f

/** 光心到 lift 之間那一站的位置。 */
private const val FACE_LIT_MIX = 0.5f

/** 盤面外緣往 sunk 靠多少。參考稿收在 `#dedede`，介於 panel 與 sunk 之間。 */
private const val FACE_EDGE_BLEND = 0.3f

/** 參考稿 `.p-gauge::after` 是 `inset: 18px` 於 238px 的環。 */
private const val FACE_INSET_RATIO = 18f / 238f

private val DIGITAL_FACE = Color(0xFFF2F4F1)
private val TACTILE_FACE_TOP = Color(0xFF3A3B3C)
private val TACTILE_FACE_BOTTOM = Color(0xFF171819)
private val FACE_RIM_SHADOW = Color(0x66000000)

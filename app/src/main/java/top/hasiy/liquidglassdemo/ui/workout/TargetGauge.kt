package top.hasiyliquidglassdemo.ui.workout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import top.hasiy.designsystem.GlassConfig
import top.hasiy.designsystem.LocalGlassConfig
import top.hasiy.designsystem.SevenSegmentText
import top.hasiy.designsystem.accentToneColor
import top.hasiy.designsystem.dangerColor
import top.hasiy.designsystem.mutedContentColor
import top.hasiy.designsystem.screenContentColor
import top.hasiy.designsystem.sunkColor
import top.hasiy.designsystem.trackColor
import top.hasiy.designsystem.tokens.GlassVisualStyle
import kotlin.math.cos
import kotlin.math.sin

/**
 * 目標值可視化的雙環儀表。
 *
 * 對應參考稿 08 的 `.cluster`。照汽車組合儀表的分工：一塊表兩條弧——
 * 最外圈細弧是**目標值**，內圈粗弧是**當前值**，兩個弧端之間的缺口就是還差多少。
 *
 * 配色只有一條規則：整塊表上只允許出現一種高飽和色，也就是強調色，它**專屬於當前值**。
 * 目標弧走低飽和的中性色（[GlassConfig.sunkColor] 那一階），靠明度而不是色相跟強調色
 * 分開——兩種飽和色擺在一起必然打架。紅只留給「超出目標」。
 *
 * 幾何全部由邊長驅動，所以同一份代碼能畫大表也能畫特寫：
 * ```
 * 50%   ~ 48.1%  目標弧
 * 48.1% ~ 46.5%  間隙
 * 46.5% ~ 40.4%  當前值弧
 * 40.4% ~ 0      盤面：刻度在外、刻度數字在內、讀數居中
 * ```
 *
 * @param label 中心讀數上方的標題，例如「當前速度」
 * @param current 當前值
 * @param target 目標值。[readOnly] 為 true 時不畫目標弧
 * @param max 量程上限
 * @param majorStep 每隔多少標一個刻度數字
 * @param unit 單位文案
 * @param decimals 讀數保留幾位小數
 * @param readOnly 裝置不支援調節。目標弧整條淡出，儀表退回單弧形態
 * @param config 玻璃主題參數
 * @param modifier 外部修飾符
 */
@Composable
fun TargetGauge(
    label: String,
    current: Float,
    target: Float,
    max: Float,
    majorStep: Float,
    unit: String,
    decimals: Int = 1,
    readOnly: Boolean = false,
    config: GlassConfig = LocalGlassConfig.current,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val accent = config.accentToneColor
    val danger = config.dangerColor
    // 目標弧：低飽和的中性色，走 muted 那一檔。
    //
    // 不能用 sunk：那是「比面板壓暗一階」的**表面**色，淺色配色下是 #C9C9C9，
    // 壓在屏底 #D1D1D1 上跟軌道幾乎同色，整條目標弧看不見。
    // muted 是文字級的中性色（淺色 #545454、深色 #818A85），兩邊對屏底都在 5:1 以上，
    // 正好對上參考稿的「靠明度而不是色相跟強調色分開」。
    val targetColor = config.mutedContentColor
    val track = config.trackColor
    val state = gaugeStateOf(current, target, readOnly)
    // 量程的下限保護。
    //
    // 呼叫端目前傳的都是編譯期常數（30f / 16f），除以 0 的路徑走不到；但這個保護
    // 便宜而且擋的是一種很難查的壞法：`x / 0f` 得到 NaN，而 NaN **穿得過**
    // `coerceIn`——`NaN < min` 與 `NaN > max` 都是 false，clamp 會把 NaN 原樣送出去。
    // NaN 進到 drawArc 的 sweepAngle 只會讓整個環**畫不出來**，沒有任何報錯。
    // 所以保護要做在除法之前，不是夾在結果上。
    val safeMax = max.coerceAtLeast(CL_MAX_FLOOR)

    // 邊長由呼叫端用 Modifier.size 給定；這裡的 aspectRatio 只是兜底，
    // 讓沒給尺寸時仍是正方形。**不能**靠 aspectRatio 自己選邊：它要嘛按寬、
    // 要嘛按高，取不了兩者的較小值——按寬會把步進器頂出畫面，按高會撐爆列寬。
    BoxWithConstraints(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, maxHeight)
        Canvas(Modifier.fillMaxSize()) {
            drawTargetRing(
                fraction = (target / safeMax).coerceIn(0f, 1f),
                color = targetColor,
                track = track,
                dim = state == GaugeState.READ_ONLY,
            )
            drawCurrentRing(
                fraction = (current / safeMax).coerceIn(0f, 1f),
                targetFraction = (target / safeMax).coerceIn(0f, 1f),
                accent = accent,
                danger = danger,
                track = track,
                over = state == GaugeState.OVER,
            )
            drawGaugeTicks(max = safeMax, color = config.mutedContentColor)
            drawGaugeScale(
                max = safeMax,
                majorStep = majorStep,
                measurer = measurer,
                color = config.mutedContentColor,
                side = side,
            )
        }

        GaugeReading(
            label = label,
            value = current,
            unit = unit,
            decimals = decimals,
            side = side,
            config = config,
        )
    }
}

/** 儀表的四種狀態。 */
enum class GaugeState {
    /** 當前值低於目標，兩個弧端之間有缺口。 */
    CHASING,

    /** 弧端對齊。 */
    HIT,

    /** 當前值超出目標，超出的那一段染成警示色。 */
    OVER,

    /** 裝置不支援調節，目標弧淡出。 */
    READ_ONLY,
}

/**
 * 由當前值與目標值判斷儀表狀態。
 *
 * @param current 當前值
 * @param target 目標值
 * @param readOnly 裝置是否只讀
 */
fun gaugeStateOf(current: Float, target: Float, readOnly: Boolean): GaugeState = when {
    readOnly -> GaugeState.READ_ONLY
    // 浮點比較留一點餘量：0.1 的步進累加幾次就湊不出精確相等
    kotlin.math.abs(current - target) < CL_EPSILON -> GaugeState.HIT
    current > target -> GaugeState.OVER
    else -> GaugeState.CHASING
}

/** 目標弧：外圈細環，加兩端圓頭。 */
private fun DrawScope.drawTargetRing(fraction: Float, color: Color, track: Color, dim: Boolean) {
    val ring = size.minDimension
    val stroke = ring * CL_TARGET_STROKE
    val inset = stroke / 2f
    val arcSize = Size(ring - stroke, ring - stroke)
    val topLeft = Offset(inset, inset)
    val alpha = if (dim) CL_READ_ONLY_ALPHA else 1f

    drawArc(
        color = track.copy(alpha = track.alpha * alpha),
        startAngle = CL_START_DEG,
        sweepAngle = CL_SWEEP_DEG,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = stroke, cap = StrokeCap.Round),
    )
    if (fraction > 0f) {
        drawArc(
            color = color.copy(alpha = color.alpha * alpha),
            startAngle = CL_START_DEG,
            sweepAngle = CL_SWEEP_DEG * fraction,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

/**
 * 當前值弧：內圈粗環。
 *
 * 超出目標時分兩段畫——到目標那一段仍是強調色，超出的那一段染成警示色，
 * 弧長就是超了多少。這是全表唯一允許出現的第二種彩色。
 */
private fun DrawScope.drawCurrentRing(
    fraction: Float,
    targetFraction: Float,
    accent: Color,
    danger: Color,
    track: Color,
    over: Boolean,
) {
    val ring = size.minDimension
    val stroke = ring * CL_CURRENT_STROKE
    val inset = ring * CL_CURRENT_INSET + stroke / 2f
    val arcSize = Size(ring - inset * 2f, ring - inset * 2f)
    val topLeft = Offset(inset, inset)
    val cap = StrokeCap.Round

    drawArc(
        color = track,
        startAngle = CL_START_DEG,
        sweepAngle = CL_SWEEP_DEG,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = stroke, cap = cap),
    )
    if (over) {
        drawArc(
            color = danger,
            startAngle = CL_START_DEG,
            sweepAngle = CL_SWEEP_DEG * fraction,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = cap),
        )
        drawArc(
            color = accent,
            startAngle = CL_START_DEG,
            sweepAngle = CL_SWEEP_DEG * targetFraction,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = cap),
        )
    } else if (fraction > 0f) {
        drawArc(
            color = accent,
            startAngle = CL_START_DEG,
            sweepAngle = CL_SWEEP_DEG * fraction,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = cap),
        )
    }
}

/** 刻度：每 1 單位一根短刻度，落在盤面外緣。 */
private fun DrawScope.drawGaugeTicks(max: Float, color: Color) {
    val ring = size.minDimension
    val center = Offset(size.width / 2f, size.height / 2f)
    val outer = ring / 2f - ring * CL_TICK_INSET
    val steps = max.toInt().coerceAtLeast(1)
    repeat(steps + 1) { index ->
        val radians = ((CL_START_DEG + CL_SWEEP_DEG * index / steps) * Math.PI / 180f).toFloat()
        val dirX = cos(radians)
        val dirY = sin(radians)
        drawLine(
            color = color.copy(alpha = CL_TICK_ALPHA),
            start = Offset(center.x + dirX * (outer - ring * CL_TICK_LENGTH), center.y + dirY * (outer - ring * CL_TICK_LENGTH)),
            end = Offset(center.x + dirX * outer, center.y + dirY * outer),
            strokeWidth = ring * CL_TICK_WIDTH,
        )
    }
}

/** 主刻度的數字，排在盤面內側。 */
private fun DrawScope.drawGaugeScale(
    max: Float,
    majorStep: Float,
    measurer: TextMeasurer,
    color: Color,
    side: Dp,
) {
    val ring = size.minDimension
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = ring * CL_SCALE_RADIUS
    val style = TextStyle(color = color, fontSize = (side.value * CL_SCALE_TEXT_RATIO).sp, fontWeight = FontWeight.SemiBold)
    // 步長的下限保護：0 或負值會讓下面這個 while 永遠不收斂，而它跑在
    // DrawScope 裡——也就是主線程的繪製階段，卡住就是 ANR，不是畫錯。
    // 呼叫端目前傳的都是常數（5f / 2f），這同樣是防禦性的。
    val step = majorStep.coerceAtLeast(CL_STEP_FLOOR)
    var value = 0f
    while (value <= max + CL_EPSILON) {
        val radians = ((CL_START_DEG + CL_SWEEP_DEG * value / max) * Math.PI / 180f).toFloat()
        val text = if (step % 1f == 0f) value.toInt().toString() else value.toString()
        val measured = measurer.measure(text, style)
        drawText(
            textLayoutResult = measured,
            topLeft = Offset(
                center.x + cos(radians) * radius - measured.size.width / 2f,
                center.y + sin(radians) * radius - measured.size.height / 2f,
            ),
        )
        value += step
    }
}

/**
 * 中心讀數：標題、當前值、單位三行。
 *
 * 參考稿刻意只放三行——帶上步進器那四行時內容半高約 22%，會跟半徑 26% 那圈
 * 刻度數字打架（正上方撞標題、底部撞按鈕）。步進器移到表外，見 [TargetStepper]。
 */
@Composable
private fun GaugeReading(
    label: String,
    value: Float,
    unit: String,
    decimals: Int,
    side: Dp,
    config: GlassConfig,
) {
    val scale = side.value
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            color = config.mutedContentColor,
            fontSize = (scale * CL_CAPTION_RATIO).sp,
        )
        val text = formatGaugeValue(value, decimals)
        val valueSize = (scale * CL_VALUE_RATIO).sp
        if (config.visualStyle == GlassVisualStyle.DIGITAL) {
            SevenSegmentText(text = text, color = config.screenContentColor, fontSize = valueSize)
        } else {
            Text(
                text = text,
                color = config.screenContentColor,
                fontSize = valueSize,
                fontWeight = FontWeight.Light,
                letterSpacing = valueSize * CL_VALUE_TRACKING,
                lineHeight = valueSize,
            )
        }
        Text(
            text = unit,
            color = config.mutedContentColor,
            fontSize = (scale * CL_CAPTION_RATIO).sp,
        )
    }
}

/** 讀數格式：按位數補齊，避免 24.8 與 25 兩種寬度來回跳。 */
fun formatGaugeValue(value: Float, decimals: Int): String =
    if (decimals <= 0) value.toInt().toString() else "%.${decimals}f".format(value)

/** 參考稿 `.cluster`：起點 218°、掃過 286°。 */
/**
 * 起點角。
 *
 * 參考稿寫的是 218°，但那是 CSS `conic-gradient` 的角度——它從**12 點**起算。
 * Compose 的 `drawArc` 從**3 點**起算，兩者差 90°。不減這一下，整條弧與刻度
 * 都會逆着轉 90 度：0 跑到左上、刻度數字順序反過來。
 */
private const val CL_START_DEG = 218f - 90f
private const val CL_SWEEP_DEG = 286f

/** 目標弧寬 1.92% 邊長；當前值弧內縮 3.46%、寬 6.15%。 */
private const val CL_TARGET_STROKE = 0.0192f
private const val CL_CURRENT_INSET = 0.0346f
private const val CL_CURRENT_STROKE = 0.0615f

/** 刻度：內縮 11.5%、長 3%、寬 0.4%。 */
private const val CL_TICK_INSET = 0.115f
private const val CL_TICK_LENGTH = 0.03f
private const val CL_TICK_WIDTH = 0.004f
private const val CL_TICK_ALPHA = 0.5f

/** 刻度數字排在半徑 26% 處，字號佔邊長 4.2%。 */
private const val CL_SCALE_RADIUS = 0.26f
private const val CL_SCALE_TEXT_RATIO = 0.042f

/** 中心讀數：標題／單位 4.2%，數值 21%，字距 −5%。 */
private const val CL_CAPTION_RATIO = 0.042f
private const val CL_VALUE_RATIO = 0.21f
private const val CL_VALUE_TRACKING = -0.05f

/** 只讀時目標弧的濃度。 */
private const val CL_READ_ONLY_ALPHA = 0.2f

/** 浮點比較的餘量。 */
private const val CL_EPSILON = 1e-4f

/** 量程與步長的下限，見 [TargetGauge] 與 [drawGaugeScale] 裡的說明。 */
private const val CL_MAX_FLOOR = 1e-3f
private const val CL_STEP_FLOOR = 1e-2f

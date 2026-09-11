package top.hasiyliquidglassdemo.ui.workout

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.hasiy.designsystem.GlassConfig
import top.hasiy.designsystem.LocalGlassConfig
import top.hasiy.designsystem.accentToneColor
import top.hasiy.designsystem.lineColor
import top.hasiy.designsystem.mutedContentColor
import top.hasiy.designsystem.panelColor
import top.hasiy.designsystem.screenColor
import top.hasiy.designsystem.tactileKeycap
import top.hasiy.designsystem.tactilePanel
import top.hasiy.designsystem.tokens.GlassVisualStyle
import top.hasiy.designsystem.trackColor

/**
 * 底部那條目標調節條。
 *
 * 對應參考稿 09 的 `.l-tune`：三欄，左右各一組目標調節、中間一條進度。
 *
 * 為什麼 09 把調節從表下方移到這裡：中間那一塊換成地圖之後，兩塊表被壓到 204dp，
 * 表下方已經沒有位置放一組按鍵了。移到獨立一條還順帶解決了一件事——左右兩組
 * 按鍵落在同一水平線上，運動中不用重新找位置。
 *
 * 三段用具名 slot 而不是一串參數：中間那段在路線模式報「已完成 / 計劃」、
 * 在賽道模式報「本圈 / 全程圈數」，講的不是同一件事，硬塞成同一組參數
 * 只會讓兩邊都得傳一半空值。
 *
 * @param config 玻璃主題參數
 * @param modifier 外部修飾符
 * @param left 左側那組調節（速度）
 * @param center 中間那段進度
 * @param right 右側那組調節（阻力）
 */
@Composable
fun TargetTuneBar(
    config: GlassConfig = LocalGlassConfig.current,
    modifier: Modifier = Modifier,
    left: @Composable () -> Unit,
    center: @Composable () -> Unit,
    right: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(TUNE_CORNER)
    val surface = if (config.visualStyle == GlassVisualStyle.TACTILE) {
        Modifier.tactilePanel(TUNE_CORNER)
    } else {
        Modifier.clip(shape).background(config.panelColor).border(1.dp, config.lineColor, shape)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(surface)
            .padding(horizontal = TUNE_PADDING_H, vertical = TUNE_PADDING_V),
        horizontalArrangement = Arrangement.spacedBy(TUNE_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 左右兩欄等寬（參考稿 `1fr auto 1fr`）：不等寬的話兩組按鍵就不對稱，
        // 左邊那組會被中間的文案推著走。
        //
        // 中間那段必須**先定住寬度**再放進來。Row 會先量沒有 weight 的子項，
        // 而進度條內部是 fillMaxWidth——不給它上限，它會把整行吃光，兩側的
        // weight 只剩 0 寬，兩組按鍵就整組消失了。
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) { left() }
        Box(modifier = Modifier.width(TUNE_PROGRESS_WIDTH)) { center() }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) { right() }
    }
}

/**
 * 調節條上的一組：標籤、減、值、加。
 *
 * @param label 標籤（目標速度 / 目標阻力）
 * @param value 當前目標值
 * @param unit 單位文案
 * @param decimals 顯示幾位小數
 * @param state 儀表狀態，決定數值的顏色
 * @param onStep 加減一格，參數為方向（+1 / −1）
 * @param stepDownLabel 減鍵的無障礙標籤
 * @param stepUpLabel 加鍵的無障礙標籤
 * @param config 玻璃主題參數
 * @param modifier 外部修飾符
 */
@Composable
fun TuneUnit(
    label: String,
    value: Float,
    unit: String,
    decimals: Int,
    state: GaugeState,
    onStep: (Int) -> Unit,
    stepDownLabel: String,
    stepUpLabel: String,
    config: GlassConfig = LocalGlassConfig.current,
    modifier: Modifier = Modifier,
) {
    val readOnly = state == GaugeState.READ_ONLY
    val tone = gaugeToneOf(state, config)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(TUNE_UNIT_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = config.mutedContentColor,
            fontSize = TUNE_LABEL_SIZE,
        )
        TuneKey("−", stepDownLabel, readOnly, config) { onStep(-1) }
        Row(
            modifier = Modifier.widthIn(min = TUNE_VALUE_MIN),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = if (readOnly) TUNE_DASH else formatGaugeValue(value, decimals),
                color = tone,
                fontSize = TUNE_VALUE_SIZE,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.alignByBaseline(),
            )
            if (unit.isNotEmpty() && !readOnly) {
                Text(
                    text = unit,
                    color = tone,
                    fontSize = TUNE_UNIT_SIZE,
                    modifier = Modifier.padding(start = 2.dp).alignByBaseline(),
                )
            }
        }
        TuneKey("＋", stepUpLabel, readOnly, config) { onStep(+1) }
    }
}

/**
 * 調節條上的一顆鍵。
 *
 * 視覺 30dp、命中 44dp。底色是**固定的中性色**，不跟儀表狀態走：參考稿只在只讀那一檔
 * 覆寫過按鍵樣式，達標與超標只換數值的顏色——按鍵一直是「可以按的那個東西」，
 * 它的顏色不該用來報狀態。
 */
@Composable
private fun TuneKey(
    label: String,
    description: String,
    disabled: Boolean,
    config: GlassConfig,
    onClick: () -> Unit,
) {
    val tactile = config.visualStyle == GlassVisualStyle.TACTILE
    val surface = if (tactile) {
        Modifier.tactileKeycap(TUNE_KEY_SIZE / 2)
    } else {
        Modifier.clip(CircleShape).background(config.mutedContentColor)
    }
    Box(
        modifier = Modifier
            .size(TUNE_KEY_TOUCH)
            .then(
                if (disabled) Modifier
                else Modifier.tactileClickable(onClickLabel = description, onClick = onClick)
            )
            .alpha(if (disabled) TUNE_DISABLED_ALPHA else 1f),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.size(TUNE_KEY_SIZE).then(surface),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                // 壓在實色鍵上的字用屏底色：淺色配色下內容色是近黑，壓在深底鍵上讀不出來
                text = label,
                color = if (tactile) config.mutedContentColor else config.screenColor,
                fontSize = TUNE_KEY_TEXT,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/**
 * 調節條中間那段進度。
 *
 * @param fraction 已完成比例（0..1）
 * @param startText 左下角文案
 * @param endText 右下角文案
 * @param config 玻璃主題參數
 * @param modifier 外部修飾符
 */
@Composable
fun TuneProgress(
    fraction: Float,
    startText: String,
    endText: String,
    config: GlassConfig = LocalGlassConfig.current,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TUNE_BAR_HEIGHT)
                .clip(RoundedCornerShape(TUNE_BAR_HEIGHT / 2))
                .background(config.trackColor),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(TUNE_BAR_HEIGHT / 2))
                    .background(config.accentToneColor),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = startText, color = config.mutedContentColor, fontSize = TUNE_LEGEND_SIZE)
            Text(text = endText, color = config.mutedContentColor, fontSize = TUNE_LEGEND_SIZE)
        }
    }
}

private const val TUNE_DASH = "—"

private val TUNE_CORNER = 16.dp
private val TUNE_PADDING_H = 10.dp
private val TUNE_PADDING_V = 5.dp
private val TUNE_GAP = 12.dp
private val TUNE_UNIT_GAP = 8.dp
private val TUNE_LABEL_SIZE = 8.sp
private val TUNE_VALUE_MIN = 56.dp
private val TUNE_VALUE_SIZE = 15.sp
private val TUNE_UNIT_SIZE = 8.sp
private const val TUNE_DISABLED_ALPHA = 0.35f

/** 視覺 30dp、命中 44dp。 */
private val TUNE_KEY_SIZE = 30.dp
private val TUNE_KEY_TOUCH = 44.dp
private val TUNE_KEY_TEXT = 16.sp

/** 中間那段的寬度。參考稿 `min-width: 150px`，這裡定死——見上面那段說明。 */
private val TUNE_PROGRESS_WIDTH = 220.dp
private val TUNE_BAR_HEIGHT = 5.dp
private val TUNE_LEGEND_SIZE = 8.sp

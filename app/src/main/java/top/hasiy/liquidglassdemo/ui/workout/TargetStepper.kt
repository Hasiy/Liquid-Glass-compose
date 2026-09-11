package top.hasiyliquidglassdemo.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.hasiy.designsystem.GlassConfig
import top.hasiy.designsystem.LocalGlassConfig
import top.hasiy.designsystem.accentToneColor
import top.hasiy.designsystem.dangerColor
import top.hasiy.designsystem.mutedContentColor
import top.hasiy.designsystem.screenColor
import top.hasiy.designsystem.tokens.GlassVisualStyle
import top.hasiy.designsystem.tactileKeycap
import top.hasiyliquidglassdemo.R

/**
 * 目標值的步進器。
 *
 * 對應參考稿 08 的 `.stepper`。它**長在表下方**而不是回到底部工具條——調什麼就在哪兒改，
 * 手指落點和視覺焦點重合。參考稿刻意把它移出表盤：中心讀數帶上步進器共四行時，
 * 內容半高約 22%，會跟半徑 26% 那圈刻度數字打架。
 *
 * 底色跟著儀表狀態走：追趕時是目標色的淡底，達標轉強調色，超標轉警示色，
 * 只讀則整組灰掉禁用——不留下改不動的控件。
 *
 * @param value 當前目標值
 * @param unit 單位文案
 * @param decimals 顯示幾位小數
 * @param state 儀表狀態，決定底色與讀數色
 * @param onStep 加減一格，參數為方向（+1 / −1）
 * @param stepDownLabel 減鍵的無障礙標籤。設成必填而不是給個「調低」的預設值：
 *   讀屏使用者聽到光禿禿的「調低」不知道在調什麼，而只有呼叫端知道這一組是速度
 *   還是阻力。同 `TuneUnit` 的做法
 * @param stepUpLabel 加鍵的無障礙標籤，同上
 * @param config 玻璃主題參數
 * @param modifier 外部修飾符
 */
@Composable
fun TargetStepper(
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
    val shape = RoundedCornerShape(STEPPER_CORNER)

    Row(
        modifier = modifier
            .clip(shape)
            .background(tone.copy(alpha = STEPPER_FILL_ALPHA))
            .padding(STEPPER_PADDING),
        horizontalArrangement = Arrangement.spacedBy(STEPPER_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepKey("−", stepDownLabel, tone, readOnly, config) { onStep(-1) }
        Row(
            modifier = Modifier.widthIn(min = STEPPER_VALUE_MIN),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = if (readOnly) DASH else formatGaugeValue(value, decimals),
                color = tone,
                fontSize = STEPPER_VALUE_SIZE,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alignByBaseline(),
            )
            if (unit.isNotEmpty() && !readOnly) {
                Text(
                    text = unit,
                    color = tone,
                    fontSize = STEPPER_UNIT_SIZE,
                    modifier = Modifier
                        .padding(start = 3.dp)
                        .alignByBaseline()
                        .alpha(STEPPER_UNIT_ALPHA),
                )
            }
        }
        StepKey("＋", stepUpLabel, tone, readOnly, config) { onStep(+1) }
    }
}

/**
 * 步進器上的一顆鍵。
 *
 * 視覺 34dp，命中區撐到 44dp——運動中手會抖、會出汗，按鍵不能只有看起來那麼大。
 */
@Composable
private fun StepKey(
    label: String,
    description: String,
    tone: androidx.compose.ui.graphics.Color,
    disabled: Boolean,
    config: GlassConfig,
    onClick: () -> Unit,
) {
    val tactile = config.visualStyle == GlassVisualStyle.TACTILE
    val surface = if (tactile) {
        Modifier.tactileKeycap(STEP_KEY_SIZE / 2)
    } else {
        Modifier.clip(CircleShape).background(tone)
    }
    Box(
        modifier = Modifier
            .size(STEP_KEY_TOUCH)
            .then(if (disabled) Modifier else Modifier.tactileClickable(onClickLabel = description, onClick = onClick))
            .alpha(if (disabled) STEPPER_DISABLED_ALPHA else 1f),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.size(STEP_KEY_SIZE).then(surface),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                // 壓在實色鍵上的字要用「壓在強調色上的文字色」那一檔，
                // 不是內容色——淺色配色下內容色是近黑，壓在深底鍵上讀不出來
                color = if (tactile) config.mutedContentColor else config.screenColor,
                fontSize = STEP_KEY_TEXT,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/**
 * 儀表狀態對應的顏色。
 *
 * 目標胶囊、步進器、底部調節條都讀這一個——三處講的是同一件事（離目標還有多遠），
 * 各自寫一份 when 的話改了一處就對不上了。
 *
 * 「還在追」與「只讀」都用中性文字色：用 sunk 那個**表面**色的話，淺色配色下是
 * #C9C9C9，壓在屏底上整組控件都看不見。
 *
 * @param state 儀表狀態
 * @param config 玻璃主題參數
 */
fun gaugeToneOf(state: GaugeState, config: GlassConfig): Color = when (state) {
    GaugeState.HIT -> config.accentToneColor
    GaugeState.OVER -> config.dangerColor
    GaugeState.READ_ONLY -> config.mutedContentColor
    GaugeState.CHASING -> config.mutedContentColor
}

/**
 * 表下方的目標胶囊。
 *
 * 對應參考稿 09 的 `.target-chip`。09 把目標值的**調節**移到了底部那一條，
 * 表下方只留一個唸得出來的當前目標——所以這裡沒有按鍵，只有圓點、數值和一個詞。
 *
 * 達標時把「目標」換成「達標」而不是換位置或加圖示：運動中眼睛只掠過一下，
 * 位置一動就得重新找。
 *
 * @param value 目標值
 * @param decimals 顯示幾位小數
 * @param state 儀表狀態，決定顏色與那個詞
 * @param config 玻璃主題參數
 * @param modifier 外部修飾符
 */
@Composable
fun TargetChip(
    value: Float,
    decimals: Int,
    state: GaugeState,
    config: GlassConfig = LocalGlassConfig.current,
    modifier: Modifier = Modifier,
) {
    val tone = gaugeToneOf(state, config)
    val readOnly = state == GaugeState.READ_ONLY
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(CHIP_CORNER))
            .background(tone.copy(alpha = CHIP_FILL_ALPHA))
            .padding(horizontal = CHIP_PADDING_H, vertical = CHIP_PADDING_V),
        horizontalArrangement = Arrangement.spacedBy(CHIP_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(CHIP_DOT)
                .clip(CircleShape)
                .background(tone),
        )
        Text(
            text = if (readOnly) DASH else formatGaugeValue(value, decimals),
            color = tone,
            fontSize = CHIP_VALUE_SIZE,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(
                if (state == GaugeState.HIT) R.string.target_chip_hit else R.string.target_chip_target
            ),
            color = tone,
            fontSize = CHIP_LABEL_SIZE,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.alpha(CHIP_LABEL_ALPHA),
        )
    }
}

private val CHIP_CORNER = 99.dp
private val CHIP_PADDING_H = 12.dp
private val CHIP_PADDING_V = 5.dp
private val CHIP_GAP = 5.dp
private val CHIP_DOT = 7.dp
private val CHIP_VALUE_SIZE = 13.sp
private val CHIP_LABEL_SIZE = 9.sp
private const val CHIP_LABEL_ALPHA = 0.8f
private const val CHIP_FILL_ALPHA = 0.14f

/** 只讀時目標值顯示成一條破折號——沒有目標可讀，不是 0。 */
private const val DASH = "—"

private val STEPPER_CORNER = 99.dp
private val STEPPER_PADDING = 5.dp
private val STEPPER_GAP = 9.dp
private val STEPPER_VALUE_MIN = 64.dp
private val STEPPER_VALUE_SIZE = 17.sp
private val STEPPER_UNIT_SIZE = 9.sp
private const val STEPPER_UNIT_ALPHA = 0.8f
private const val STEPPER_FILL_ALPHA = 0.13f
private const val STEPPER_DISABLED_ALPHA = 0.35f

/** 視覺 34dp、命中 44dp。 */
private val STEP_KEY_SIZE = 34.dp
private val STEP_KEY_TOUCH = 44.dp
private val STEP_KEY_TEXT = 18.sp

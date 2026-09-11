package top.hasiyliquidglassdemo.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.hasiy.designsystem.GlassConfig
import top.hasiy.designsystem.accentToneColor
import top.hasiy.designsystem.mutedContentColor
import top.hasiy.designsystem.screenColor
import top.hasiy.designsystem.tactileKeycap
import top.hasiy.designsystem.tokens.GlassVisualStyle
import top.hasiyliquidglassdemo.R
import kotlin.math.roundToInt

/**
 * 橫屏 dock 上的步進鍵組——「不用點開就能調」的那組按鍵。
 *
 * 這裡**只有按鍵**，卡片的底、框、指示燈都不在這個檔案裡：它們由 [MetricCard]
 * 照原樣畫。可調節的格子本來就靠「淡綠底加一圈綠框加右上角那顆燈」說出自己
 * 能調（見 MetricCard 的 fill / borderColor / 指示燈），那套樣式豎屏橫屏共用。
 * 這組鍵只是把原本角上那對很小的「− ＋」**提示**換成真的按鍵——樣式照舊，
 * 只是從「看得出能調」變成「就地能調」。
 *
 * 自己再畫一套底和框的話，同一個阻力格在豎屏和橫屏會長成兩個樣子，那正是
 * MetricCard 裡那段註解一開始就要避免的事。
 */
/**
 * 一組步進鍵。
 *
 * @param range 量程，`step` 決定要兩顆還是四顆
 * @param onStep 步進回呼，參數是步進的個數
 * @param metricLabel 指標名，拼進無障礙標籤——光說「調低」讀屏使用者不知道對象是誰
 * @param unit 單位，同上
 * @param config 玻璃主題參數
 * @param density 卡片密度檔
 * @param locked 整組禁用
 */
@Composable
internal fun AdjustKeys(
    range: ControlRange,
    onStep: (Int) -> Unit,
    metricLabel: String,
    unit: String,
    config: GlassConfig,
    density: MetricCardDensity,
    locked: Boolean,
) {
    // 粗調 = 一個整數單位要走幾個步進。step 已經 >= 1 時算出來是 1，
    // 粗細同級，那就只出一對鍵。
    val coarse = if (range.step <= 0f) 1 else (1f / range.step).roundToInt().coerceAtLeast(1)
    val fineLabel = formatStepAmount(range.step, range.decimals)
    val keys = if (coarse <= 1) {
        listOf(-1 to MINUS, 1 to PLUS)
    } else {
        listOf(
            -coarse to MINUS + COARSE_LABEL,
            -1 to MINUS + fineLabel,
            1 to PLUS + fineLabel,
            coarse to PLUS + COARSE_LABEL,
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(density.pick(GAP, GAP_COMPACT, GAP_DENSE))) {
        keys.forEach { (steps, label) ->
            val amount = if (steps < 0) -steps else steps
            val amountLabel = if (amount == coarse && coarse > 1) COARSE_LABEL else fineLabel
            AdjustKey(
                label = label,
                description = stringResource(
                    if (steps < 0) R.string.inline_adjust_down_by else R.string.inline_adjust_up_by,
                    metricLabel,
                    amountLabel,
                    unit,
                ),
                config = config,
                density = density,
                disabled = locked,
                onClick = { onStep(steps) },
            )
        }
    }
}

/**
 * 一顆步進鍵。
 *
 * 做成膠囊而不是圓鈕：「−0.1」是四個字元，塞進圓鈕就得把字縮到讀不出來。
 * 寬度跟著內容走，高度統一。
 *
 * 命中區比視覺高度大一圈（[KEY_TOUCH]）——運動中手會抖、會出汗，按鍵不能
 * 只有看起來那麼大。dock 的格子只有 56..72dp 高，命中區塞不進 44dp 的話
 * 至少要保住 [KEY_TOUCH]。
 */
@Composable
private fun AdjustKey(
    label: String,
    description: String,
    config: GlassConfig,
    density: MetricCardDensity,
    disabled: Boolean,
    onClick: () -> Unit,
) {
    val tactile = config.visualStyle == GlassVisualStyle.TACTILE
    val height = density.pick(KEY_HEIGHT, KEY_HEIGHT_COMPACT, KEY_HEIGHT_DENSE)
    val shape = RoundedCornerShape(height / 2)
    val surface = if (tactile) {
        Modifier.tactileKeycap(height / 2)
    } else {
        Modifier.clip(shape).background(config.accentToneColor)
    }
    Box(
        modifier = Modifier
            .height(density.pick(KEY_TOUCH, KEY_TOUCH_COMPACT, KEY_TOUCH_DENSE))
            .then(
                if (disabled) Modifier
                else Modifier.tactileClickable(onClickLabel = description, onClick = onClick)
            )
            .alpha(if (disabled) DISABLED_ALPHA else 1f),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .height(height)
                .then(surface)
                .padding(horizontal = density.pick(KEY_PAD, KEY_PAD_COMPACT, KEY_PAD_DENSE)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                // 壓在實色鍵上的字用屏底色：淺色配色下內容色是近黑，壓在深底鍵上讀不出來
                color = if (tactile) config.mutedContentColor else config.screenColor,
                fontSize = density.pick(KEY_TEXT, KEY_TEXT_COMPACT, KEY_TEXT_DENSE),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

/**
 * 把步進量寫成標籤。
 *
 * 用量程自己的 `decimals`，不寫死「0.1」：換一台步進 0.5 的機器，寫死的標籤
 * 就會騙人——鍵上印 0.1、按下去跳 0.5。
 */
internal fun formatStepAmount(step: Float, decimals: Int): String =
    if (decimals <= 0) step.roundToInt().toString() else "%.${decimals}f".format(step)

/** 減號用 U+2212 而不是 ASCII 連字號：連字號在數字旁邊會被讀成分隔符。 */
private const val MINUS = "−"
private const val PLUS = "+"

/** 粗調的一格固定是一個整數單位。 */
private const val COARSE_LABEL = "1"

private val ADJUST_BORDER = 1.dp
private const val FILL_ALPHA = 0.08f
private const val BORDER_ALPHA = 0.55f
private const val DISABLED_ALPHA = 0.38f

private val PAD_H = 10.dp
private val PAD_H_COMPACT = 8.dp
private val PAD_H_DENSE = 6.dp

private val LABEL_SIZE = 11.sp
private val LABEL_SIZE_COMPACT = 10.sp
private val LABEL_SIZE_DENSE = 9.sp
private val VALUE_SIZE = 22.sp
private val VALUE_SIZE_COMPACT = 19.sp
private val VALUE_SIZE_DENSE = 16.sp
private val UNIT_SIZE = 11.sp
private val UNIT_SIZE_COMPACT = 10.sp
private val UNIT_SIZE_DENSE = 9.sp

private val GAP: Dp = 6.dp
private val GAP_COMPACT: Dp = 5.dp
private val GAP_DENSE: Dp = 4.dp

private val KEY_HEIGHT = 30.dp
private val KEY_HEIGHT_COMPACT = 27.dp
private val KEY_HEIGHT_DENSE = 24.dp
private val KEY_TOUCH = 44.dp
private val KEY_TOUCH_COMPACT = 38.dp
private val KEY_TOUCH_DENSE = 32.dp
private val KEY_PAD = 9.dp
private val KEY_PAD_COMPACT = 7.dp
private val KEY_PAD_DENSE = 5.dp
private val KEY_TEXT = 14.sp
private val KEY_TEXT_COMPACT = 13.sp
private val KEY_TEXT_DENSE = 11.sp

/**
 * 指標卡上的量程刻度——躺下來的那根液柱。
 *
 * 本身沒有任何繪製：直接用調節浮層那根管子（[ValueTube]），只是把 orientation
 * 換成橫的。豎的橫的是同一個元件，使用者在浮層裡看到的和在 dock 卡上看到的
 * 就是同一根管子，不用認兩次。
 *
 * 兩處只有三個參數不同：
 *
 * - `corner` 傳「高度的一半」做成膠囊。浮層那根是 36dp 圓角配 156dp 長度；
 *   這裡只有十來 dp 高，沿用 36 會把整條圓成一個橢圓
 * - `ticks` 按**一個整數單位**一條。浮層那根按固定間距（11dp）鋪，長度夠；
 *   這裡短，固定間距只畫得出兩三條。阻力 1..16 畫 15 條剛好；目標速度
 *   0.5..20 步進 0.1 有 195 段，每段一條會糊成實色，按整數單位是 19 條
 * - 關掉光暈：十來 dp 的管腔散不開那道光，只會把液面糊掉
 *
 * 讀數與單位由左邊那一欄負責，這裡不標數字：一格裡同一個值報兩次，眼睛得先
 * 確認兩邊一致才敢往下讀。
 *
 * @param range 量程
 * @param value 當前值
 * @param config 玻璃主題參數
 * @param density 卡片密度檔
 * @param modifier 外部修飾符
 */
@Composable
internal fun AdjustScale(
    range: ControlRange,
    value: Float,
    config: GlassConfig,
    density: MetricCardDensity,
    modifier: Modifier = Modifier,
) {
    val height = density.pick(SCALE_HEIGHT, SCALE_HEIGHT_COMPACT, SCALE_HEIGHT_DENSE)
    ValueTube(
        fraction = range.fractionOf(value),
        orientation = TubeOrientation.HORIZONTAL,
        config = config,
        modifier = modifier.height(height),
        corner = height / 2,
        // 上限兜一下：量程很大時（真機換成功率 0..1500）每單位一條就密到沒有意義
        ticks = (range.max - range.min).roundToInt().coerceIn(0, SCALE_TICK_MAX),
        showGlow = false,
    )
}

/**
 * 刻度條的高度。
 *
 * 比一般進度條粗（14dp 而不是 6..8dp）：管壁、內縮、刻痕與液面線四樣疊起來，
 * 太細就只剩一條綠色，看不出它是根管子。它擺在數值與按鍵之間、行內垂直居中，
 * 而那一行的高度由按鍵的命中區（32..44dp）決定，所以加粗不會把格子頂高。
 */
private val SCALE_HEIGHT = 14.dp
private val SCALE_HEIGHT_COMPACT = 12.dp
private val SCALE_HEIGHT_DENSE = 10.dp
private const val SCALE_TICK_MAX = 24

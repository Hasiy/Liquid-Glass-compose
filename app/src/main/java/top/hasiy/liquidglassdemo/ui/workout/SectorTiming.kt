package top.hasiyliquidglassdemo.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.hasiy.designsystem.GlassConfig
import top.hasiy.designsystem.LocalGlassConfig
import top.hasiy.designsystem.isLightSurface
import top.hasiy.designsystem.lineColor
import top.hasiy.designsystem.mutedContentColor
import top.hasiy.designsystem.panelColor
import top.hasiy.designsystem.screenContentColor
import top.hasiy.designsystem.tactilePanel
import top.hasiy.designsystem.tokens.GlassVisualStyle
import top.hasiyliquidglassdemo.R

/**
 * 一段的成績檔次。
 *
 * 沿用 F1 的顏色約定，但把對手換成了自己：F1 的紫是「全場最快」、綠是「個人最佳」，
 * 個人訓練沒有對手，所以這裡紫是「破歷史最快」、綠是「本次會話最快」。
 */
enum class SectorGrade {
    /** 比歷史最快還快。 */
    RECORD,

    /** 本次訓練裡最快的一次，但沒破歷史。 */
    BEST,

    /** 沒刷新任何最佳。 */
    PLAIN,
}

/**
 * 一段的分段計時。
 *
 * @param label 段名（S1 / S2 / S3）
 * @param time 本圈這一段的用時
 * @param fastest 這一段的歷史最快
 * @param average 這一段的平均
 * @param grade 成績檔次
 * @param current 是不是正在跑的那一段
 */
data class SectorSplit(
    val label: String,
    val time: String,
    val fastest: String,
    val average: String,
    val grade: SectorGrade,
    val current: Boolean = false,
)

/**
 * 分段計時那一排。
 *
 * 對應參考稿 09 賽道模式的 `.sector-row`。F1 把一圈切成三段，因為整圈時間只能告訴你
 * 「慢了」，分段才能告訴你「哪一段慢了」。
 *
 * 「跑到哪一段」與「這一段跑得好不好」是兩件事，所以前者用描邊、後者用文字顏色——
 * 兩套編碼正交，同一張卡上可以同時出現。
 *
 * @param splits 三段的成績
 * @param config 玻璃主題參數
 * @param modifier 外部修飾符
 */
@Composable
fun SectorRow(
    splits: List<SectorSplit>,
    config: GlassConfig = LocalGlassConfig.current,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SECTOR_GAP),
    ) {
        splits.forEach { split ->
            SectorCard(split = split, config = config, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun SectorCard(
    split: SectorSplit,
    config: GlassConfig,
    modifier: Modifier = Modifier,
) {
    val grade = sectorGradeColor(split.grade, config)
    // 描邊顏色：檔次優先，其次才是「正在跑的那一段」。
    //
    // 順序跟參考稿的 CSS 一致——`.is-record` / `.is-best` 寫在 `.is-current` 後面，
    // 後者被覆蓋。理由也對：破紀錄那一段值得標出來，而「跑到哪兒了」看位置點就知道。
    val border = when {
        split.grade != SectorGrade.PLAIN -> grade.copy(alpha = SECTOR_GRADE_ALPHA)
        split.current -> config.mutedContentColor.copy(alpha = SECTOR_CURRENT_ALPHA)
        else -> config.lineColor
    }
    val shape = RoundedCornerShape(SECTOR_CORNER)
    val surface = if (config.visualStyle == GlassVisualStyle.TACTILE) {
        Modifier.tactilePanel(SECTOR_CORNER)
    } else {
        Modifier.clip(shape).background(config.panelColor).border(1.dp, border, shape)
    }

    Column(
        modifier = modifier
            .then(surface)
            .padding(horizontal = SECTOR_PADDING_H, vertical = SECTOR_PADDING_V),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = split.label,
                // 段名有三種身分：普通、當前段、破紀錄。檔次優先——當前段的描邊已經
                // 說明了「在這裡」，段名這一處要留給成績。
                color = if (split.grade == SectorGrade.PLAIN) config.mutedContentColor else grade,
                fontSize = SECTOR_LABEL_SIZE,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.alignByBaseline(),
            )
            Text(
                text = split.time,
                color = if (split.grade == SectorGrade.PLAIN) config.screenContentColor else grade,
                fontSize = SECTOR_TIME_SIZE,
                modifier = Modifier.alignByBaseline(),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 1.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.sector_fastest, split.fastest),
                color = if (split.grade == SectorGrade.PLAIN) config.mutedContentColor else grade,
                fontSize = SECTOR_REF_SIZE,
            )
            Text(
                text = stringResource(R.string.sector_average, split.average),
                color = if (split.grade == SectorGrade.PLAIN) config.mutedContentColor else grade,
                fontSize = SECTOR_REF_SIZE,
            )
        }
    }
}

/**
 * 分段計時的圖例。
 *
 * 三檔顏色不給說明就只能靠猜。
 *
 * @param config 玻璃主題參數
 * @param modifier 外部修飾符
 */
@Composable
fun SectorLegend(
    config: GlassConfig = LocalGlassConfig.current,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(LEGEND_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendItem(stringResource(R.string.sector_legend_record), sectorGradeColor(SectorGrade.RECORD, config), config)
        LegendItem(stringResource(R.string.sector_legend_best), sectorGradeColor(SectorGrade.BEST, config), config)
        LegendItem(stringResource(R.string.sector_legend_plain), config.mutedContentColor, config)
        LegendItem(stringResource(R.string.sector_legend_now), config.mutedContentColor, config)
    }
}

@Composable
private fun LegendItem(label: String, color: Color, config: GlassConfig) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(LEGEND_DASH_W)
                .height(LEGEND_DASH_H)
                .clip(RoundedCornerShape(LEGEND_DASH_H / 2))
                .background(color),
        )
        Text(text = label, color = config.mutedContentColor, fontSize = SECTOR_REF_SIZE)
    }
}

/**
 * 檔次對應的顏色。
 *
 * 這兩個顏色**不進主題 token**：它們是 F1 的語義色（紫 = 破紀錄、綠 = 本次最佳），
 * 只在賽道模式出現，換主題色也不該跟著變——變了就不是那套約定了。
 *
 * 明暗各一組：深色屏用亮紫亮綠，淺色屏必須換成深紫深綠，否則壓在 #D1D1D1 的屏底上
 * 讀不出來（亮紫 #C08BFF 對淺屏只有 1.8:1）。
 */
private fun sectorGradeColor(grade: SectorGrade, config: GlassConfig): Color = when (grade) {
    SectorGrade.RECORD -> if (config.isLightSurface) F1_PURPLE_LIGHT else F1_PURPLE_DARK
    SectorGrade.BEST -> if (config.isLightSurface) F1_GREEN_LIGHT else F1_GREEN_DARK
    SectorGrade.PLAIN -> config.mutedContentColor
}

/** 深色屏用的 F1 紫與綠，照抄參考稿。 */
private val F1_PURPLE_DARK = Color(0xFFC08BFF)
private val F1_GREEN_DARK = Color(0xFF3DDC84)

/** 淺色屏用的深色版，照抄參考稿的 nordic 覆寫。 */
private val F1_PURPLE_LIGHT = Color(0xFF6D33AD)
private val F1_GREEN_LIGHT = Color(0xFF12703F)

private val SECTOR_GAP = 6.dp
private val SECTOR_CORNER = 12.dp
private val SECTOR_PADDING_H = 10.dp
private val SECTOR_PADDING_V = 6.dp
private val SECTOR_LABEL_SIZE = 9.sp
private val SECTOR_TIME_SIZE = 17.sp
private val SECTOR_REF_SIZE = 8.sp
private const val SECTOR_CURRENT_ALPHA = 0.55f
private const val SECTOR_GRADE_ALPHA = 0.5f

private val LEGEND_GAP = 14.dp
private val LEGEND_DASH_W = 12.dp
private val LEGEND_DASH_H = 3.dp

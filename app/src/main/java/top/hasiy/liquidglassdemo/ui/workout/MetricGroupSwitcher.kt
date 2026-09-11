package top.hasiyliquidglassdemo.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.hasiy.designsystem.GlassConfig
import top.hasiy.designsystem.LocalGlassConfig
import top.hasiy.designsystem.accentToneColor
import top.hasiy.designsystem.lineColor
import top.hasiy.designsystem.mutedContentColor
import top.hasiy.designsystem.panelColor
import top.hasiy.designsystem.quietAccentColor
import top.hasiy.designsystem.screenContentColor
import top.hasiy.designsystem.tactilePanel
import top.hasiy.designsystem.tokens.GlassVisualStyle
import top.hasiyliquidglassdemo.R

/** 參考稿只有主組與第 2 組。 */
private const val GROUP_COUNT = 2

/**
 * 換組按鈕與組別指示點。
 *
 * 對應參考稿的 `.p-bottom button` 與 `.group-nav`：一顆「更多指標」按鈕，
 * 右邊兩個點顯示現在在第幾組。
 *
 * @param groupIndex 當前組別
 * @param config 玻璃主題參數
 * @param onSwitch 點擊換組
 * @param modifier 外部修飾符
 * @param slotsPerGroup 這一族每組有幾個資料位。給了就顯示成參考稿豎屏那種
 *   「第 1 組 · 6 / 12」（本組格數 / 兩組合計）；給 0 退成橫屏那種只有組號的
 *   「1 / 2」。
 *
 *   參考稿豎屏寫的是「6 / 8」，但那串是**寫死在 HTML 裡的示意文字**，
 *   腳本不會更新它，8 也對不上任何一個能算出來的數（豎屏每組 6 格、共 2 組）。
 *   所以格式照抄、數字改成算得出來的。
 */
@Composable
fun MetricGroupSwitcher(
    groupIndex: Int,
    config: GlassConfig = LocalGlassConfig.current,
    onSwitch: () -> Unit,
    modifier: Modifier = Modifier,
    slotsPerGroup: Int = 0,
) {
    val shape = RoundedCornerShape(BAR_CORNER)
    val groupLabel = if (slotsPerGroup > 0) {
        stringResource(
            R.string.workout_group_indicator,
            groupIndex + 1,
            slotsPerGroup,
            slotsPerGroup * GROUP_COUNT,
        )
    } else {
        stringResource(R.string.workout_group_indicator_short, groupIndex + 1, GROUP_COUNT)
    }
    // 這條在 Tactile 下是嵌在面板裡的凹槽，不是浮起來的按鍵——參考稿的
    // .l-dock / .p-bottom button / .group-nav 共用同一組凹陷樣式
    val surface = if (config.visualStyle == GlassVisualStyle.TACTILE) {
        Modifier.tactilePanel(BAR_CORNER)
    } else {
        Modifier
            .clip(shape)
            // 這條是**提亮**一階的面板，不是壓暗。參考稿基礎主題 `#151919` 高於屏底
            // `#070909`，Nordic 直接覆寫成 `var(--panel)`——兩邊都是往上走一階。
            // 之前拿強調色鋪 10% 疊在屏底上，淺色配色下算出來比屏底還暗，
            // 整條沉下去了。
            .background(config.panelColor)
            .border(1.dp, config.lineColor, shape)
    }
    Row(
        // 參考稿的 .p-bottom button 是一條通欄：左邊標題、右邊組別，不是一顆小膠囊
        modifier = modifier
            .fillMaxWidth()
            .height(BAR_HEIGHT)
            .tactileClickable(onClick = onSwitch)
            .then(surface)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.workout_more_metrics),
            // 參考稿的標題不是主文字色而是弱一階（基礎主題 `#c0c7c3`、
            // Nordic `#444444`），字重則是 `font-weight: 750`
            color = config.mutedContentColor,
            fontSize = TITLE_SIZE,
            fontWeight = FontWeight.Bold,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = groupLabel,
                color = config.quietAccentColor,
                fontSize = META_SIZE,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = ARROW,
                color = config.quietAccentColor,
                fontSize = META_SIZE,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private const val ARROW = "→"
private val BAR_CORNER = 14.dp
private val BAR_HEIGHT = 43.dp
private val TITLE_SIZE = 12.sp
private val META_SIZE = 11.sp


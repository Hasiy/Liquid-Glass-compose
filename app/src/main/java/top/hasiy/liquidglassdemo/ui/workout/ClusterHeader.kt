package top.hasiyliquidglassdemo.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import top.hasiy.designsystem.dangerColor
import top.hasiy.designsystem.mutedContentColor
import top.hasiy.designsystem.screenContentColor
import top.hasiyliquidglassdemo.R

/**
 * 平板兩個顯示模式共用的頂欄。
 *
 * 對應參考稿的 `.l-top`：左邊一行身分（`18:42 · 室内单车 · 已连接`）加一顆記錄徽章，
 * 右邊是操作。
 *
 * 為什麼不用 [WorkoutHeader] 的緊湊模式：那個把時長排在**右邊**，設備名在左，
 * 中間留一大段空白——它是為手機橫屏設計的，那裡右側只有兩顆按鈕。這兩頁右側擠了
 * 模式切換、版面編輯、主題色、暫停、滑動結束五組，時長再放右邊就跟它們搶位置，
 * 而且參考稿本來就是「時長開頭的一句話」。
 *
 * 返回鍵是 App 這一側要的，參考稿沒有——它畫的是全屏運動中的樣子。
 *
 * @param state 當前狀態
 * @param onBack 返回上一頁
 * @param config 玻璃主題參數
 * @param modifier 外部修飾符
 * @param actions 右側的操作
 */
@Composable
fun ClusterHeader(
    state: WorkoutUiState,
    onBack: () -> Unit,
    config: GlassConfig = LocalGlassConfig.current,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HEADER_GAP),
    ) {
        Box(
            modifier = Modifier
                .size(BACK_SIZE)
                .tactileClickable(
                    onClickLabel = stringResource(R.string.matrix_back_desc),
                    onClick = onBack,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = BACK_GLYPH, color = config.screenContentColor, fontSize = 22.sp)
        }

        // 一句話裡兩個層級：時長與設備名是背景資訊，連線狀態才是要確認的那一項。
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(
                    R.string.cluster_identity,
                    state.durationText,
                    state.device.name,
                ),
                color = config.mutedContentColor,
                fontSize = IDENTITY_SIZE,
            )
            Text(
                text = stringResource(state.runtime.labelRes),
                color = when {
                    state.runtime.isOffline -> config.dangerColor
                    else -> config.screenContentColor
                },
                fontSize = STATUS_SIZE,
                fontWeight = FontWeight.SemiBold,
            )
        }

        RecordingBadge(state = state, config = config)

        Spacer(Modifier.weight(1f))
        actions()
    }
}

/**
 * 記錄狀態徽章。
 *
 * 參考稿的 `.l-top .live`：強調色 12% 的膠囊。凍結時轉中性——計時停了還說
 * 「記錄中」是騙人的。
 */
@Composable
private fun RecordingBadge(state: WorkoutUiState, config: GlassConfig) {
    val live = state.runtime.isLive
    val tint = if (live) config.accentToneColor else config.mutedContentColor
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(BADGE_CORNER))
            .background(tint.copy(alpha = BADGE_FILL_ALPHA))
            .padding(horizontal = BADGE_PADDING_H, vertical = BADGE_PADDING_V),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(
                if (live) R.string.card_state_recording else R.string.workout_frozen
            ),
            color = tint,
            fontSize = BADGE_SIZE,
        )
    }
}

private val HEADER_GAP = 8.dp
private val BACK_SIZE = 40.dp
private val IDENTITY_SIZE = 9.sp
private val STATUS_SIZE = 11.sp
private val BADGE_CORNER = 99.dp
private val BADGE_PADDING_H = 8.dp
private val BADGE_PADDING_V = 3.dp
private val BADGE_SIZE = 8.sp
private const val BADGE_FILL_ALPHA = 0.12f
private const val BACK_GLYPH = "‹"

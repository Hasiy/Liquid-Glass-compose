package top.hasiyliquidglassdemo.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
 * 頂欄：裝置名與連線狀態，右側是會話讀數。
 *
 * 對應參考稿的 `.p-top` / `.p-title` / `.p-session`。橫屏的 `.l-top` / `.l-meta`
 * 是同一組資訊換一個排法，用 [compact] 切換。
 *
 * @param state 當前狀態
 * @param config 玻璃主題參數
 * @param modifier 外部修飾符
 * @param trailing 右側附加內容，通常是 `SessionActions`
 * @param compact 橫屏用的緊湊排法
 */
@Composable
fun WorkoutHeader(
    state: WorkoutUiState,
    config: GlassConfig = LocalGlassConfig.current,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    trailing: @Composable () -> Unit = {},
) {
    if (compact) {
        // 橫屏高度緊，全部擠一行
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DeviceTitle(state, config, compact = true)
            Spacer(Modifier.weight(1f))
            SessionReadout(state, config)
            trailing()
        }
        return
    }

    // 豎屏分兩行，與參考稿一致：上排是設備與操作，下排是時長與記錄狀態。
    // 擠成一行的話設備名會直接頂到時長上（「室内单车18:42」）。
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DeviceTitle(state, config, compact = false)
            Spacer(Modifier.weight(1f))
            trailing()
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.metric_duration),
                    color = config.mutedContentColor,
                    fontSize = 11.sp,
                )
                Text(
                    text = state.durationText,
                    color = config.screenContentColor,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Light,
                )
            }
            RecordingPill(state, config)
        }
    }
}

/** 設備名加一顆連線狀態膠囊。 */
@Composable
private fun DeviceTitle(state: WorkoutUiState, config: GlassConfig, compact: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = state.device.name,
            color = config.screenContentColor,
            fontSize = if (compact) 13.sp else 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        ConnectionLine(state, config)
    }
}

/**
 * 連線狀態：一顆色點加一行字，**沒有底**。
 *
 * 參考稿的 `.p-title small` 就是設備名底下的一行小字（`● 已连接 · FTMS Bike`），
 * 不是膠囊。做成膠囊會和右邊的操作按鈕搶視覺重量。
 */
@Composable
private fun ConnectionLine(state: WorkoutUiState, config: GlassConfig) {
    val runtime = state.runtime
    val tint = when {
        runtime.isOffline -> config.dangerColor
        runtime == WorkoutRuntimeState.ACTIVE -> config.accentToneColor
        else -> config.mutedContentColor
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(5.dp).clip(CircleShape).background(tint))
        Text(
            text = "${stringResource(runtime.labelRes)} · ${state.device.protocol}",
            color = tint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** 橫屏的會話讀數：時長加一行記錄狀態。 */
@Composable
private fun SessionReadout(state: WorkoutUiState, config: GlassConfig) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = state.durationText,
            color = config.screenContentColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            // 只有還在跑的時候才叫「記錄中」；凍結的計時說成記錄中會誤導
            text = stringResource(
                if (state.runtime.isLive) R.string.workout_recording else R.string.workout_frozen
            ),
            color = if (state.runtime.isLive) config.accentToneColor else config.mutedContentColor,
            fontSize = 10.sp,
        )
    }
}

/** 記錄狀態膠囊，豎屏用。 */
@Composable
private fun RecordingPill(state: WorkoutUiState, config: GlassConfig) {
    val live = state.runtime.isLive
    val tint = if (live) config.accentToneColor else config.mutedContentColor
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(PILL_CORNER))
            .background(tint.copy(alpha = PILL_FILL_ALPHA))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(tint))
        Text(
            text = stringResource(
                if (live) R.string.workout_recording else R.string.workout_frozen
            ),
            color = tint,
            fontSize = 11.sp,
        )
    }
}

/** 運動時長的當前值。 */
internal val WorkoutUiState.durationText: String
    get() = MetricCatalog.find("duration")?.let { valueOf(it) }.orEmpty()

/** 執行狀態的顯示文案。 */
internal val WorkoutRuntimeState.labelRes: Int
    get() = when (this) {
        WorkoutRuntimeState.ACTIVE -> R.string.workout_status_active
        WorkoutRuntimeState.PAUSED -> R.string.workout_status_paused
        WorkoutRuntimeState.DISCONNECTED -> R.string.workout_status_disconnected
        WorkoutRuntimeState.RECONNECTING -> R.string.workout_status_reconnecting
        WorkoutRuntimeState.ENDED -> R.string.workout_status_ended
    }

private val PILL_CORNER = 999.dp
private const val PILL_FILL_ALPHA = 0.14f

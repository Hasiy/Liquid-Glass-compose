package top.hasiyliquidglassdemo.ui.workout

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.withTimeoutOrNull
import top.hasiy.designsystem.GlassConfig
import top.hasiy.designsystem.LocalGlassConfig
import top.hasiy.designsystem.accentToneColor
import top.hasiy.designsystem.dangerColor
import top.hasiy.designsystem.lineColor
import top.hasiy.designsystem.mutedContentColor
import top.hasiy.designsystem.onAccentColor
import top.hasiy.designsystem.screenContentColor
import top.hasiy.designsystem.tactileKeycap
import top.hasiy.designsystem.tokens.GlassVisualStyle
import top.hasiy.designsystem.trackColor
import top.hasiyliquidglassdemo.R

/**
 * 會話操作：暫停 / 繼續，以及滑動結束。
 *
 * 對應參考稿的 `.p-top-actions .pause` 與 `.l-actions`。結束沒有做成按鈕——
 * 結束不可逆，一顆按鈕在騎行途中太容易誤觸，所以照參考稿用滑動確認。
 *
 * @param state 當前狀態
 * @param config 玻璃主題參數
 * @param controller 狀態持有者
 * @param modifier 外部修飾符
 * @param showSlideToEnd 是否顯示滑動結束；已結束時沒有意義
 */
@Composable
fun SessionActions(
    state: WorkoutUiState,
    config: GlassConfig = LocalGlassConfig.current,
    controller: WorkoutController,
    modifier: Modifier = Modifier,
    showSlideToEnd: Boolean = true,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PauseButton(state = state, config = config, controller = controller)
        if (showSlideToEnd && state.runtime != WorkoutRuntimeState.ENDED) {
            SlideToEndControl(
                progress = state.slideEndProgress,
                config = config,
                onDrag = controller::updateSlideEnd,
                onRelease = controller::releaseSlideEnd,
                modifier = Modifier.width(SLIDE_WIDTH),
            )
        }
    }
}

/**
 * 暫停 / 繼續。
 *
 * 參考稿的 `.pause` 是**強調色實心**（`color: var(--on-accent); background: var(--lime)`），
 * 48×32、圓角 10。Tactile 覆寫成深色膠帽，Digital 覆寫成近黑實心。
 *
 * 斷線與已結束時按不動——那兩種狀態下暫停沒有意義。
 */
@Composable
private fun PauseButton(
    state: WorkoutUiState,
    config: GlassConfig = LocalGlassConfig.current,
    controller: WorkoutController,
) {
    val paused = state.runtime == WorkoutRuntimeState.PAUSED
    val enabled = state.runtime == WorkoutRuntimeState.ACTIVE || paused
    val tactile = config.visualStyle == GlassVisualStyle.TACTILE
    val shape = RoundedCornerShape(PAUSE_CORNER)

    val surface = when {
        tactile -> Modifier.tactileKeycap(PAUSE_CORNER)
        else -> Modifier.clip(shape).background(config.accentToneColor)
    }
    val label = if (tactile) config.screenContentColor else config.onAccentColor

    Row(
        modifier = Modifier
            .height(ACTION_HEIGHT)
            .tactileClickable(enabled = enabled) { if (paused) controller.resume() else controller.pause() }
            .then(surface)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (paused) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = label,
                modifier = Modifier.size(13.dp),
            )
        } else {
            PauseGlyph(label)
        }
        Text(
            text = stringResource(if (paused) R.string.action_resume else R.string.action_pause),
            color = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** 兩根豎條。Material 的 Filled 圖示集裡沒有 Pause，畫比引一整包省事。 */
@Composable
private fun PauseGlyph(tint: Color) {
    Row(
        modifier = Modifier.size(width = 10.dp, height = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        repeat(2) {
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(1.dp))
                    .background(tint)
            )
        }
    }
}

/**
 * 滑動結束：先長按解鎖，再把滑塊拖到行程的 [SLIDE_END_THRESHOLD]。
 *
 * 結束不可逆，而且是在運動途中——手一滑就結束了不能接受。所以做成兩道關卡：
 * 先按住 [ARM_MILLIS] 毫秒解鎖（軌道上會有一條進度條走完），解鎖後**不鬆手**
 * 直接往右拖，拖過門檻才算。中途鬆手一切歸零。
 *
 * 進度不存在這裡而是回寫給 [WorkoutController]，因為「有沒有達標」是狀態的一
 * 部分——鬆手時要靠它決定結束還是彈回。
 *
 * @param progress 當前拖動進度 0..1
 * @param config 玻璃主題參數
 * @param onDrag 拖動回呼，傳入新的進度
 * @param onRelease 鬆手回呼
 * @param modifier 外部修飾符
 */
@Composable
fun SlideToEndControl(
    progress: Float,
    config: GlassConfig = LocalGlassConfig.current,
    onDrag: (Float) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
    wide: Boolean = false,
) {
    // 加寬檔對應參考稿的 `.slide-end.wide`：暫停頁那條軌道更長更高，旋鈕也大一號。
    // 那一屏只有兩個控件，尺寸跟著放大才和旁邊 46dp 的「開始」對齊。
    val height = if (wide) WIDE_HEIGHT else ACTION_HEIGHT
    val knob = if (wide) WIDE_KNOB_SIZE else KNOB_SIZE
    val shape = RoundedCornerShape(if (wide) WIDE_CORNER else SLIDE_CORNER)
    val reached = progress >= SLIDE_END_THRESHOLD
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    // pointerInput 的 lambda 只在 key 變動時重建，直接讀 progress 會一路用著手勢
    // 開始那一刻的舊值，拖起來會跳
    val latestProgress by rememberUpdatedState(progress)
    val danger = config.dangerColor
    val endLabel = stringResource(R.string.action_end_session_a11y)

    var holding by remember { mutableStateOf(false) }
    var armed by remember { mutableStateOf(false) }
    // 解鎖進度只是給人看的，跟著按住的時間走
    val armProgress by animateFloatAsState(
        targetValue = if (holding) 1f else 0f,
        animationSpec = if (holding) {
            tween(ARM_MILLIS.toInt(), easing = LinearEasing)
        } else {
            snap()
        },
        label = "slideArm",
    )

    BoxWithConstraints(
        // 結束是危險動作，整個控件走危險色——參考稿的 .slide-end 是
        // `rgba(255,105,105,.1)` 的底加 `.22` 的描邊，不是中性軌道
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(danger.copy(alpha = TRACK_FILL_ALPHA))
            .border(1.dp, danger.copy(alpha = TRACK_BORDER_ALPHA), shape)
            // 結束是不可逆的，而它唯一的入口是「按住五秒解鎖、再拖到底」。
            // 這種純手勢對 TalkBack 與開關控制的使用者等於沒有入口——他們既點不到
            // 也拖不動，只能放棄。所以另外掛一個等效動作。
            //
            // 它走的是**同一條狀態機**（拖到底再放手），不是繞過確認：releaseSlideEnd
            // 仍然自己判斷有沒有過閾值。無障礙服務本身就是刻意觸發，不需要防手滑。
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(endLabel) {
                        onDrag(1f)
                        onRelease()
                        true
                    }
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val trackPx = with(density) { (maxWidth - knob - SLIDE_PADDING * 2) .toPx() }
            .coerceAtLeast(1f)

        // 長按解鎖的進度條：沒有它，按住五秒的期間畫面毫無反應
        if (armProgress > 0f && !armed) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(armProgress)
                    .background(danger.copy(alpha = ARM_FILL_ALPHA))
            )
        }

        Text(
            text = stringResource(
                when {
                    reached -> R.string.action_slide_release
                    armed -> R.string.action_slide_to_end
                    else -> R.string.action_hold_to_end
                }
            ),
            color = danger,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.Center)
                // 兩端都讓開旋鈕的寬度：只讓開起點的話，旋鈕滑到右邊就把字蓋掉一半
                .padding(horizontal = knob),
        )

        Box(
            modifier = Modifier
                .padding(SLIDE_PADDING)
                .offset { IntOffset((progress * trackPx).roundToInt(), 0) }
                .size(knob)
                // 參考稿的旋鈕是圓角方塊（radius 9 / 25px），不是圓
                .clip(RoundedCornerShape(if (wide) WIDE_KNOB_CORNER else KNOB_CORNER))
                .background(danger)
                .pointerInput(trackPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        holding = true
                        armed = false
                        try {
                            // 按住不放到逾時＝解鎖。中途抬手的話這段會提早回傳。
                            val liftedEarly = withTimeoutOrNull(ARM_MILLIS) {
                                while (true) {
                                    val change = awaitPointerEvent().changes
                                        .firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull true
                                    if (!change.pressed) return@withTimeoutOrNull true
                                    change.consume()
                                }
                                @Suppress("UNREACHABLE_CODE")
                                true
                            }
                            if (liftedEarly != null) return@awaitEachGesture

                            armed = true
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)

                            // 解鎖後直接接著拖，不用抬手再按一次
                            while (true) {
                                val change = awaitPointerEvent().changes
                                    .firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) break
                                val dx = change.positionChange().x
                                if (dx != 0f) {
                                    onDrag(latestProgress + dx / trackPx)
                                    change.consume()
                                }
                            }
                        } finally {
                            holding = false
                            armed = false
                            onRelease()
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                // 箭頭只是裝飾：目前在哪一階（長按解鎖／滑動結束／鬆手結束）由旁邊那個
                // Text 講，操作入口由外層的 customAction 提供。這裡再給一次固定文字
                // 反而會讓讀屏念出一句跟當前狀態不符的「長按解鎖」。
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

/** 參考稿 .p-top-actions button 高 32、.slide-end 寬 80、旋鈕 25，這裡按手機比例放大 1.2 倍。 */
private val ACTION_HEIGHT = 38.dp
/** 兩端各讓出一個旋鈕寬，中間還要放得下四個字。 */
private val SLIDE_WIDTH = 132.dp
private val SLIDE_PADDING = 4.dp
private val KNOB_SIZE = 30.dp

/** 參考稿 `.slide-end.wide`：46 高、旋鈕 36、圓角 15 / 11。 */
private val WIDE_HEIGHT = 46.dp
private val WIDE_KNOB_SIZE = 36.dp
private val WIDE_CORNER = 15.dp
private val WIDE_KNOB_CORNER = 11.dp
private val KNOB_CORNER = 11.dp
private val SLIDE_CORNER = 14.dp
private val PAUSE_CORNER = 12.dp

private const val TRACK_FILL_ALPHA = 0.10f
private const val TRACK_BORDER_ALPHA = 0.22f
private const val DISABLED_ALPHA = 0.45f

/** 解鎖要按住多久。結束不可逆，寧可慢一點也不要誤觸。 */
private const val ARM_MILLIS = 5_000L
private const val ARM_FILL_ALPHA = 0.18f

package top.hasiyliquidglassdemo.ui.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.hasiy.designsystem.GlassConfig
import top.hasiy.designsystem.LocalGlassConfig
import top.hasiy.designsystem.accentToneColor
import top.hasiy.designsystem.dangerColor
import top.hasiy.designsystem.isLightSurface
import top.hasiy.designsystem.lineColor
import top.hasiy.designsystem.mutedContentColor
import top.hasiy.designsystem.onAccentColor
import top.hasiy.designsystem.panelColor
import top.hasiy.designsystem.screenColor
import top.hasiy.designsystem.screenContentColor
import top.hasiyliquidglassdemo.R

/**
 * 執行狀態浮層：暫停、斷線 / 重連、已結束。
 *
 * 對應參考稿的 `.pause-layer` 與 `.scenario-screen.disconnected`。
 *
 * 暫停態與其餘幾種**不是同一個形態**：
 * - 暫停是參考稿的 `.pause-layer`——鋪滿整屏、**沒有卡片**，內容直接落在遮罩上，
 *   下面一排「開始」與「滑動結束」。暫停是使用者自己按的，這一屏就是他要操作的地方，
 *   套一張卡反而把「整場運動停住了」說小了
 * - 斷線、重連、已結束是**通知**：一張居中的小卡說明發生了什麼，不需要占滿整屏
 *
 * 遮罩會吃掉點擊：這幾種狀態下底層的指標卡本來就不該能點。
 *
 * @param state 當前狀態
 * @param config 玻璃主題參數
 * @param controller 狀態持有者
 * @param modifier 外部修飾符
 */
@Composable
fun RuntimeStateOverlay(
    state: WorkoutUiState,
    config: GlassConfig = LocalGlassConfig.current,
    controller: WorkoutController,
    modifier: Modifier = Modifier,
) {
    val runtime = state.runtime
    AnimatedVisibility(
        visible = runtime != WorkoutRuntimeState.ACTIVE,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        // 退場動畫期間 runtime 已經是 ACTIVE 了，內容要記住最後一次非 ACTIVE 的
        // 狀態，否則收起的瞬間會閃一下空白
        val shown = rememberLastInactive(runtime)
        val paused = shown == WorkoutRuntimeState.PAUSED
        // 暫停那一層比別的透一點：底下的讀數要還看得見「停在哪個數」。
        // 參考稿深色屏 .64、淺色屏 .72——淺色底的遮罩要更厚才壓得住底下的內容。
        val scrim = when {
            !paused -> SCRIM_ALPHA
            config.isLightSurface -> PAUSE_SCRIM_ALPHA_LIGHT
            else -> PAUSE_SCRIM_ALPHA_DARK
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(config.screenColor.copy(alpha = scrim))
                // 遮罩吃掉點擊：底下的指標卡在這些狀態下本來就不該能點。
                // 用空的 interactionSource 是為了不畫出水波紋。
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {}
                // 這一層只是用來吃掉點擊，本身沒有任何內容。不清掉語意的話，讀屏
                // 會看到一個蓋滿整個畫面、可聚焦、可點、但講不出名字的節點，
                // 反而比沒有它更難導覽。
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            // 參考稿還給遮罩掛了 `backdrop-filter: grayscale(1) blur(2px)`——
            // 底下的畫面在暫停時褪成灰。Compose 要 RenderEffect 才做得到，
            // 那是 API 31 起才有的，而遮罩本身已經把「停住了」講清楚，所以沒做。
            if (paused) {
                PauseLayer(state = state, config = config, controller = controller)
            } else {
                OverlayCard(runtime = shown, config = config, controller = controller)
            }
        }
    }
}

/** 記住最後一次非執行中的狀態，供退場動畫期間繼續顯示。 */
@Composable
private fun rememberLastInactive(runtime: WorkoutRuntimeState): WorkoutRuntimeState {
    val holder = remember { mutableStateOf(WorkoutRuntimeState.PAUSED) }
    if (runtime != WorkoutRuntimeState.ACTIVE) holder.value = runtime
    return holder.value
}

/**
 * 暫停態鋪滿整屏的那一層。
 *
 * 對應參考稿的 `.pause-layer`：一句說明、凍結住的時長、一句注解，底下並排
 * 「開始」與「滑動結束」。**沒有卡片**——見 [RuntimeStateOverlay] 的說明。
 *
 * 結束走的是滑動而不是按鈕：暫停頁最容易誤觸的就是結束，一按就沒了整場運動。
 * 參考稿在這裡用的也是滑動確認，跟頂欄同一個控件、同一個進度（[WorkoutUiState.slideEndProgress]），
 * 所以在頂欄滑到一半再暫停，這裡接著滑得下去。
 */
@Composable
private fun PauseLayer(
    state: WorkoutUiState,
    config: GlassConfig,
    controller: WorkoutController,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 20.dp),
    ) {
        Text(
            text = stringResource(R.string.overlay_paused_lead),
            color = config.mutedContentColor,
            fontSize = LEAD_SIZE,
        )
        Text(
            text = state.durationText,
            color = config.screenContentColor,
            fontSize = DURATION_SIZE,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 5.dp, bottom = 2.dp),
        )
        Text(
            text = stringResource(R.string.overlay_paused_note),
            color = config.mutedContentColor,
            fontSize = NOTE_SIZE,
            modifier = Modifier.padding(bottom = 13.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.overlay_paused_resume),
                color = config.onAccentColor,
                fontSize = RESUME_TEXT_SIZE,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .size(width = RESUME_WIDTH, height = ACTION_HEIGHT)
                    .clip(RoundedCornerShape(ACTION_CORNER))
                    .background(config.accentToneColor)
                    .tactileClickable(
                        onClickLabel = stringResource(R.string.overlay_paused_resume),
                        onClick = controller::resume,
                    )
                    .wrapContentHeight(),
            )
            SlideToEndControl(
                progress = state.slideEndProgress,
                config = config,
                onDrag = controller::updateSlideEnd,
                onRelease = controller::releaseSlideEnd,
                // 參考稿的 `.slide-end.wide`：暫停頁的滑軌比頂欄那條長，
                // 手指要走完整一段才算確認
                modifier = Modifier.width(SLIDE_WIDE_WIDTH),
                wide = true,
            )
        }
    }
}

@Composable
private fun OverlayCard(
    runtime: WorkoutRuntimeState,
    config: GlassConfig = LocalGlassConfig.current,
    controller: WorkoutController,
) {
    val tint = if (runtime.isOffline) config.dangerColor else config.accentToneColor
    val shape = RoundedCornerShape(CARD_CORNER)

    Column(
        modifier = Modifier
            // 斷線、重連、已結束這幾個狀態是文字直接重繪的，不牽動焦點。
            // 連線中途斷掉時要主動念出來，否則使用者得自己重新掃一遍畫面才發現。
            .semantics { liveRegion = LiveRegionMode.Polite }
            .clip(shape)
            .background(config.panelColor)
            .border(1.dp, config.lineColor, shape)
            .padding(horizontal = 22.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(runtime = runtime, tint = tint)
            Text(
                text = stringResource(runtime.overlayTitleRes),
                color = config.screenContentColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Text(
            text = stringResource(runtime.overlayNoteRes),
            color = config.mutedContentColor,
            fontSize = 12.sp,
        )

        // 斷線與重連沒有按鈕：能不能連上不是使用者按一下就能決定的。
        // 場景切換條那邊才是這個 demo 觸發重連的入口。
        val action = runtime.primaryAction
        if (action != null) {
            Text(
                text = stringResource(action),
                color = config.onAccentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(config.accentToneColor)
                    .clickable {
                        when (runtime) {
                            WorkoutRuntimeState.PAUSED -> controller.resume()
                            WorkoutRuntimeState.ENDED -> controller.activate()
                            else -> Unit
                        }
                    }
                    .padding(horizontal = 20.dp, vertical = 9.dp),
            )
        }
    }
}

/** 重連時讓點子閃爍，其餘狀態是一顆靜止的點。 */
@Composable
private fun StatusDot(runtime: WorkoutRuntimeState, tint: Color) {
    val pulsing = runtime == WorkoutRuntimeState.RECONNECTING
    val transition = rememberInfiniteTransition(label = "statusDot")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (pulsing) DOT_MIN_ALPHA else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(DOT_PERIOD_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "statusDotAlpha",
    )
    Box(Modifier.size(8.dp).alpha(alpha).clip(CircleShape).background(tint))
}

private val WorkoutRuntimeState.overlayTitleRes: Int
    get() = when (this) {
        WorkoutRuntimeState.PAUSED -> R.string.overlay_paused_title
        WorkoutRuntimeState.DISCONNECTED -> R.string.overlay_disconnected_title
        WorkoutRuntimeState.RECONNECTING -> R.string.overlay_reconnecting_title
        WorkoutRuntimeState.ENDED -> R.string.overlay_ended_title
        WorkoutRuntimeState.ACTIVE -> R.string.overlay_paused_title
    }

private val WorkoutRuntimeState.overlayNoteRes: Int
    get() = when (this) {
        WorkoutRuntimeState.PAUSED -> R.string.overlay_paused_note
        WorkoutRuntimeState.DISCONNECTED, WorkoutRuntimeState.RECONNECTING ->
            R.string.overlay_offline_note
        WorkoutRuntimeState.ENDED -> R.string.overlay_ended_note
        WorkoutRuntimeState.ACTIVE -> R.string.overlay_paused_note
    }

/** 浮層上那顆按鈕；離線狀態沒有可按的動作。 */
private val WorkoutRuntimeState.primaryAction: Int?
    get() = when (this) {
        WorkoutRuntimeState.PAUSED -> R.string.action_resume
        WorkoutRuntimeState.ENDED -> R.string.overlay_restart
        else -> null
    }

private val CARD_CORNER = 20.dp
private const val SCRIM_ALPHA = 0.82f

/** 參考稿 `.pause-layer` 的遮罩濃度，深色屏 .64、淺色屏 .72。 */
private const val PAUSE_SCRIM_ALPHA_DARK = 0.64f
private const val PAUSE_SCRIM_ALPHA_LIGHT = 0.72f

/** 暫停層的三行文案與底下那一排。參考稿 8 / 24 / 7 px 與 88×46、圓角 15。 */
private val LEAD_SIZE = 12.sp
private val DURATION_SIZE = 32.sp
private val NOTE_SIZE = 11.sp
private val RESUME_TEXT_SIZE = 13.sp
private val RESUME_WIDTH = 96.dp
private val ACTION_HEIGHT = 46.dp
private val ACTION_CORNER = 15.dp
private val SLIDE_WIDE_WIDTH = 190.dp
private const val DOT_MIN_ALPHA = 0.25f
private const val DOT_PERIOD_MS = 700

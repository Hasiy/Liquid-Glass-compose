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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.hasiy.designsystem.GlassConfig
import top.hasiy.designsystem.LocalGlassConfig
import top.hasiy.designsystem.accentToneColor
import top.hasiy.designsystem.dangerColor
import top.hasiy.designsystem.inkColor
import top.hasiy.designsystem.lineColor
import top.hasiy.designsystem.mutedContentColor
import top.hasiy.designsystem.panelColor
import top.hasiy.designsystem.screenColor
import top.hasiy.designsystem.screenContentColor
import top.hasiy.designsystem.tactileKeycap
import top.hasiy.designsystem.tokens.GlassVisualStyle
import top.hasiyliquidglassdemo.R

/**
 * 控制寫入結果的 Toast。
 *
 * 對應參考稿的 `.control-toast` / `.feedback-toast`：三種結果的文案、顏色與
 * 數值處理都不同——寫入中樂觀顯示目標值，失敗與拒絕都已在
 * [WorkoutController.writeValue] 回彈，這裡只負責把「回彈了」講清楚。
 *
 * @param state 當前寫入狀態；[ControlWriteState.Idle] 時不顯示
 * @param config 玻璃主題參數
 * @param modifier 外部修飾符
 */
@Composable
fun ControlWriteToast(
    state: ControlWriteState,
    config: GlassConfig = LocalGlassConfig.current,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = state.isVisible,
        // 從下方滑入：Toast 壓在浮層與指標格之上，從下面來才不會蓋住讀數
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier,
    ) {
        // AnimatedVisibility 退場時 state 已經變回 Idle，這裡要記住最後一次
        // 非 Idle 的內容，否則收起的那一瞬間文字會空掉
        val shown = rememberLastVisible(state)
        if (shown != null) ToastBody(shown, config)
    }
}

/** 記住最後一次可見的寫入狀態，供退場動畫期間繼續顯示。 */
@Composable
private fun rememberLastVisible(state: ControlWriteState): ControlWriteState? {
    val holder = remember { mutableStateOf<ControlWriteState?>(null) }
    if (state.isVisible) holder.value = state
    return holder.value
}

@Composable
private fun ToastBody(state: ControlWriteState, config: GlassConfig) {
    val tint = when (state) {
        is ControlWriteState.Writing -> config.accentToneColor
        is ControlWriteState.Failed, is ControlWriteState.Rejected -> config.dangerColor
        ControlWriteState.Idle -> config.mutedContentColor
    }
    val shape = RoundedCornerShape(TOAST_CORNER)
    // Toast 也要跟著風格換材質，不只換顏色：參考稿的
    // `.theme-tactile .control-toast` 是深色漸層加外影，`.theme-digital` 是近黑實心。
    val surface = when (config.visualStyle) {
        GlassVisualStyle.TACTILE -> Modifier.tactileKeycap(TOAST_CORNER)
        GlassVisualStyle.DIGITAL -> Modifier
            .clip(shape)
            .background(config.inkColor)
        else -> Modifier
            .clip(shape)
            .background(config.panelColor)
            .border(1.dp, config.lineColor, shape)
    }

    Row(
        modifier = Modifier
            // 這些提示自己出現又自己消失，不牽動焦點——沒有 liveRegion 的話讀屏
            // 使用者永遠不會知道剛才那次寫入失敗了。「已拒絕，已還原為 X」這種
            // 回彈訊息尤其需要當場講出來。
            .semantics { liveRegion = LiveRegionMode.Polite }
            .then(surface)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state is ControlWriteState.Writing) {
            WritingSpinner(tint)
        } else {
            // 失敗與拒絕不轉圈——結果已經定了，轉圈會讓人以為還在試
            Box(Modifier.size(DOT_SIZE).clip(RoundedCornerShape(50)).background(tint))
        }
        Column {
            Text(
                text = toastTitle(state),
                color = if (config.visualStyle == GlassVisualStyle.DIGITAL) {
                    config.screenColor
                } else {
                    config.screenContentColor
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            val note = toastNote(state)
            if (note != null) {
                Text(text = note, color = tint, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun toastTitle(state: ControlWriteState): String = when (state) {
    // 參考稿是「正在设定阻力 8 级…」——同時在調兩個指標時，不說是哪一個就看不懂
    is ControlWriteState.Writing -> stringResource(
        R.string.toast_writing,
        MetricCatalog.find(state.metricId)?.labelRes?.let { stringResource(it) }.orEmpty(),
        state.target + unitOf(state.metricId),
    )
    is ControlWriteState.Failed -> stringResource(R.string.toast_failed)
    is ControlWriteState.Rejected -> stringResource(R.string.toast_rejected, state.restored)
    ControlWriteState.Idle -> ""
}

@Composable
private fun toastNote(state: ControlWriteState): String? = when (state) {
    is ControlWriteState.Failed -> stringResource(R.string.toast_failed_note)
    is ControlWriteState.Rejected -> stringResource(R.string.toast_rejected_note)
    else -> null
}

/** 指標的單位；沒有單位時回空字串。 */
@Composable
private fun unitOf(metricId: String): String =
    MetricCatalog.find(metricId)?.unitRes?.let { " " + stringResource(it) }.orEmpty()

/** 寫入中的旋轉指示。 */
@Composable
private fun WritingSpinner(tint: Color) {
    val transition = rememberInfiniteTransition(label = "writeSpinner")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(SPINNER_PERIOD_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "writeSpinnerAngle",
    )
    Canvas(Modifier.size(SPINNER_SIZE)) {
        val stroke = size.minDimension * SPINNER_STROKE_RATIO
        drawArc(
            color = tint,
            startAngle = angle,
            sweepAngle = SPINNER_SWEEP_DEG,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

private val TOAST_CORNER = 14.dp
private val SPINNER_SIZE = 14.dp
private val DOT_SIZE = 8.dp
private const val SPINNER_PERIOD_MS = 900
private const val SPINNER_SWEEP_DEG = 270f
private const val SPINNER_STROKE_RATIO = 0.16f

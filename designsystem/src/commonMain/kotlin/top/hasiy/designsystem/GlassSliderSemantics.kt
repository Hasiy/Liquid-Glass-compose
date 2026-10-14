/* Copyright 2026 FitDash contributors. */
package top.hasiy.designsystem

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import kotlin.math.roundToInt

/** Shared accessibility contract for the custom slider renderers. */
internal fun Modifier.glassSliderSemantics(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    enabled: Boolean,
    onSetValue: (Float) -> Unit,
): Modifier = semantics(mergeDescendants = true) {
    progressBarRangeInfo = ProgressBarRangeInfo(value, range, steps)
    if (!enabled) disabled()
    setProgress { target ->
        if (!enabled || !target.isFinite() || range.endInclusive <= range.start) {
            false
        } else {
            val clamped = target.coerceIn(range)
            val next = if (steps > 0) {
                val intervals = steps.toFloat() + 1f
                val fraction = (clamped - range.start) / (range.endInclusive - range.start)
                range.start + (fraction * intervals).roundToInt() / intervals *
                    (range.endInclusive - range.start)
            } else clamped
            if (next == value) false else {
                onSetValue(next)
                true
            }
        }
    }
}

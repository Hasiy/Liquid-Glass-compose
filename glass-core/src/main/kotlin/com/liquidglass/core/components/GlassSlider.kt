package com.liquidglass.core.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.liquidglass.core.surface.BackdropPainter
import com.liquidglass.core.surface.GlassSurface
import com.liquidglass.core.theme.GlassTokens
import com.liquidglass.core.theme.LocalGlassPalette

/**
 * SPEC-03 §5：按住整条任意位置直接拖动/点按设置值（REQ-SLIDER-DRAG）。
 */
@Composable
fun GlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: BackdropPainter? = null,
) {
    val palette = LocalGlassPalette.current
    val currentValue by rememberUpdatedState(value)
    var dragging by remember { mutableStateOf(false) }
    val animated by animateFloatAsState(
        targetValue = value.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = if (dragging) 2000f else GlassTokens.springStiffness),
        label = "slider",
    )
    val knobScale by animateFloatAsState(
        targetValue = if (dragging) 1.05f else 1f,
        animationSpec = spring(GlassTokens.springDamping, GlassTokens.springStiffness),
        label = "knob",
    )

    GlassSurface(
        modifier = modifier
            .height(48.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    dragging = true
                    onValueChange((down.position.x / size.width.toFloat()).coerceIn(0f, 1f))
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.first()
                        if (!change.pressed) break
                        change.consume()
                        val delta = change.positionChange().x / size.width.toFloat()
                        onValueChange((currentValue + delta).coerceIn(0f, 1f))
                    }
                    dragging = false
                }
            },
        shape = RoundedCornerShape(GlassTokens.radiusPill),
        backdrop = backdrop,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.height / 2f
            val inset = GlassTokens.strokeWidth.toPx() / 2f

            drawRoundRect(color = palette.trackFill, cornerRadius = CornerRadius(r))
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(palette.rimLightTop, palette.rimLight.copy(alpha = palette.rimLight.alpha * 0.5f)),
                ),
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2, size.height - inset * 2),
                cornerRadius = CornerRadius(r - inset),
                style = Stroke(GlassTokens.strokeWidth.toPx()),
            )

            val fillW = (size.width - 4f) * animated
            if (fillW > r) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(palette.progressFillStart, palette.progressFillEnd),
                    ),
                    topLeft = Offset(2f, 2f),
                    size = Size(fillW, size.height - 4f),
                    cornerRadius = CornerRadius((r - 2f) * knobScale),
                )
            }
        }
    }
}

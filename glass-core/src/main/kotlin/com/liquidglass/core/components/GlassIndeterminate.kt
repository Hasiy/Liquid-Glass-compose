package com.liquidglass.core.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.liquidglass.core.surface.BackdropPainter
import com.liquidglass.core.surface.GlassSurface
import com.liquidglass.core.theme.GlassTokens
import com.liquidglass.core.theme.LocalGlassPalette

/** SPEC-03 §6：载入条，亮段 30% 循环滑动 */
@Composable
fun GlassIndeterminate(
    modifier: Modifier = Modifier,
    backdrop: BackdropPainter? = null,
    captureKey: Any? = null,
) {
    val palette = LocalGlassPalette.current
    val transition = rememberInfiniteTransition(label = "indeterminate")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "slide",
    )

    GlassSurface(
        modifier = modifier.height(10.dp),
        shape = RoundedCornerShape(GlassTokens.radiusPill),
        backdrop = backdrop,
        captureKey = captureKey,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.height / 2f
            val inset = GlassTokens.strokeWidth.toPx() / 2f

            // 细轨道
            drawRoundRect(
                color = palette.trackFill,
                cornerRadius = CornerRadius(r),
            )
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(palette.rimLight.copy(alpha = palette.rimLight.alpha * 0.6f), palette.rimLight.copy(alpha = palette.rimLight.alpha * 0.3f)),
                ),
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2, size.height - inset * 2),
                cornerRadius = CornerRadius(r - inset),
                style = Stroke(GlassTokens.strokeWidth.toPx()),
            )

            // 亮段 30%
            val segW = size.width * 0.3f
            val x = t * (size.width - segW)
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        palette.progressFillStart.copy(alpha = 0f),
                        palette.progressFillEnd,
                        palette.progressFillStart.copy(alpha = 0f),
                    ),
                    startX = x,
                    endX = x + segW,
                ),
                topLeft = Offset(x, 1f),
                size = Size(segW, size.height - 2f),
                cornerRadius = CornerRadius(r - 1f),
            )
        }
    }
}

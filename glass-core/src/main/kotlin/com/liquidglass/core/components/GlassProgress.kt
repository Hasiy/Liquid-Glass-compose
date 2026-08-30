package com.liquidglass.core.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.unit.dp
import com.liquidglass.core.surface.BackdropPainter
import com.liquidglass.core.surface.GlassSurface
import com.liquidglass.core.theme.GlassTokens
import com.liquidglass.core.theme.LocalGlassPalette

/**
 * SPEC-03 §4：确定进度条（加宽版 28dp）。
 * 轨道 = trackFill + rim 描边；填充 = 水平渐变 pill，端部带亮边。
 */
@Composable
fun GlassProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    backdrop: BackdropPainter? = null,
    captureKey: Any? = null,
) {
    val palette = LocalGlassPalette.current
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(GlassTokens.springDamping, GlassTokens.springStiffness),
        label = "progress",
    )
    GlassSurface(
        modifier = modifier.height(28.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(GlassTokens.radiusPill),
        backdrop = backdrop,
        captureKey = captureKey,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.height / 2f
            val inset = GlassTokens.strokeWidth.toPx() / 2f

            // 轨道
            drawRoundRect(
                color = palette.trackFill,
                cornerRadius = CornerRadius(r),
            )
            // 轨道 rim
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(palette.rimLightTop, palette.rimLight.copy(alpha = palette.rimLight.alpha * 0.5f)),
                ),
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2, size.height - inset * 2),
                cornerRadius = CornerRadius(r - inset),
                style = Stroke(GlassTokens.strokeWidth.toPx()),
            )

            // 填充
            val fillW = (size.width - 4f) * animated
            if (fillW > r * 2) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(palette.progressFillStart, palette.progressFillEnd),
                    ),
                    topLeft = Offset(2f, 2f),
                    size = Size(fillW, size.height - 4f),
                    cornerRadius = CornerRadius(r - 2f),
                )
                // 端部亮边（knob 高光）
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(palette.progressFillEnd.copy(alpha = 0f), palette.progressFillEnd),
                        startX = 2f + fillW - 24f,
                        endX = 2f + fillW,
                    ),
                    topLeft = Offset(2f + fillW - 24f, 2f),
                    size = Size(24f, size.height - 4f),
                    cornerRadius = CornerRadius(r - 2f),
                )
            }
        }
    }
}

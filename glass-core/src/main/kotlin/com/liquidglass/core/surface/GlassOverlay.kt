package com.liquidglass.core.surface

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.shape.RoundedCornerShape
import com.liquidglass.core.theme.GlassTokens
import com.liquidglass.core.theme.LocalGlassPalette

/**
 * SPEC-02 §3 玻璃叠层，按序绘制：
 * 1 glassFill 填充 → 2 顶部 innerHighlight → 3 底部 innerShadow → 4 rim 描边（顶部加亮）
 */
@Composable
fun Modifier.glassOverlay(
    shape: RoundedCornerShape,
    fill: androidx.compose.ui.graphics.Color? = null,
): Modifier {
    val palette = LocalGlassPalette.current
    val fillColor = fill ?: palette.glassFill
    return this
        .clip(shape)
        .drawBehind {
            val r = shape.topStart.toPx(size, this)

            // 1. 基础填充
            drawRoundRect(
                color = fillColor,
                cornerRadius = CornerRadius(r),
            )

            // 2. 顶部内侧高光带（垂直渐变 alpha→0）
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(palette.innerHighlight, palette.innerHighlight.copy(alpha = 0f)),
                    startY = 0f,
                    endY = size.height * 0.38f,
                ),
                cornerRadius = CornerRadius(r),
            )

            // 3. 底部内侧暗带（厚度感）
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(palette.innerShadow.copy(alpha = 0f), palette.innerShadow),
                    startY = size.height * 0.82f,
                    endY = size.height,
                ),
                cornerRadius = CornerRadius(r),
            )

            // 4. rim 描边：垂直渐变模拟顶部光源加亮
            val strokePx = GlassTokens.strokeWidth.toPx()
            val inset = strokePx / 2f
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        palette.rimLightTop,
                        palette.rimLight,
                        palette.rimLight.copy(alpha = palette.rimLight.alpha * 0.5f),
                    ),
                    startY = 0f,
                    endY = size.height,
                ),
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2, size.height - inset * 2),
                cornerRadius = CornerRadius((r - inset).coerceAtLeast(0f)),
                style = Stroke(strokePx),
            )
        }
}

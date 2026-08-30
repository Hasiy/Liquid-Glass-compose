package com.liquidglass.demo.pages

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.liquidglass.core.theme.GlassPalette
import com.liquidglass.core.theme.LocalGlassPalette

/**
 * SPEC-05 §2：连续变焦的共享封面视觉（纯 Canvas）。
 * 首页 hero 卡与详情页封面复用同一绘制，保证 sharedBounds 缩放自然。
 * 内容：对角渐变 + 两个水滴玻璃 + 顶部高光条。
 */
@Composable
fun HeroArt(modifier: Modifier = Modifier) {
    val palette = LocalGlassPalette.current
    Canvas(modifier.fillMaxSize()) {
        // 对角渐变背景
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    palette.blobTeal,
                    palette.blobPurple,
                    palette.blobMagenta,
                ),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height),
            ),
        )

        // 光斑柔化
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.blobOrange.copy(alpha = 0.5f),
                    Color.Transparent,
                ),
                center = Offset(size.width * 0.2f, size.height * 0.25f),
                radius = size.width * 0.5f,
            ),
            center = Offset(size.width * 0.2f, size.height * 0.25f),
            radius = size.width * 0.5f,
        )

        drawDroplet(palette, cx = size.width * 0.30f, cy = size.height * 0.45f, r = size.height * 0.30f)
        drawDroplet(palette, cx = size.width * 0.78f, cy = size.height * 0.62f, r = size.height * 0.42f)

        // 顶部高光条（液态光泽）
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.28f),
                    Color.White.copy(alpha = 0f),
                ),
                startY = 0f,
                endY = size.height * 0.28f,
            ),
            topLeft = Offset.Zero,
            size = Size(size.width, size.height * 0.28f),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDroplet(
    palette: GlassPalette,
    cx: Float,
    cy: Float,
    r: Float,
) {
    val rect = Rect(Offset(cx - r, cy - r), Size(r * 2, r * 2))
    // 玻璃圆面
    drawOval(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.22f),
                palette.glassFillRaised,
            ),
            start = Offset(cx - r, cy - r),
            end = Offset(cx + r, cy + r),
        ),
        topLeft = rect.topLeft,
        size = rect.size,
    )
    // rim 描边
    drawOval(
        color = Color.White.copy(alpha = 0.45f),
        topLeft = rect.topLeft,
        size = rect.size,
        style = Stroke(
            width = r * 0.06f,
            cap = StrokeCap.Round,
        ),
    )
    // 内侧高光（左上小弧）
    drawOval(
        color = Color.White.copy(alpha = 0.25f),
        topLeft = Offset(cx - r * 0.55f, cy - r * 0.7f),
        size = Size(r * 0.5f, r * 0.3f),
    )
}
package com.liquidglass.demo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.liquidglass.core.surface.BackdropPainter
import com.liquidglass.core.theme.GlassPalette
import com.liquidglass.core.theme.LocalGlassPalette

/**
 * SPEC-04 §1：页面背景 = 深色基底 + 柔和弥散光斑 + 底部暗角（iOS 26 质感）。
 * 光斑用径向渐变（低 alpha 大半径）天然柔化；多枚叠加制造"光泽弥漫"，
 * 底部纵向暗角增强景深。
 */
fun liquidBackdrop(palette: GlassPalette): BackdropPainter = { pageSize ->
    val w = pageSize.width
    val h = pageSize.height

    // 基底：深紫黑 + 纵向微弱冷调层次
    drawRect(palette.bgBase)
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                blend(palette.bgBase, palette.blobPurple, 0.28f),
                palette.bgBase,
                blend(palette.bgBase, palette.blobTeal, 0.18f),
            ),
            startY = 0f,
            endY = h,
        ),
    )

    // 漫射环境光晕（让整体背景有微弱发光感）
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                palette.blobPurple.copy(alpha = 0.22f),
                palette.blobTeal.copy(alpha = 0.10f),
                Color.Transparent,
            ),
            center = Offset(w * 0.5f, h * 0.35f),
            radius = w * 0.9f,
        ),
    )

    // 光斑（中高 alpha 大半径，柔边弥漫）
    fun blob(color: Color, cx: Float, cy: Float, r: Float, a: Float) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = a), color.copy(alpha = a * 0.35f), Color.Transparent),
                center = Offset(cx, cy),
                radius = r,
            ),
            center = Offset(cx, cy),
            radius = r,
        )
    }

    blob(palette.blobOrange, w * 0.12f, h * 0.08f, w * 0.55f, 0.72f)
    blob(palette.blobMagenta, w * 0.92f, h * 0.22f, w * 0.68f, 0.78f)
    blob(palette.blobPurple, w * 0.08f, h * 0.78f, w * 0.75f, 0.68f)
    blob(palette.blobTeal, w * 0.90f, h * 0.84f, w * 0.62f, 0.62f)
    // 底部额外光斑，专门给 tab 栏区域提供毛玻璃底色
    blob(palette.blobMagenta, w * 0.25f, h * 0.92f, w * 0.45f, 0.45f)
    blob(palette.blobTeal, w * 0.72f, h * 0.95f, w * 0.40f, 0.40f)
    // 冷色高光，丰富层次
    blob(blend(palette.blobTeal, Color.White, 0.40f), w * 0.75f, h * 0.12f, w * 0.35f, 0.45f)
    blob(blend(palette.blobPurple, Color.White, 0.35f), w * 0.42f, h * 0.48f, w * 0.48f, 0.32f)
    blob(blend(palette.blobMagenta, Color.White, 0.30f), w * 0.55f, h * 0.68f, w * 0.40f, 0.28f)

    // 底部暗角（图景聚焦，增强厚重感）
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                blend(palette.bgBase, Color.Black, 0.45f),
            ),
            startY = h * 0.55f,
            endY = h,
        ),
    )
}

/** 按分量在 [a] 与 [b] 间按比例 [t] 混合（简化，不处理 alpha/buffer 不再使用） */
private fun blend(a: Color, b: Color, t: Float): Color = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = a.alpha + (b.alpha - a.alpha) * t,
)

/** 整页背景画布（L0 层） */
@Composable
fun BackdropLayer(modifier: Modifier = Modifier) {
    val palette = LocalGlassPalette.current
    Canvas(modifier.fillMaxSize()) {
        liquidBackdrop(palette)(size)
    }
}
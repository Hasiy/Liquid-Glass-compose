package com.liquidglass.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * SPEC-01 §1 颜色 tokens。默认暗色 palette（对齐参考截图）。
 */
@Immutable
data class GlassPalette(
    val bgBase: Color,
    val blobOrange: Color,
    val blobMagenta: Color,
    val blobPurple: Color,
    val blobTeal: Color,
    val glassFill: Color,
    val glassFillRaised: Color,
    val rimLight: Color,
    val rimLightTop: Color,
    val innerHighlight: Color,
    val innerShadow: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val trackFill: Color,
    val progressFillStart: Color,
    val progressFillEnd: Color,
    val iconTint: Color,
) {
    companion object {
        val Dark = GlassPalette(
            bgBase = Color(0xFF120A1A),
            blobOrange = Color(0xFFE87C3A),
            blobMagenta = Color(0xFFD14E8C),
            blobPurple = Color(0xFF8551D6),
            blobTeal = Color(0xFF2BA3B0),
            glassFill = Color.White.copy(alpha = 0.09f),
            glassFillRaised = Color.White.copy(alpha = 0.13f),
            rimLight = Color.White.copy(alpha = 0.38f),
            rimLightTop = Color.White.copy(alpha = 0.58f),
            innerHighlight = Color.White.copy(alpha = 0.14f),
            innerShadow = Color.Black.copy(alpha = 0.22f),
            textPrimary = Color.White.copy(alpha = 0.92f),
            textSecondary = Color.White.copy(alpha = 0.55f),
            trackFill = Color.White.copy(alpha = 0.12f),
            progressFillStart = Color.White.copy(alpha = 0.45f),
            progressFillEnd = Color.White.copy(alpha = 0.80f),
            iconTint = Color.White.copy(alpha = 0.92f),
        )

        // 预留，本期不启用
        val Light = Dark.copy(
            bgBase = Color(0xFFF2F0F7),
            glassFill = Color.White.copy(alpha = 0.55f),
            rimLight = Color.White.copy(alpha = 0.80f),
            textPrimary = Color.Black.copy(alpha = 0.88f),
            textSecondary = Color.Black.copy(alpha = 0.55f),
            iconTint = Color.Black.copy(alpha = 0.88f),
        )
    }
}

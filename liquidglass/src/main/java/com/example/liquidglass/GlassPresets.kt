package com.example.liquidglass

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 玻璃主題預設變體。
 *
 * 提供四種主題，可透過主題切換設定動態選用：
 * - [Drop]：半透明白 + 左上柔光 + 底部聚光（水珠感）
 * - [Neutral]：均勻磨砂灰，無明顯高光，偏保守
 * - [Dark]：深色半透明玻璃，適合強調按鈕 / 深色介面
 * - [Native]：Compose 原生 Material3 樣式（無玻璃效果）
 */
object GlassPresets {

    /** 水滴：半透明白玻璃，左上柔光、底部聚光，現有 Tab Bar 的效果 */
    val Drop = GlassConfig(
        baseColor = Color(0xFF9A9AA8),
        bodyTopAlpha = 0.30f,
        bodyBottomAlpha = 0.32f,
        highlightInnerAlpha = 0.16f,
        highlightOuterAlpha = 0.035f,
        highlightCenterX = 0.20f,
        highlightCenterY = 0.08f,
        highlightRadiusFactor = 0.72f,
        borderWidth = 1.dp,
        borderTopAlpha = 0.20f,
        borderBottomAlpha = 0.02f,
        shadowElevation = 5.dp,
        shadowAmbientAlpha = 0.30f,
        shadowSpotAlpha = 0.22f,
        contentColor = Color.White,
        followTouchHighlight = true,
        hideHighlightOnTouch = false,
    )

    /** 中性：均勻磨砂灰，柔光弱而居中，視覺最收斂 */
    val Neutral = GlassConfig(
        baseColor = Color(0xFFA5A5AE),
        bodyTopAlpha = 0.26f,
        bodyBottomAlpha = 0.28f,
        highlightInnerAlpha = 0.08f,
        highlightOuterAlpha = 0.02f,
        highlightCenterX = 0.42f,
        highlightCenterY = 0.20f,
        highlightRadiusFactor = 0.60f,
        borderWidth = 1.dp,
        borderTopAlpha = 0.16f,
        borderBottomAlpha = 0.02f,
        shadowElevation = 4.dp,
        shadowAmbientAlpha = 0.24f,
        shadowSpotAlpha = 0.18f,
        contentColor = Color.White,
        followTouchHighlight = true,
        hideHighlightOnTouch = false,
    )

    /** 深色玻璃：深色半透明基底，柔光較強，適合強調按鈕 */
    val Dark = GlassConfig(
        baseColor = Color(0xFF2E2E3A),
        bodyTopAlpha = 0.38f,
        bodyBottomAlpha = 0.42f,
        highlightInnerAlpha = 0.16f,
        highlightOuterAlpha = 0.06f,
        highlightCenterX = 0.22f,
        highlightCenterY = 0.10f,
        highlightRadiusFactor = 0.80f,
        borderWidth = 1.dp,
        borderTopAlpha = 0.30f,
        borderBottomAlpha = 0.04f,
        shadowElevation = 6.dp,
        shadowAmbientAlpha = 0.34f,
        shadowSpotAlpha = 0.26f,
        contentColor = Color.White,
        followTouchHighlight = true,
        hideHighlightOnTouch = false,
    )

    /** 原生：Compose 原生 Material3 樣式（元件內部自動改用原生渲染） */
    val Native = GlassConfig(
        native = true,
        baseColor = Color(0xFFCFCFD8),
        bodyTopAlpha = 0.9f,
        bodyBottomAlpha = 0.9f,
        highlightInnerAlpha = 0.0f,
        highlightOuterAlpha = 0.0f,
        highlightCenterX = 0.5f,
        highlightCenterY = 0.2f,
        highlightRadiusFactor = 0.6f,
        borderWidth = 0.dp,
        borderTopAlpha = 0f,
        borderBottomAlpha = 0f,
        shadowElevation = 0.dp,
        shadowAmbientAlpha = 0f,
        shadowSpotAlpha = 0f,
        contentColor = Color.White,
        followTouchHighlight = false,
        hideHighlightOnTouch = false,
    )

    /** 所有可選主題，供設定頁展示 */
    val All: List<GlassConfig> = listOf(Drop, Neutral, Dark, Native)
}

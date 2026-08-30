package com.liquidglass.core.theme

import androidx.compose.ui.unit.dp

/**
 * SPEC-01 设计 tokens：所有组件唯一允许引用的视觉常量来源。
 */
object GlassTokens {

    // ---- 形状 ----
    val radiusCardL = 28.dp
    val radiusCard = 20.dp
    val radiusPill = 999.dp

    // ---- 模糊与层级（SPEC-01 §3 / SPEC-02）----
    const val blurMedium = 24f
    const val blurStrong = 64f
    const val offscreenScale = 0.25f
    const val offscreenBlurRadius = 25
    const val snapshotThrottleMs = 100L

    // ---- 尺寸与间距 ----
    val strokeWidth = 1.dp
    val pagePadding = 20.dp
    val cardPadding = 16.dp
    val listItemHeight = 76.dp
    val iconSize = 44.dp
    val tabBarHeight = 68.dp
    val tabBarBottomMargin = 16.dp

    // ---- 动效 ----
    const val springDamping = 0.6f
    const val springStiffness = 320f
    const val pressScale = 0.96f
    const val animMediumMs = 320
}
